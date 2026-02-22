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
#include <libavutil/imgutils.h>
}

#pragma clang diagnostic pop

#include "bink.h"
#include <time.h>
#include <string.h>
#include <stdlib.h>
#include <stdio.h>

// ---------------------------------------------------------------------------
// Internal BinkContext -- wraps all FFmpeg state.
// BINK *must* be the first member so that HBINK (which is _BINK*) can be
// trivially cast to BinkContext* with no offset adjustment needed.
// ---------------------------------------------------------------------------

struct BinkContext {
    BINK             bink;               // MUST be first -- HBINK casts to here
    AVFormatContext* fmtCtx;
    AVCodecContext*  videoCodecCtx;
    int32_t          videoStreamIdx;
    AVFrame*         decodedFrame;
    SwsContext*      swsCtx;
    AVPacket*        packet;
    bool             frameDecoded;       // true when decodedFrame holds current data
    double           frameInterval;      // seconds per frame
    uint64_t         lastFrameTimeNs;    // CLOCK_MONOTONIC nanoseconds at last BinkNextFrame
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

static void bink_ctx_free(BinkContext* ctx)
{
    if (!ctx) return;
    if (ctx->packet)        av_packet_free(&ctx->packet);
    if (ctx->decodedFrame)  av_frame_free(&ctx->decodedFrame);
    if (ctx->swsCtx)        sws_freeContext(ctx->swsCtx);
    if (ctx->videoCodecCtx) avcodec_free_context(&ctx->videoCodecCtx);
    if (ctx->fmtCtx)        avformat_close_input(&ctx->fmtCtx);
    delete ctx;
}

// ---------------------------------------------------------------------------
// BinkOpen
// ---------------------------------------------------------------------------

HBINK BinkOpen(const char* name, UINT /*flags*/)
{
    BinkContext* ctx = new BinkContext();
    memset(ctx, 0, sizeof(*ctx));

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

    // -- Find first video stream --
    ctx->videoStreamIdx = -1;
    for (uint32_t i = 0; i < ctx->fmtCtx->nb_streams; i++) {
        if (ctx->fmtCtx->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
            ctx->videoStreamIdx = static_cast<int32_t>(i);
            break;
        }
    }
    if (ctx->videoStreamIdx < 0) {
        fprintf(stderr, "BinkOpen: no video stream in '%s'\n", name);
        bink_ctx_free(ctx);
        return NULL;
    }

    AVStream* stream = ctx->fmtCtx->streams[ctx->videoStreamIdx];

    // -- Open decoder --
    const AVCodec* codec = avcodec_find_decoder(stream->codecpar->codec_id);
    if (!codec) {
        fprintf(stderr, "BinkOpen: decoder not found (codec_id %d)\n",
                static_cast<int32_t>(stream->codecpar->codec_id));
        bink_ctx_free(ctx);
        return NULL;
    }

    ctx->videoCodecCtx = avcodec_alloc_context3(codec);
    if (!ctx->videoCodecCtx) { bink_ctx_free(ctx); return NULL; }

    if (avcodec_parameters_to_context(ctx->videoCodecCtx, stream->codecpar) < 0) {
        bink_ctx_free(ctx);
        return NULL;
    }

    if (avcodec_open2(ctx->videoCodecCtx, codec, nullptr) < 0) {
        fprintf(stderr, "BinkOpen: avcodec_open2 failed\n");
        bink_ctx_free(ctx);
        return NULL;
    }

    // -- Pixel-format converter: decoded pix_fmt → RGB565LE --
    ctx->swsCtx = sws_getContext(
        ctx->videoCodecCtx->width,  ctx->videoCodecCtx->height,
        ctx->videoCodecCtx->pix_fmt,
        ctx->videoCodecCtx->width,  ctx->videoCodecCtx->height,
        AV_PIX_FMT_RGB565LE,
        SWS_BILINEAR, nullptr, nullptr, nullptr);
    if (!ctx->swsCtx) { bink_ctx_free(ctx); return NULL; }

    // -- Allocate decode helpers --
    ctx->decodedFrame = av_frame_alloc();
    ctx->packet       = av_packet_alloc();
    if (!ctx->decodedFrame || !ctx->packet) { bink_ctx_free(ctx); return NULL; }

    // -- Populate public BINK fields --
    ctx->bink.Width       = static_cast<UINT>(ctx->videoCodecCtx->width);
    ctx->bink.Height      = static_cast<UINT>(ctx->videoCodecCtx->height);
    ctx->bink.FrameNum    = 1;   // 1-based, matching original Bink convention

    // Frame count: prefer nb_frames, fall back to duration calculation
    if (stream->nb_frames > 0) {
        ctx->bink.Frames = static_cast<UINT>(stream->nb_frames);
    } else if (stream->duration > 0 && stream->r_frame_rate.den > 0) {
        double dur_sec      = static_cast<double>(stream->duration) * av_q2d(stream->time_base);
        double fps          = av_q2d(stream->r_frame_rate);
        ctx->bink.Frames    = static_cast<UINT>(dur_sec * fps + 0.5);
    } else {
        ctx->bink.Frames = 1;   // unknown; prevents immediate Is_Complete()
    }

    // Frame rate as integer ratio (used by game to compute TicksPerFrame)
    ctx->bink.FrameRate    = static_cast<UINT>(stream->r_frame_rate.num);
    ctx->bink.FrameRateDiv = static_cast<UINT>(stream->r_frame_rate.den);
    if (ctx->bink.FrameRateDiv == 0) ctx->bink.FrameRateDiv = 1;

    // Frame interval in seconds
    if (stream->r_frame_rate.num > 0 && stream->r_frame_rate.den > 0)
        ctx->frameInterval = static_cast<double>(stream->r_frame_rate.den)
                           / static_cast<double>(stream->r_frame_rate.num);
    else
        ctx->frameInterval = 1.0 / 15.0;   // fallback: 15 fps

    // lastFrameTimeNs = 0 → first BinkWait returns 0 (display immediately)
    ctx->lastFrameTimeNs = 0;

    fprintf(stderr, "BinkOpen: '%s'  %ux%u  %u frames  @ %u/%u fps\n",
            name, ctx->bink.Width, ctx->bink.Height,
            ctx->bink.Frames, ctx->bink.FrameRate, ctx->bink.FrameRateDiv);

    return reinterpret_cast<HBINK>(ctx);
}

// ---------------------------------------------------------------------------
// BinkDoFrame -- decode the current video frame into ctx->decodedFrame
// ---------------------------------------------------------------------------

int32_t BinkDoFrame(HBINK bink)
{
    if (!bink) return 0;
    BinkContext* ctx = reinterpret_cast<BinkContext*>(bink);
    if (ctx->frameDecoded) return 0;   // already decoded for this display cycle

    while (true) {
        int32_t ret = av_read_frame(ctx->fmtCtx, ctx->packet);
        if (ret < 0) {
            // EOF or read error -- mark movie as complete
            bink->FrameNum = bink->Frames;
            return 0;
        }

        if (ctx->packet->stream_index != ctx->videoStreamIdx) {
            av_packet_unref(ctx->packet);
            continue;   // skip audio and other streams
        }

        ret = avcodec_send_packet(ctx->videoCodecCtx, ctx->packet);
        av_packet_unref(ctx->packet);
        if (ret < 0) continue;   // send error: try next packet

        ret = avcodec_receive_frame(ctx->videoCodecCtx, ctx->decodedFrame);
        if (ret == 0) {
            ctx->frameDecoded = true;
            return 0;
        }
        // AVERROR(EAGAIN) = need more packets before a frame is ready; loop back
        // Any other error: skip this packet and try the next one
    }
}

// ---------------------------------------------------------------------------
// BinkCopyToBuffer -- convert decoded frame to RGB565 and write to caller's buffer
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
// BinkWait -- return 0 when it is time to display a frame, 1 if still waiting
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
// BinkNextFrame -- advance timing reference and prepare for the next frame
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
// BinkClose -- free all FFmpeg resources
// ---------------------------------------------------------------------------

void BinkClose(HBINK bink)
{
    if (!bink) return;
    bink_ctx_free(reinterpret_cast<BinkContext*>(bink));
}

// ---------------------------------------------------------------------------
// BinkSoundUseDirectSound -- no-op (audio deferred; game calls
//   Temp_Disable_Audio(true) during playback, so silence is expected)
// ---------------------------------------------------------------------------

void BinkSoundUseDirectSound(UINT /*device*/) {}

// ---------------------------------------------------------------------------
// Stub functions (never called by game code)
// ---------------------------------------------------------------------------

void BinkGoto          (HBINK /*bink*/, UINT /*framenum*/, int32_t /*flags*/) {}
UINT BinkGetTrackMaxSize(HBINK /*bink*/, UINT /*track*/) { return 0; }
UINT BinkGetTrackData  (HBINK /*bink*/, UINT /*track*/) { return 0; }
UINT BinkGetTrackType  (HBINK /*bink*/, UINT /*track*/) { return 0; }
UINT BinkGetNumTracks  (HBINK /*bink*/)                  { return 0; }
UINT BinkGetTrackID    (HBINK /*bink*/, UINT /*track*/) { return 0; }
void BinkSetVolume(HBINK /*bink*/, UINT /*track*/, int32_t /*vol*/) {}
void BinkSetPan   (HBINK /*bink*/, UINT /*track*/, int32_t /*pan*/) {}
