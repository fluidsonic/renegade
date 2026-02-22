#include "global.h"

// Suppress warnings inside FFmpeg C headers (not our code)
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wconversion"
#pragma clang diagnostic ignored "-Wsign-conversion"
#pragma clang diagnostic ignored "-Wdouble-promotion"
#pragma clang diagnostic ignored "-Wdeprecated-declarations"

extern "C" {
#include <libavformat/avformat.h>
#include <libavcodec/avcodec.h>
#include <libswscale/swscale.h>
#include <libswresample/swresample.h>
#include <libavutil/channel_layout.h>
}

#pragma clang diagnostic pop

// miniaudio -- implementation lives in mss_impl.cpp; include declarations only
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wconversion"
#pragma clang diagnostic ignored "-Wsign-conversion"
#pragma clang diagnostic ignored "-Wdouble-promotion"
#pragma clang diagnostic ignored "-Wunused-function"
#pragma clang diagnostic ignored "-Wdeprecated-declarations"
#include "miniaudio.h"
#pragma clang diagnostic pop

#include "bink.h"
#include <time.h>
#include <string.h>
#include <stdlib.h>
#include <stdio.h>

// ---------------------------------------------------------------------------
// Internal BinkContext -- wraps FFmpeg + miniaudio state.
// BINK must be first member so HBINK (which is _BINK*) can be trivially cast.
// ---------------------------------------------------------------------------

struct BinkContext {
    BINK             bink;               // MUST be first -- HBINK points here

    // Video
    AVFormatContext* fmtCtx;
    AVCodecContext*  videoCodecCtx;
    int32_t          videoStreamIdx;
    AVFrame*         decodedFrame;
    SwsContext*      swsCtx;
    AVPacket*        packet;
    bool             frameDecoded;
    double           frameInterval;      // seconds per frame
    uint64_t         lastFrameTimeNs;    // CLOCK_MONOTONIC reference for BinkWait

    // Audio
    AVCodecContext*  audioCodecCtx;      // null if no audio stream
    int32_t          audioStreamIdx;     // -1 if none
    SwrContext*      swrCtx;
    AVFrame*         audioFrame;
    uint8_t*         swrOutBuf;          // scratch buffer for swr_convert output
    int32_t          swrOutCapacity;     // capacity in frames
    ma_device        audioDevice;
    bool             audioDeviceInit;
    ma_pcm_rb        audioRingBuf;       // lock-free ring buffer: producer=game thread, consumer=audio callback
    bool             audioRingBufInit;
};

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

static uint64_t now_ns()
{
    struct timespec ts;
    clock_gettime(CLOCK_MONOTONIC, &ts);
    return static_cast<uint64_t>(ts.tv_sec) * 1000000000ULL
         + static_cast<uint64_t>(ts.tv_nsec);
}

// ---------------------------------------------------------------------------
// Audio callback -- runs on miniaudio audio thread
// ---------------------------------------------------------------------------

static void bink_audio_callback(ma_device* pDevice, void* pOutput,
                                 const void* /*pInput*/, ma_uint32 frameCount)
{
    BinkContext* ctx = static_cast<BinkContext*>(pDevice->pUserData);
    float* out = static_cast<float*>(pOutput);
    ma_uint32 remaining = frameCount;

    while (remaining > 0) {
        ma_uint32 chunk = remaining;
        void* pBuf = nullptr;
        ma_pcm_rb_acquire_read(&ctx->audioRingBuf, &chunk, &pBuf);
        if (chunk == 0) {
            // underrun -- output silence
            memset(out, 0, static_cast<size_t>(remaining) * 2u * sizeof(float));
            break;
        }
        memcpy(out, pBuf, static_cast<size_t>(chunk) * 2u * sizeof(float));
        ma_pcm_rb_commit_read(&ctx->audioRingBuf, chunk);
        out += chunk * 2u;
        remaining -= chunk;
    }
}

// Push one decoded audio frame into the ring buffer
static void push_audio_frame(BinkContext* ctx, AVFrame* frame)
{
    if (!ctx->swrCtx || !ctx->audioRingBufInit) return;

    // Grow scratch buffer if needed
    int32_t maxOut = swr_get_out_samples(ctx->swrCtx, frame->nb_samples);
    if (maxOut <= 0) maxOut = frame->nb_samples * 2;

    if (maxOut > ctx->swrOutCapacity) {
        free(ctx->swrOutBuf);
        ctx->swrOutCapacity = maxOut * 2;
        ctx->swrOutBuf = static_cast<uint8_t*>(
            malloc(static_cast<size_t>(ctx->swrOutCapacity) * 2u * sizeof(float)));
        if (!ctx->swrOutBuf) { ctx->swrOutCapacity = 0; return; }
    }

    uint8_t* outData[1] = { ctx->swrOutBuf };
    int32_t converted = swr_convert(ctx->swrCtx, outData, maxOut,
                                    const_cast<const uint8_t**>(frame->data),
                                    frame->nb_samples);
    if (converted <= 0) return;

    // Write to ring buffer, handling wrap-around
    uint8_t* src      = ctx->swrOutBuf;
    int32_t  leftover = converted;
    while (leftover > 0) {
        ma_uint32 chunk = static_cast<ma_uint32>(leftover);
        void* pBuf = nullptr;
        ma_pcm_rb_acquire_write(&ctx->audioRingBuf, &chunk, &pBuf);
        if (chunk == 0 || !pBuf) break;   // ring buffer full -- discard
        memcpy(pBuf, src, static_cast<size_t>(chunk) * 2u * sizeof(float));
        ma_pcm_rb_commit_write(&ctx->audioRingBuf, chunk);
        src     += static_cast<size_t>(chunk) * 2u * sizeof(float);
        leftover -= static_cast<int32_t>(chunk);
    }
}

// ---------------------------------------------------------------------------
// Cleanup helper
// ---------------------------------------------------------------------------

static void bink_ctx_free(BinkContext* ctx)
{
    if (!ctx) return;

    // Audio teardown (must stop device before uninit)
    if (ctx->audioDeviceInit) {
        ma_device_stop(&ctx->audioDevice);
        ma_device_uninit(&ctx->audioDevice);
    }
    if (ctx->audioRingBufInit)  ma_pcm_rb_uninit(&ctx->audioRingBuf);
    if (ctx->swrOutBuf)         free(ctx->swrOutBuf);
    if (ctx->swrCtx)            swr_free(&ctx->swrCtx);
    if (ctx->audioFrame)        av_frame_free(&ctx->audioFrame);
    if (ctx->audioCodecCtx)     avcodec_free_context(&ctx->audioCodecCtx);

    // Video teardown
    if (ctx->packet)            av_packet_free(&ctx->packet);
    if (ctx->decodedFrame)      av_frame_free(&ctx->decodedFrame);
    if (ctx->swsCtx)            sws_freeContext(ctx->swsCtx);
    if (ctx->videoCodecCtx)     avcodec_free_context(&ctx->videoCodecCtx);
    if (ctx->fmtCtx)            avformat_close_input(&ctx->fmtCtx);

    delete ctx;
}

// ---------------------------------------------------------------------------
// BinkOpen
// ---------------------------------------------------------------------------

HBINK BinkOpen(const char* name, UINT /*flags*/)
{
    BinkContext* ctx = new BinkContext();
    memset(ctx, 0, sizeof(*ctx));
    ctx->videoStreamIdx = -1;
    ctx->audioStreamIdx = -1;

    // -- Open container --
    if (avformat_open_input(&ctx->fmtCtx, name, nullptr, nullptr) < 0) {
        fprintf(stderr, "BinkOpen: avformat_open_input failed for '%s'\n", name);
        delete ctx;
        return NULL;
    }
    if (avformat_find_stream_info(ctx->fmtCtx, nullptr) < 0) {
        fprintf(stderr, "BinkOpen: avformat_find_stream_info failed\n");
        bink_ctx_free(ctx);
        return NULL;
    }

    // -- Find video and (optionally) audio streams --
    for (uint32_t i = 0; i < ctx->fmtCtx->nb_streams; i++) {
        AVMediaType t = ctx->fmtCtx->streams[i]->codecpar->codec_type;
        if (t == AVMEDIA_TYPE_VIDEO && ctx->videoStreamIdx < 0)
            ctx->videoStreamIdx = static_cast<int32_t>(i);
        else if (t == AVMEDIA_TYPE_AUDIO && ctx->audioStreamIdx < 0)
            ctx->audioStreamIdx = static_cast<int32_t>(i);
    }

    if (ctx->videoStreamIdx < 0) {
        fprintf(stderr, "BinkOpen: no video stream in '%s'\n", name);
        bink_ctx_free(ctx);
        return NULL;
    }

    // -- Open video decoder --
    AVStream* vstream = ctx->fmtCtx->streams[ctx->videoStreamIdx];
    const AVCodec* vcodec = avcodec_find_decoder(vstream->codecpar->codec_id);
    if (!vcodec) {
        fprintf(stderr, "BinkOpen: no video decoder for codec_id %d\n",
                static_cast<int32_t>(vstream->codecpar->codec_id));
        bink_ctx_free(ctx);
        return NULL;
    }
    ctx->videoCodecCtx = avcodec_alloc_context3(vcodec);
    if (!ctx->videoCodecCtx
        || avcodec_parameters_to_context(ctx->videoCodecCtx, vstream->codecpar) < 0
        || avcodec_open2(ctx->videoCodecCtx, vcodec, nullptr) < 0) {
        fprintf(stderr, "BinkOpen: video decoder init failed\n");
        bink_ctx_free(ctx);
        return NULL;
    }

    // -- Pixel-format converter: decoded → BGR565LE (matches GL_UNSIGNED_SHORT_5_6_5_REV) --
    ctx->swsCtx = sws_getContext(
        ctx->videoCodecCtx->width,  ctx->videoCodecCtx->height,
        ctx->videoCodecCtx->pix_fmt,
        ctx->videoCodecCtx->width,  ctx->videoCodecCtx->height,
        AV_PIX_FMT_BGR565LE,   // D3DFMT_R5G6B5 with GL_UNSIGNED_SHORT_5_6_5_REV needs BGR order
        SWS_BILINEAR, nullptr, nullptr, nullptr);
    if (!ctx->swsCtx) { bink_ctx_free(ctx); return NULL; }

    // -- Decode helper allocations --
    ctx->decodedFrame = av_frame_alloc();
    ctx->packet       = av_packet_alloc();
    if (!ctx->decodedFrame || !ctx->packet) { bink_ctx_free(ctx); return NULL; }

    // -- Populate public BINK fields --
    ctx->bink.Width    = static_cast<UINT>(ctx->videoCodecCtx->width);
    ctx->bink.Height   = static_cast<UINT>(ctx->videoCodecCtx->height);
    ctx->bink.FrameNum = 1;   // 1-based, matching original Bink convention

    if (vstream->nb_frames > 0) {
        ctx->bink.Frames = static_cast<UINT>(vstream->nb_frames);
    } else if (vstream->duration > 0 && vstream->r_frame_rate.den > 0) {
        double dur = static_cast<double>(vstream->duration) * av_q2d(vstream->time_base);
        ctx->bink.Frames = static_cast<UINT>(dur * av_q2d(vstream->r_frame_rate) + 0.5);
    } else {
        ctx->bink.Frames = 1;
    }

    ctx->bink.FrameRate    = static_cast<UINT>(vstream->r_frame_rate.num);
    ctx->bink.FrameRateDiv = static_cast<UINT>(vstream->r_frame_rate.den);
    if (ctx->bink.FrameRateDiv == 0) ctx->bink.FrameRateDiv = 1;

    ctx->frameInterval = (vstream->r_frame_rate.num > 0 && vstream->r_frame_rate.den > 0)
        ? static_cast<double>(vstream->r_frame_rate.den) / static_cast<double>(vstream->r_frame_rate.num)
        : 1.0 / 15.0;

    // lastFrameTimeNs = 0 → first BinkWait returns 0 immediately
    ctx->lastFrameTimeNs = 0;

    // -- Audio setup --
    if (ctx->audioStreamIdx >= 0) {
        AVStream* astream = ctx->fmtCtx->streams[ctx->audioStreamIdx];
        const AVCodec* acodec = avcodec_find_decoder(astream->codecpar->codec_id);
        if (acodec) {
            ctx->audioCodecCtx = avcodec_alloc_context3(acodec);
            if (ctx->audioCodecCtx
                && avcodec_parameters_to_context(ctx->audioCodecCtx, astream->codecpar) == 0
                && avcodec_open2(ctx->audioCodecCtx, acodec, nullptr) == 0) {

                ctx->audioFrame = av_frame_alloc();

                // Set up resampler: decoder format → float interleaved stereo
                AVChannelLayout outLayout = {};
                av_channel_layout_default(&outLayout, 2);   // stereo

                AVChannelLayout inLayout = ctx->audioCodecCtx->ch_layout;
                if (inLayout.nb_channels == 0)
                    av_channel_layout_default(&inLayout, 2);  // assume stereo fallback

                int32_t sampleRate = ctx->audioCodecCtx->sample_rate;
                if (sampleRate <= 0) sampleRate = 44100;

                if (swr_alloc_set_opts2(&ctx->swrCtx,
                        &outLayout, AV_SAMPLE_FMT_FLT, sampleRate,
                        &inLayout,  ctx->audioCodecCtx->sample_fmt, sampleRate,
                        0, nullptr) == 0
                    && swr_init(ctx->swrCtx) == 0) {

                    // Ring buffer: 2 seconds of float stereo
                    ma_uint32 ringFrames = static_cast<ma_uint32>(sampleRate) * 2u;
                    if (ma_pcm_rb_init(ma_format_f32, 2, ringFrames,
                                       nullptr, nullptr, &ctx->audioRingBuf) == MA_SUCCESS) {
                        ctx->audioRingBufInit = true;

                        ma_device_config devCfg   = ma_device_config_init(ma_device_type_playback);
                        devCfg.playback.format    = ma_format_f32;
                        devCfg.playback.channels  = 2;
                        devCfg.sampleRate         = static_cast<ma_uint32>(sampleRate);
                        devCfg.dataCallback       = bink_audio_callback;
                        devCfg.pUserData          = ctx;

                        if (ma_device_init(nullptr, &devCfg, &ctx->audioDevice) == MA_SUCCESS) {
                            ctx->audioDeviceInit = true;
                            ma_device_start(&ctx->audioDevice);
                        } else {
                            fprintf(stderr, "BinkOpen: ma_device_init failed, no audio\n");
                        }
                    }
                }
                av_channel_layout_uninit(&outLayout);
            } else {
                if (ctx->audioCodecCtx) avcodec_free_context(&ctx->audioCodecCtx);
                ctx->audioStreamIdx = -1;
            }
        } else {
            ctx->audioStreamIdx = -1;
        }
    }

    fprintf(stderr, "BinkOpen: '%s'  %ux%u  %u frames  @ %u/%u fps  audio=%s\n",
            name, ctx->bink.Width, ctx->bink.Height, ctx->bink.Frames,
            ctx->bink.FrameRate, ctx->bink.FrameRateDiv,
            ctx->audioDeviceInit ? "yes" : "no");

    return reinterpret_cast<HBINK>(ctx);
}

// ---------------------------------------------------------------------------
// BinkDoFrame -- decode the current video frame; also drains audio packets
// ---------------------------------------------------------------------------

int32_t BinkDoFrame(HBINK bink)
{
    if (!bink) return 0;
    BinkContext* ctx = reinterpret_cast<BinkContext*>(bink);
    if (ctx->frameDecoded) return 0;

    while (true) {
        int32_t ret = av_read_frame(ctx->fmtCtx, ctx->packet);
        if (ret < 0) {
            bink->FrameNum = bink->Frames;   // EOF -- signal completion
            return 0;
        }

        if (ctx->packet->stream_index == ctx->videoStreamIdx) {
            ret = avcodec_send_packet(ctx->videoCodecCtx, ctx->packet);
            av_packet_unref(ctx->packet);
            if (ret < 0) continue;

            ret = avcodec_receive_frame(ctx->videoCodecCtx, ctx->decodedFrame);
            if (ret == 0) {
                ctx->frameDecoded = true;
                return 0;
            }
            // AVERROR(EAGAIN) = need more packets; any other error = skip

        } else if (ctx->audioStreamIdx >= 0
                   && ctx->packet->stream_index == ctx->audioStreamIdx
                   && ctx->audioCodecCtx) {

            if (avcodec_send_packet(ctx->audioCodecCtx, ctx->packet) >= 0) {
                while (avcodec_receive_frame(ctx->audioCodecCtx, ctx->audioFrame) == 0) {
                    push_audio_frame(ctx, ctx->audioFrame);
                    av_frame_unref(ctx->audioFrame);
                }
            }
            av_packet_unref(ctx->packet);

        } else {
            av_packet_unref(ctx->packet);
        }
    }
}

// ---------------------------------------------------------------------------
// BinkCopyToBuffer -- convert decoded frame to BGR565LE and write to caller's buffer
// ---------------------------------------------------------------------------

int32_t BinkCopyToBuffer(HBINK bink, void* dest, int32_t destpitch,
                          UINT /*destheight*/, UINT destx, UINT desty,
                          UINT /*flags*/)
{
    if (!bink || !dest) return 0;
    BinkContext* ctx = reinterpret_cast<BinkContext*>(bink);
    if (!ctx->frameDecoded) return 0;

    uint8_t* dstRow = static_cast<uint8_t*>(dest)
                    + static_cast<size_t>(desty) * static_cast<size_t>(destpitch)
                    + static_cast<size_t>(destx) * 2u;

    uint8_t* dstData[4]   = { dstRow, nullptr, nullptr, nullptr };
    int32_t  dstStride[4] = { destpitch, 0, 0, 0 };

    sws_scale(ctx->swsCtx,
              const_cast<const uint8_t* const*>(ctx->decodedFrame->data),
              ctx->decodedFrame->linesize,
              0, ctx->videoCodecCtx->height,
              dstData, dstStride);

    return 0;
}

// ---------------------------------------------------------------------------
// BinkWait -- return 0 when it's time to display, 1 if still waiting
// ---------------------------------------------------------------------------

int32_t BinkWait(HBINK bink)
{
    if (!bink) return 0;
    BinkContext* ctx = reinterpret_cast<BinkContext*>(bink);

    if (ctx->lastFrameTimeNs == 0)
        return 0;   // first frame: display immediately

    uint64_t elapsed  = now_ns() - ctx->lastFrameTimeNs;
    uint64_t interval = static_cast<uint64_t>(ctx->frameInterval * 1.0e9);
    return (elapsed < interval) ? 1 : 0;
}

// ---------------------------------------------------------------------------
// BinkNextFrame -- advance timing and prepare for the next frame
// ---------------------------------------------------------------------------

void BinkNextFrame(HBINK bink)
{
    if (!bink) return;
    BinkContext* ctx = reinterpret_cast<BinkContext*>(bink);

    ctx->lastFrameTimeNs = now_ns();
    bink->FrameNum++;
    ctx->frameDecoded = false;
    av_frame_unref(ctx->decodedFrame);
}

// ---------------------------------------------------------------------------
// BinkClose
// ---------------------------------------------------------------------------

void BinkClose(HBINK bink)
{
    if (!bink) return;
    bink_ctx_free(reinterpret_cast<BinkContext*>(bink));
}

// ---------------------------------------------------------------------------
// BinkSoundUseDirectSound -- no-op (audio is handled by ma_device above)
// ---------------------------------------------------------------------------

void BinkSoundUseDirectSound(UINT /*device*/) {}

// ---------------------------------------------------------------------------
// Stubs -- never called by game code
// ---------------------------------------------------------------------------

void BinkGoto          (HBINK /*bink*/, UINT /*framenum*/, int32_t /*flags*/) {}
UINT BinkGetTrackMaxSize(HBINK /*bink*/, UINT /*track*/) { return 0; }
UINT BinkGetTrackData  (HBINK /*bink*/, UINT /*track*/) { return 0; }
UINT BinkGetTrackType  (HBINK /*bink*/, UINT /*track*/) { return 0; }
UINT BinkGetNumTracks  (HBINK /*bink*/)                  { return 0; }
UINT BinkGetTrackID    (HBINK /*bink*/, UINT /*track*/) { return 0; }
void BinkSetVolume(HBINK /*bink*/, UINT /*track*/, int32_t /*vol*/) {}
void BinkSetPan   (HBINK /*bink*/, UINT /*track*/, int32_t /*pan*/) {}
