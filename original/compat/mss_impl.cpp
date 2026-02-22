#include "global.h"

// Suppress warnings inside miniaudio (it's a C library, not our code)
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wconversion"
#pragma clang diagnostic ignored "-Wsign-conversion"
#pragma clang diagnostic ignored "-Wdouble-promotion"
#pragma clang diagnostic ignored "-Wunused-variable"
#pragma clang diagnostic ignored "-Wunused-parameter"
#pragma clang diagnostic ignored "-Wunused-function"
#pragma clang diagnostic ignored "-Wdeprecated-declarations"

#define MINIAUDIO_IMPLEMENTATION
#include "miniaudio.h"

#pragma clang diagnostic pop

#include "mss.h"
#include <stdio.h>
#include <pthread.h>
#include <time.h>
#include <string.h>
#include <stdlib.h>

// ---------------------------------------------------------------------------
// Constants and global state
// ---------------------------------------------------------------------------

#define MAX_2D_SAMPLES   64
#define MAX_3D_SAMPLES   32
#define MAX_STREAMS      16
#define MAX_TIMERS       16
#define USER_DATA_SLOTS   8
#define LISTENER_HANDLE  ((H3DPOBJECT)(MAX_3D_SAMPLES + 1u))

static ma_engine g_engine;
static bool      g_engine_initialized = false;
// Must be recursive: Windows CRITICAL_SECTION (which MMSLockClass originally wrapped) allows
// the same thread to nest lock calls. Several call chains do exactly that (e.g. Free_3D_Driver_List
// → Release_3D_Handles, Open_2D_Device → Close_2D_Device) — non-recursive would deadlock.
static pthread_mutex_t g_lock = PTHREAD_RECURSIVE_MUTEX_INITIALIZER;
static _AIL_DIG_DRIVER_STRUCT g_driver = {0u};
static int32_t   g_3d_room_type = ENVIRONMENT_GENERIC;

// File callbacks for MIX archive streaming
static U32  (*g_file_open_cb )(char const*, uintptr_t*) = nullptr;
static void (*g_file_close_cb)(uintptr_t)               = nullptr;
static S32  (*g_file_seek_cb )(uintptr_t, S32, U32)     = nullptr;
static U32  (*g_file_read_cb )(uintptr_t, void*, U32)   = nullptr;

// ---------------------------------------------------------------------------
// Sample structs
// ---------------------------------------------------------------------------

struct Sample2D {
    bool     in_use              = false;
    bool     sound_initialized   = false;
    bool     decoder_initialized = false;
    ma_sound   sound             = {};
    ma_decoder decoder           = {};
    int32_t  loop_count          = 1;
    int32_t  loop_remaining      = 0;
    int32_t  volume              = 127;
    int32_t  pan                 = 64;
    int32_t  current_rate        = 44100;
    int32_t  native_rate         = 44100;
    void* user_data[USER_DATA_SLOTS] = {};
};
static Sample2D g_samples[MAX_2D_SAMPLES];

struct Sample3D {
    bool     in_use              = false;
    bool     sound_initialized   = false;
    bool     decoder_initialized = false;
    ma_sound   sound             = {};
    ma_decoder decoder           = {};
    int32_t  loop_count          = 1;
    int32_t  loop_remaining      = 0;
    int32_t  volume              = 127;
    int32_t  current_rate        = 44100;
    int32_t  native_rate         = 44100;
    float    obstruction         = 0.0f;
    float    occlusion           = 0.0f;
    void*    obj_user_data[USER_DATA_SLOTS] = {};
    int32_t  int_user_data[USER_DATA_SLOTS] = {};
};
static Sample3D g_samples3d[MAX_3D_SAMPLES];

struct Stream {
    bool     in_use              = false;
    bool     sound_initialized   = false;
    bool     decoder_initialized = false;
    ma_sound   sound             = {};
    ma_decoder decoder           = {};
    int32_t  volume              = 127;
    int32_t  pan                 = 64;
    int32_t  loop_count          = 1;
    int32_t  current_rate        = 44100;
    int32_t  native_rate         = 44100;
};
static Stream g_streams[MAX_STREAMS];

typedef void (*AILTIMERCB)(U32);
struct Timer {
    bool       in_use    = false;
    bool       running   = false;
    AILTIMERCB cb        = nullptr;
    uint32_t   period_ms = 100u;
    pthread_t  thread    = {};
};
static Timer g_timers[MAX_TIMERS];

// ---------------------------------------------------------------------------
// Custom VFS for MIX archive streaming
// ---------------------------------------------------------------------------

struct MssVfsFile {
    uintptr_t handle;
    U32 size;
    U32 pos;
};

static ma_result mss_vfs_open(ma_vfs* pVFS, const char* pFilePath, ma_uint32 openMode, ma_vfs_file* pFile) {
    (void)pVFS;
    if (!g_file_open_cb || openMode != MA_OPEN_MODE_READ) {
        fprintf(stderr, "[audio] vfs_open: no callback or wrong mode for \"%s\"\n", pFilePath ? pFilePath : "(null)");
        return MA_ERROR;
    }
    // MSS convention: open_cb returns file size (0=failure), writes file handle into *param.
    uintptr_t handle = 0;
    U32 size = g_file_open_cb(pFilePath, &handle);
    if (handle == 0) {
        fprintf(stderr, "[audio] vfs_open: FAILED \"%s\"\n", pFilePath ? pFilePath : "(null)");
        return MA_ERROR;
    }
    MssVfsFile* f = (MssVfsFile*)malloc(sizeof(MssVfsFile));
    if (!f) { g_file_close_cb(handle); return MA_ERROR; }
    f->handle = handle; f->size = size; f->pos = 0u;
    *pFile = f;
    fprintf(stderr, "[audio] vfs_open: OK \"%s\" size=%u handle=%p\n", pFilePath, size, (void*)handle);
    return MA_SUCCESS;
}

static ma_result mss_vfs_close(ma_vfs* pVFS, ma_vfs_file file) {
    (void)pVFS;
    MssVfsFile* f = (MssVfsFile*)file;
    if (!f) return MA_ERROR;
    if (g_file_close_cb) g_file_close_cb(f->handle);
    free(f);
    return MA_SUCCESS;
}

static ma_result mss_vfs_read(ma_vfs* pVFS, ma_vfs_file file, void* pDst, size_t sizeInBytes, size_t* pBytesRead) {
    (void)pVFS;
    MssVfsFile* f = (MssVfsFile*)file;
    if (!f || !g_file_read_cb) return MA_ERROR;
    U32 n = g_file_read_cb(f->handle, pDst, (U32)sizeInBytes);
    f->pos += n;
    if (pBytesRead) *pBytesRead = (size_t)n;
    static int32_t s_read_log = 0;
    if (s_read_log++ < 5) fprintf(stderr, "[audio] vfs_read: requested=%zu got=%u pos=%u\n", sizeInBytes, n, f->pos);
    return MA_SUCCESS;
}

static ma_result mss_vfs_seek(ma_vfs* pVFS, ma_vfs_file file, ma_int64 offset, ma_seek_origin origin) {
    (void)pVFS;
    MssVfsFile* f = (MssVfsFile*)file;
    if (!f || !g_file_seek_cb) return MA_ERROR;
    U32 from;
    switch (origin) {
        case ma_seek_origin_start:   from = AIL_FILE_SEEK_BEGIN;   break;
        case ma_seek_origin_current: from = AIL_FILE_SEEK_CURRENT; break;
        default:                     from = AIL_FILE_SEEK_END;     break;
    }
    g_file_seek_cb(f->handle, (S32)offset, from);
    switch (origin) {
        case ma_seek_origin_start:   f->pos = (U32)offset;                        break;
        case ma_seek_origin_current: f->pos = (U32)((S32)f->pos + (S32)offset);   break;
        default:                     f->pos = (U32)((S32)f->size + (S32)offset);   break;
    }
    return MA_SUCCESS;
}

static ma_result mss_vfs_tell(ma_vfs* pVFS, ma_vfs_file file, ma_int64* pCursor) {
    (void)pVFS;
    MssVfsFile* f = (MssVfsFile*)file;
    if (!f || !pCursor) return MA_ERROR;
    *pCursor = (ma_int64)f->pos;
    return MA_SUCCESS;
}

static ma_result mss_vfs_info(ma_vfs* pVFS, ma_vfs_file file, ma_file_info* pInfo) {
    (void)pVFS;
    MssVfsFile* f = (MssVfsFile*)file;
    if (!f || !pInfo) return MA_ERROR;
    pInfo->sizeInBytes = (ma_uint64)f->size;
    return MA_SUCCESS;
}

static ma_vfs_callbacks g_mss_vfs = {
    mss_vfs_open, nullptr, mss_vfs_close,
    mss_vfs_read, nullptr, mss_vfs_seek, mss_vfs_tell, mss_vfs_info
};

// ---------------------------------------------------------------------------
// Helper functions
// ---------------------------------------------------------------------------

static Sample2D* get_sample(HSAMPLE s) {
    if (s == 0u || s == (HSAMPLE)-1) return nullptr;
    size_t idx = (size_t)s - 1u;
    if (idx >= (size_t)MAX_2D_SAMPLES) return nullptr;
    return g_samples[idx].in_use ? &g_samples[idx] : nullptr;
}

static Sample3D* get_sample3d(H3DSAMPLE s) {
    if (s == 0u || s == LISTENER_HANDLE) return nullptr;
    size_t idx = (size_t)s - 1u;
    if (idx >= (size_t)MAX_3D_SAMPLES) return nullptr;
    return g_samples3d[idx].in_use ? &g_samples3d[idx] : nullptr;
}

static Stream* get_stream(HSTREAM s) {
    if (s == 0u || s == (HSTREAM)-1) return nullptr;
    size_t idx = (size_t)s - 1u;
    if (idx >= (size_t)MAX_STREAMS) return nullptr;
    return g_streams[idx].in_use ? &g_streams[idx] : nullptr;
}

static float miles_vol_to_ma(int32_t v) { return (float)v / 127.0f; }
static float miles_pan_to_ma(int32_t p) { return (2.0f * (float)p / 127.0f) - 1.0f; }
static int32_t ma_vol_to_miles(float v) { return (int32_t)(v * 127.0f); }
static int32_t ma_pan_to_miles(float p) { return (int32_t)((p + 1.0f) * 0.5f * 127.0f); }

static void update_3d_vol(Sample3D* s) {
    if (!s || !s->sound_initialized) return;
    float v = miles_vol_to_ma(s->volume) * (1.0f - s->obstruction) * (1.0f - s->occlusion);
    ma_sound_set_volume(&s->sound, v);
}

static void uninit_sample2d(Sample2D* s) {
    if (s->sound_initialized)   { ma_sound_uninit(&s->sound);     s->sound_initialized   = false; }
    if (s->decoder_initialized) { ma_decoder_uninit(&s->decoder); s->decoder_initialized = false; }
}
static void uninit_sample3d(Sample3D* s) {
    if (s->sound_initialized)   { ma_sound_uninit(&s->sound);     s->sound_initialized   = false; }
    if (s->decoder_initialized) { ma_decoder_uninit(&s->decoder); s->decoder_initialized = false; }
}
static void uninit_stream(Stream* s) {
    if (s->sound_initialized)   { ma_sound_uninit(&s->sound);     s->sound_initialized   = false; }
    if (s->decoder_initialized) { ma_decoder_uninit(&s->decoder); s->decoder_initialized = false; }
}

// ---------------------------------------------------------------------------
// Timer thread function
// ---------------------------------------------------------------------------

static void* timer_thread_func(void* arg) {
    Timer* t = (Timer*)arg;
    while (t->running) {
        struct timespec ts;
        ts.tv_sec  = (time_t)(t->period_ms / 1000u);
        ts.tv_nsec = (int64_t)((t->period_ms % 1000u) * 1000000u);
        nanosleep(&ts, nullptr);
        if (t->running && t->cb) t->cb(0u);
    }
    return nullptr;
}

// ---------------------------------------------------------------------------
// Core lifecycle
// ---------------------------------------------------------------------------

MSSERROR AIL_startup() {
    if (g_engine_initialized) { fprintf(stderr, "[audio] AIL_startup: already initialized\n"); return MSS_NO_ERROR; }
    ma_engine_config cfg = ma_engine_config_init();
    cfg.listenerCount = 1;
    ma_result res = ma_engine_init(&cfg, &g_engine);
    fprintf(stderr, "[audio] AIL_startup: ma_engine_init result=%d (%s)\n", (int32_t)res, (res == MA_SUCCESS) ? "SUCCESS" : "FAILED");
    if (res != MA_SUCCESS) return MSS_ERROR;
    g_engine_initialized = true;
    return MSS_NO_ERROR;
}

void AIL_shutdown() {
    fprintf(stderr, "[audio] AIL_shutdown\n");
    if (!g_engine_initialized) return;
    for (int32_t i = 0; i < MAX_TIMERS; i++) g_timers[i].running = false;
    for (int32_t i = 0; i < MAX_2D_SAMPLES; i++) { uninit_sample2d(&g_samples[i]); g_samples[i].in_use = false; }
    for (int32_t i = 0; i < MAX_3D_SAMPLES; i++) { uninit_sample3d(&g_samples3d[i]); g_samples3d[i].in_use = false; }
    for (int32_t i = 0; i < MAX_STREAMS; i++)    { uninit_stream(&g_streams[i]); g_streams[i].in_use = false; }
    ma_engine_uninit(&g_engine);
    g_engine_initialized = false;
}

void AIL_lock()   { pthread_mutex_lock(&g_lock); }
void AIL_unlock() { pthread_mutex_unlock(&g_lock); }

UINT32 AIL_ms_count() {
    struct timespec ts;
    clock_gettime(CLOCK_MONOTONIC, &ts);
    return (UINT32)((uint64_t)ts.tv_sec * 1000u + (uint64_t)ts.tv_nsec / 1000000u);
}

// ---------------------------------------------------------------------------
// Driver functions
// ---------------------------------------------------------------------------

HDIGDRIVER AIL_open_digital_driver(UINT32 freq, INT32 bits, INT32 chans, UINT32 flags) {
    (void)freq; (void)bits; (void)chans; (void)flags;
    return &g_driver;
}

void AIL_close_digital_driver(HDIGDRIVER h) { (void)h; }

S32 AIL_waveOutOpen(HDIGDRIVER* driver, void* hwnd, UINT32 num, LPWAVEFORMAT pwf) {
    (void)hwnd; (void)num; (void)pwf;
    if (driver) *driver = &g_driver;
    return MSS_NO_ERROR;
}

void AIL_waveOutClose(HDIGDRIVER h) { (void)h; }

S32 AIL_set_preference(UINT32 pref, INT32 val) {
    (void)pref; (void)val;
    return MSS_NO_ERROR;
}

// ---------------------------------------------------------------------------
// 2D sample implementations
// ---------------------------------------------------------------------------

HSAMPLE AIL_allocate_sample_handle(HDIGDRIVER h) {
    (void)h;
    for (int32_t i = 0; i < MAX_2D_SAMPLES; i++) {
        if (!g_samples[i].in_use) {
            g_samples[i] = Sample2D{};
            g_samples[i].in_use = true;
            fprintf(stderr, "[audio] allocate_2d_sample: handle=%d\n", i + 1);
            return (HSAMPLE)(uint32_t)(i + 1);
        }
    }
    fprintf(stderr, "[audio] allocate_2d_sample: POOL FULL\n");
    return (HSAMPLE)-1;
}

void AIL_release_sample_handle(HSAMPLE s) {
    Sample2D* smp = get_sample(s);
    if (!smp) return;
    uninit_sample2d(smp);
    smp->in_use = false;
}

void AIL_init_sample(HSAMPLE s) {
    Sample2D* smp = get_sample(s);
    if (!smp) return;
    if (smp->sound_initialized) {
        ma_sound_stop(&smp->sound);
        ma_sound_seek_to_pcm_frame(&smp->sound, 0u);
        ma_sound_set_volume(&smp->sound, 1.0f);
        ma_sound_set_pan(&smp->sound, 0.0f);
        ma_sound_set_looping(&smp->sound, MA_FALSE);
    }
    smp->loop_count = 1;
    smp->loop_remaining = 0;
    smp->volume = 127;
    smp->pan = 64;
    smp->current_rate = smp->native_rate;
}

INT32 AIL_WAV_info(const void* data, AILSOUNDINFO* info, size_t buf_size) {
    if (!data || !info || buf_size < 12u) return 0;
    const uint8_t* buf = (const uint8_t*)data;
    if (buf[0] != 'R' || buf[1] != 'I' || buf[2] != 'F' || buf[3] != 'F') return 0;
    if (buf[8] != 'W' || buf[9] != 'A' || buf[10] != 'V' || buf[11] != 'E') return 0;
    uint32_t file_size;
    memcpy(&file_size, buf + 4, 4u);
    // Clamp to actual buffer size — callers may pass a partial read (e.g. 4096-byte stack buffer)
    uint32_t limit = (uint32_t)MIN(file_size + 8u, (uint32_t)buf_size);
    memset(info, 0, sizeof(*info));
    uint32_t off = 12u;
    while (off + 8u <= limit) {
        uint32_t chunk_sz;
        memcpy(&chunk_sz, buf + off + 4u, 4u);
        if (buf[off] == 'f' && buf[off + 1] == 'm' && buf[off + 2] == 't' && buf[off + 3] == ' ' && chunk_sz >= 16u) {
            uint16_t fmt_tag, channels, block_align, bits;
            uint32_t sample_rate;
            memcpy(&fmt_tag,     buf + off + 8u,  2u);
            memcpy(&channels,    buf + off + 10u, 2u);
            memcpy(&sample_rate, buf + off + 12u, 4u);
            memcpy(&block_align, buf + off + 20u, 2u);
            memcpy(&bits,        buf + off + 22u, 2u);
            info->format     = (UINT32)fmt_tag;
            info->channels   = (INT32)channels;
            info->rate       = sample_rate;
            info->bits       = (INT32)bits;
            info->block_size = (UINT32)block_align;
        } else if (buf[off] == 'd' && buf[off + 1] == 'a' && buf[off + 2] == 't' && buf[off + 3] == 'a') {
            info->data_ptr    = (void*)(buf + off + 8u);
            info->initial_ptr = info->data_ptr;
            info->data_len    = chunk_sz;
        }
        off += 8u + ((chunk_sz + 1u) & ~1u);
    }
    if (info->rate == 0u) return 0;
    if (info->format == 1u && info->bits > 0 && info->channels > 0)
        info->samples = info->data_len / ((uint32_t)info->bits / 8u * (uint32_t)info->channels);
    return 1;
}

void AIL_set_named_sample_file(HSAMPLE s, const char* name, const void* data, UINT32 len, INT32 flags) {
    (void)flags;
    Sample2D* smp = get_sample(s);
    if (!smp || !data || len == 0u) {
        fprintf(stderr, "[audio] set_sample_file: handle=%u name=\"%s\" INVALID (smp=%p data=%p len=%u)\n",
            (uint32_t)s, name ? name : "(null)", (void*)smp, data, len);
        return;
    }
    uninit_sample2d(smp);
    ma_result dec_res = ma_decoder_init_memory(data, (size_t)len, nullptr, &smp->decoder);
    fprintf(stderr, "[audio] set_sample_file: handle=%u name=\"%s\" len=%u decoder=%s\n",
        (uint32_t)s, name ? name : "(null)", len, (dec_res == MA_SUCCESS) ? "OK" : "FAILED");
    if (dec_res != MA_SUCCESS) return;
    smp->decoder_initialized = true;
    ma_uint32 rate = 44100u;
    ma_data_source_get_data_format(&smp->decoder, nullptr, nullptr, &rate, nullptr, 0u);
    smp->native_rate  = (int32_t)rate;
    smp->current_rate = smp->native_rate;
    ma_result snd_res = ma_sound_init_from_data_source(&g_engine, &smp->decoder,
            MA_SOUND_FLAG_NO_SPATIALIZATION, nullptr, &smp->sound);
    fprintf(stderr, "[audio] set_sample_file: handle=%u sound_init=%s rate=%u\n",
        (uint32_t)s, (snd_res == MA_SUCCESS) ? "OK" : "FAILED", (uint32_t)rate);
    if (snd_res != MA_SUCCESS) {
        ma_decoder_uninit(&smp->decoder);
        smp->decoder_initialized = false;
        return;
    }
    smp->sound_initialized = true;
    ma_sound_set_volume(&smp->sound, miles_vol_to_ma(smp->volume));
    ma_sound_set_pan(&smp->sound, miles_pan_to_ma(smp->pan));
    ma_sound_set_looping(&smp->sound, (smp->loop_count == 0) ? MA_TRUE : MA_FALSE);
}

void AIL_set_sample_address(HSAMPLE s, const void* addr, UINT32 len) {
    AIL_set_named_sample_file(s, nullptr, addr, len, 0);
}

void AIL_set_sample_type(HSAMPLE s, INT32 format, UINT32 flags) {
    (void)s; (void)format; (void)flags;
}

void AIL_start_sample(HSAMPLE s) {
    Sample2D* smp = get_sample(s);
    fprintf(stderr, "[audio] start_sample: handle=%u sound_initialized=%s\n",
        (uint32_t)s, (smp && smp->sound_initialized) ? "true" : "false");
    if (!smp || !smp->sound_initialized) return;
    ma_sound_seek_to_pcm_frame(&smp->sound, 0u);
    ma_sound_set_looping(&smp->sound, (smp->loop_count == 0) ? MA_TRUE : MA_FALSE);
    smp->loop_remaining = (smp->loop_count > 1) ? smp->loop_count - 1 : 0;
    ma_sound_start(&smp->sound);
}

void AIL_stop_sample(HSAMPLE s) {
    fprintf(stderr, "[audio] stop_sample: handle=%u\n", (uint32_t)s);
    Sample2D* smp = get_sample(s);
    if (!smp || !smp->sound_initialized) return;
    ma_sound_stop(&smp->sound);
}

void AIL_resume_sample(HSAMPLE s) {
    Sample2D* smp = get_sample(s);
    if (!smp || !smp->sound_initialized) return;
    ma_sound_start(&smp->sound);
}

void AIL_end_sample(HSAMPLE s) {
    fprintf(stderr, "[audio] end_sample: handle=%u\n", (uint32_t)s);
    Sample2D* smp = get_sample(s);
    if (!smp) return;
    uninit_sample2d(smp);
    smp->loop_remaining = 0;
}

INT32 AIL_sample_status(HSAMPLE s) {
    static int32_t s_status_log = 0;
    Sample2D* smp = get_sample(s);
    INT32 status;
    if (!smp || !smp->sound_initialized) {
        status = SMP_DONE;
    } else if (ma_sound_is_playing(&smp->sound)) {
        status = SMP_PLAYING;
    } else if (ma_sound_at_end(&smp->sound) && smp->loop_remaining > 0) {
        smp->loop_remaining--;
        ma_sound_seek_to_pcm_frame(&smp->sound, 0u);
        ma_sound_start(&smp->sound);
        status = SMP_PLAYING;
    } else {
        status = ma_sound_at_end(&smp->sound) ? SMP_DONE : SMP_STOPPED;
    }
    if (s_status_log++ < 5) fprintf(stderr, "[audio] sample_status: handle=%u -> %d\n", (uint32_t)s, status);
    return status;
}

void AIL_set_sample_volume(HSAMPLE s, INT32 vol) {
    Sample2D* smp = get_sample(s);
    if (!smp) return;
    if (smp->volume == vol) return;
    fprintf(stderr, "[audio] set_sample_volume: handle=%u vol=%d (%.3f)\n",
        (uint32_t)s, vol, miles_vol_to_ma(vol));
    smp->volume = vol;
    if (smp->sound_initialized) ma_sound_set_volume(&smp->sound, miles_vol_to_ma(vol));
}

INT32 AIL_sample_volume(HSAMPLE s) {
    Sample2D* smp = get_sample(s);
    if (!smp || !smp->sound_initialized) return 0;
    return ma_vol_to_miles(ma_sound_get_volume(&smp->sound));
}

void AIL_set_sample_pan(HSAMPLE s, INT32 pan) {
    Sample2D* smp = get_sample(s);
    if (!smp) return;
    if (smp->pan == pan) return;
    smp->pan = pan;
    if (smp->sound_initialized) ma_sound_set_pan(&smp->sound, miles_pan_to_ma(pan));
}

INT32 AIL_sample_pan(HSAMPLE s) {
    Sample2D* smp = get_sample(s);
    if (!smp || !smp->sound_initialized) return 64;
    return ma_pan_to_miles(ma_sound_get_pan(&smp->sound));
}

void AIL_set_sample_loop_count(HSAMPLE s, INT32 n) {
    Sample2D* smp = get_sample(s);
    if (!smp) return;
    smp->loop_count = n;
    if (smp->sound_initialized)
        ma_sound_set_looping(&smp->sound, (n == 0) ? MA_TRUE : MA_FALSE);
}

INT32 AIL_sample_loop_count(HSAMPLE s) {
    Sample2D* smp = get_sample(s);
    return smp ? smp->loop_count : 0;
}

void AIL_set_sample_playback_rate(HSAMPLE s, INT32 rate) {
    Sample2D* smp = get_sample(s);
    if (!smp) return;
    smp->current_rate = rate;
    if (smp->sound_initialized && smp->native_rate > 0)
        ma_sound_set_pitch(&smp->sound, (float)rate / (float)smp->native_rate);
}

INT32 AIL_sample_playback_rate(HSAMPLE s) {
    Sample2D* smp = get_sample(s);
    return smp ? smp->current_rate : 44100;
}

UINT32 AIL_sample_position(HSAMPLE s) {
    Sample2D* smp = get_sample(s);
    if (!smp || !smp->sound_initialized) return 0u;
    ma_uint64 cursor = 0u;
    ma_sound_get_cursor_in_pcm_frames(&smp->sound, &cursor);
    return (UINT32)(cursor * 4u);
}

void AIL_set_sample_ms_position(HSAMPLE s, UINT32 ms) {
    Sample2D* smp = get_sample(s);
    if (!smp || !smp->sound_initialized || smp->native_rate <= 0) return;
    ma_uint64 frame = (ma_uint64)ms * (ma_uint64)smp->native_rate / 1000u;
    ma_sound_seek_to_pcm_frame(&smp->sound, frame);
}

void AIL_sample_ms_position(HSAMPLE s, INT32* len, INT32* pos) {
    Sample2D* smp = get_sample(s);
    if (!smp || !smp->sound_initialized || smp->native_rate <= 0) {
        if (len) *len = 0;
        if (pos) *pos = 0;
        return;
    }
    ma_uint64 cursor = 0u, total = 0u;
    ma_sound_get_cursor_in_pcm_frames(&smp->sound, &cursor);
    ma_sound_get_length_in_pcm_frames(&smp->sound, &total);
    if (pos) *pos = (INT32)(cursor * 1000u / (ma_uint64)smp->native_rate);
    if (len) *len = (INT32)(total  * 1000u / (ma_uint64)smp->native_rate);
}

void AIL_set_sample_user_data(HSAMPLE s, INT32 index, void* val) {
    Sample2D* smp = get_sample(s);
    if (!smp || index < 0 || index >= USER_DATA_SLOTS) return;
    smp->user_data[index] = val;
}

void* AIL_sample_user_data(HSAMPLE s, INT32 index) {
    Sample2D* smp = get_sample(s);
    if (!smp || index < 0 || index >= USER_DATA_SLOTS) return nullptr;
    return smp->user_data[index];
}

void AIL_set_sample_processor(HSAMPLE s, INT32 proc_type, HPROVIDER hp) {
    (void)s; (void)proc_type; (void)hp;
}

void AIL_set_filter_sample_preference(HSAMPLE s, const char* name, void* val) {
    (void)s; (void)name; (void)val;
}

// ---------------------------------------------------------------------------
// Provider/filter enumeration
// ---------------------------------------------------------------------------

S32 AIL_enumerate_3D_providers(HPROENUM* next, HPROVIDER* p, char** name) {
    static char provider_name[] = "miniaudio 3D";
    if (!next || !p || !name) return 0;
    if (*next == HPROENUM_FIRST) {
        fprintf(stderr, "[audio] enumerate_3d_providers: returning \"%s\"\n", provider_name);
        *p = (HPROVIDER)1u;
        *name = provider_name;
        *next = 1u;
        return 1;
    }
    return 0;
}

S32 AIL_enumerate_filters(HPROENUM* next, HPROVIDER* p, char** name) {
    (void)next; (void)p; (void)name;
    return 0;
}

MSSERROR AIL_open_3D_provider(HPROVIDER p)  {
    fprintf(stderr, "[audio] open_3d_provider: handle=%u -> OK\n", (uint32_t)p);
    (void)p; return MSS_NO_ERROR;
}
void     AIL_close_3D_provider(HPROVIDER p) { (void)p; }

void AIL_set_3D_speaker_type(HPROVIDER p, S32 type) { (void)p; (void)type; }

void AIL_set_3D_room_type(HPROVIDER p, INT32 type) {
    (void)p;
    g_3d_room_type = type;
}

INT32 AIL_3D_room_type(HPROVIDER p) {
    (void)p;
    return g_3d_room_type;
}

// ---------------------------------------------------------------------------
// Listener
// ---------------------------------------------------------------------------

H3DPOBJECT AIL_3D_open_listener(HPROVIDER p) {
    fprintf(stderr, "[audio] 3d_open_listener: -> handle=%u\n", (uint32_t)LISTENER_HANDLE);
    (void)p;
    return LISTENER_HANDLE;
}

// ---------------------------------------------------------------------------
// 3D sample implementations
// ---------------------------------------------------------------------------

H3DSAMPLE AIL_allocate_3D_sample_handle(HPROVIDER p) {
    (void)p;
    for (int32_t i = 0; i < MAX_3D_SAMPLES; i++) {
        if (!g_samples3d[i].in_use) {
            g_samples3d[i] = Sample3D{};
            g_samples3d[i].in_use = true;
            fprintf(stderr, "[audio] allocate_3d_sample: handle=%d\n", i + 1);
            return (H3DSAMPLE)(uint32_t)(i + 1);
        }
    }
    fprintf(stderr, "[audio] allocate_3d_sample: POOL FULL\n");
    return 0u;
}

void AIL_release_3D_sample_handle(H3DSAMPLE s) {
    Sample3D* smp = get_sample3d(s);
    if (!smp) return;
    uninit_sample3d(smp);
    smp->in_use = false;
}

U32 AIL_set_3D_sample_file(H3DSAMPLE s, void* data) {
    Sample3D* smp = get_sample3d(s);
    if (!smp || !data) {
        fprintf(stderr, "[audio] set_3d_sample_file: handle=%u INVALID (smp=%p data=%p)\n",
            (uint32_t)s, (void*)smp, data);
        return 0u;
    }
    const uint8_t* buf = (const uint8_t*)data;
    bool is_riff = (buf[0] == 'R' && buf[1] == 'I' && buf[2] == 'F' && buf[3] == 'F');
    fprintf(stderr, "[audio] set_3d_sample_file: handle=%u RIFF=%s\n", (uint32_t)s, is_riff ? "yes" : "no");
    if (!is_riff) return 0u;
    uint32_t riff_size;
    memcpy(&riff_size, buf + 4u, 4u);
    size_t total = (size_t)riff_size + 8u;
    uninit_sample3d(smp);
    ma_result dec_res = ma_decoder_init_memory(data, total, nullptr, &smp->decoder);
    fprintf(stderr, "[audio] set_3d_sample_file: handle=%u total_bytes=%zu decoder=%s\n",
        (uint32_t)s, total, (dec_res == MA_SUCCESS) ? "OK" : "FAILED");
    if (dec_res != MA_SUCCESS) return 0u;
    smp->decoder_initialized = true;
    ma_uint32 rate = 44100u;
    ma_data_source_get_data_format(&smp->decoder, nullptr, nullptr, &rate, nullptr, 0u);
    smp->native_rate  = (int32_t)rate;
    smp->current_rate = smp->native_rate;
    ma_result snd_res = ma_sound_init_from_data_source(&g_engine, &smp->decoder, 0u, nullptr, &smp->sound);
    fprintf(stderr, "[audio] set_3d_sample_file: handle=%u sound_init=%s\n",
        (uint32_t)s, (snd_res == MA_SUCCESS) ? "OK" : "FAILED");
    if (snd_res != MA_SUCCESS) {
        ma_decoder_uninit(&smp->decoder);
        smp->decoder_initialized = false;
        return 0u;
    }
    smp->sound_initialized = true;
    update_3d_vol(smp);
    ma_sound_set_looping(&smp->sound, (smp->loop_count == 0) ? MA_TRUE : MA_FALSE);
    return 1u;
}

void AIL_set_3D_sample_address(H3DSAMPLE s, const void* addr, UINT32 len) {
    Sample3D* smp = get_sample3d(s);
    if (!smp || !addr || len == 0u) return;
    uninit_sample3d(smp);
    if (ma_decoder_init_memory(addr, (size_t)len, nullptr, &smp->decoder) != MA_SUCCESS) return;
    smp->decoder_initialized = true;
    ma_uint32 rate = 44100u;
    ma_data_source_get_data_format(&smp->decoder, nullptr, nullptr, &rate, nullptr, 0u);
    smp->native_rate  = (int32_t)rate;
    smp->current_rate = smp->native_rate;
    if (ma_sound_init_from_data_source(&g_engine, &smp->decoder, 0u, nullptr, &smp->sound) != MA_SUCCESS) {
        ma_decoder_uninit(&smp->decoder);
        smp->decoder_initialized = false;
        return;
    }
    smp->sound_initialized = true;
    update_3d_vol(smp);
}

void AIL_set_3D_sample_type(H3DSAMPLE s, INT32 format, UINT32 flags) {
    (void)s; (void)format; (void)flags;
}

void AIL_start_3D_sample(H3DSAMPLE s) {
    Sample3D* smp = get_sample3d(s);
    fprintf(stderr, "[audio] start_3d_sample: handle=%u sound_initialized=%s\n",
        (uint32_t)s, (smp && smp->sound_initialized) ? "true" : "false");
    if (!smp || !smp->sound_initialized) return;
    ma_sound_seek_to_pcm_frame(&smp->sound, 0u);
    ma_sound_set_looping(&smp->sound, (smp->loop_count == 0) ? MA_TRUE : MA_FALSE);
    smp->loop_remaining = (smp->loop_count > 1) ? smp->loop_count - 1 : 0;
    ma_sound_start(&smp->sound);
}

void AIL_stop_3D_sample(H3DSAMPLE s) {
    Sample3D* smp = get_sample3d(s);
    if (!smp || !smp->sound_initialized) return;
    ma_sound_stop(&smp->sound);
}

void AIL_resume_3D_sample(H3DSAMPLE s) {
    Sample3D* smp = get_sample3d(s);
    if (!smp || !smp->sound_initialized) return;
    ma_sound_start(&smp->sound);
}

void AIL_end_3D_sample(H3DSAMPLE s) {
    Sample3D* smp = get_sample3d(s);
    if (!smp) return;
    uninit_sample3d(smp);
    smp->loop_remaining = 0;
}

INT32 AIL_3D_sample_status(H3DSAMPLE s) {
    Sample3D* smp = get_sample3d(s);
    if (!smp || !smp->sound_initialized) return SMP_DONE;
    if (ma_sound_is_playing(&smp->sound)) return SMP_PLAYING;
    if (ma_sound_at_end(&smp->sound) && smp->loop_remaining > 0) {
        smp->loop_remaining--;
        ma_sound_seek_to_pcm_frame(&smp->sound, 0u);
        ma_sound_start(&smp->sound);
        return SMP_PLAYING;
    }
    return ma_sound_at_end(&smp->sound) ? SMP_DONE : SMP_STOPPED;
}

void* AIL_3D_object_user_data(H3DSAMPLE s, UINT32 index) {
    Sample3D* smp = get_sample3d(s);
    if (!smp || index >= (UINT32)USER_DATA_SLOTS) return nullptr;
    return smp->obj_user_data[index];
}

void AIL_set_3D_object_user_data(H3DSAMPLE s, UINT32 index, void* val) {
    Sample3D* smp = get_sample3d(s);
    if (!smp || index >= (UINT32)USER_DATA_SLOTS) return;
    smp->obj_user_data[index] = val;
}

void AIL_set_3D_user_data(H3DSAMPLE s, UINT32 index, INT32 val) {
    Sample3D* smp = get_sample3d(s);
    if (!smp || index >= (UINT32)USER_DATA_SLOTS) return;
    smp->int_user_data[index] = val;
}

INT32 AIL_3D_user_data(H3DSAMPLE s, UINT32 index) {
    Sample3D* smp = get_sample3d(s);
    if (!smp || index >= (UINT32)USER_DATA_SLOTS) return 0;
    return smp->int_user_data[index];
}

void AIL_set_3D_sample_volume(H3DSAMPLE s, INT32 vol) {
    Sample3D* smp = get_sample3d(s);
    if (!smp) return;
    if (smp->volume == vol) return;
    fprintf(stderr, "[audio] set_3d_sample_volume: handle=%u vol=%d\n", (uint32_t)s, vol);
    smp->volume = vol;
    update_3d_vol(smp);
}

INT32 AIL_3D_sample_volume(H3DSAMPLE s) {
    Sample3D* smp = get_sample3d(s);
    return smp ? smp->volume : 0;
}

void AIL_set_3D_sample_distances(H3DSAMPLE s, float min_d, float max_d) {
    Sample3D* smp = get_sample3d(s);
    if (!smp || !smp->sound_initialized) return;
    ma_sound_set_min_distance(&smp->sound, min_d);
    ma_sound_set_max_distance(&smp->sound, max_d);
}

void AIL_set_3D_sample_cone(H3DSAMPLE s, float inner, float outer, INT32 outer_vol) {
    Sample3D* smp = get_sample3d(s);
    if (!smp || !smp->sound_initialized) return;
    float pi = 3.14159265358979323846f;
    ma_sound_set_cone(&smp->sound,
        inner * pi / 180.0f,
        outer * pi / 180.0f,
        miles_vol_to_ma(outer_vol));
}

void AIL_set_3D_sample_playback_rate(H3DSAMPLE s, INT32 rate) {
    Sample3D* smp = get_sample3d(s);
    if (!smp) return;
    smp->current_rate = rate;
    if (smp->sound_initialized && smp->native_rate > 0)
        ma_sound_set_pitch(&smp->sound, (float)rate / (float)smp->native_rate);
}

INT32 AIL_3D_sample_playback_rate(H3DSAMPLE s) {
    Sample3D* smp = get_sample3d(s);
    return smp ? smp->current_rate : 44100;
}

void AIL_set_3D_sample_loop_count(H3DSAMPLE s, INT32 n) {
    Sample3D* smp = get_sample3d(s);
    if (!smp) return;
    smp->loop_count = n;
    if (smp->sound_initialized)
        ma_sound_set_looping(&smp->sound, (n == 0) ? MA_TRUE : MA_FALSE);
}

INT32 AIL_3D_sample_loop_count(H3DSAMPLE s) {
    Sample3D* smp = get_sample3d(s);
    return smp ? smp->loop_count : 0;
}

void AIL_set_3D_sample_offset(H3DSAMPLE s, U32 bytes) {
    Sample3D* smp = get_sample3d(s);
    if (!smp || !smp->sound_initialized || smp->native_rate <= 0) return;
    ma_sound_seek_to_pcm_frame(&smp->sound, (ma_uint64)(bytes / 4u));
}

U32 AIL_3D_sample_offset(H3DSAMPLE s) {
    Sample3D* smp = get_sample3d(s);
    if (!smp || !smp->sound_initialized) return 0u;
    ma_uint64 cursor = 0u;
    ma_sound_get_cursor_in_pcm_frames(&smp->sound, &cursor);
    return (U32)(cursor * 4u);
}

U32 AIL_3D_sample_length(H3DSAMPLE s) {
    Sample3D* smp = get_sample3d(s);
    if (!smp || !smp->sound_initialized) return 0u;
    ma_uint64 total = 0u;
    ma_sound_get_length_in_pcm_frames(&smp->sound, &total);
    return (U32)(total * 4u);
}

UINT32 AIL_3D_sample_position(H3DSAMPLE s) {
    return AIL_3D_sample_offset(s);
}

void AIL_set_3D_sample_obstruction(H3DSAMPLE s, float val) {
    Sample3D* smp = get_sample3d(s);
    if (!smp) return;
    smp->obstruction = val;
    update_3d_vol(smp);
}

void AIL_set_3D_sample_occlusion(H3DSAMPLE s, float val) {
    Sample3D* smp = get_sample3d(s);
    if (!smp) return;
    smp->occlusion = val;
    update_3d_vol(smp);
}

float AIL_3D_sample_obstruction(H3DSAMPLE s) {
    Sample3D* smp = get_sample3d(s);
    return smp ? smp->obstruction : 0.0f;
}

float AIL_3D_sample_occlusion(H3DSAMPLE s) {
    Sample3D* smp = get_sample3d(s);
    return smp ? smp->occlusion : 0.0f;
}

void AIL_set_3D_sample_effects_level(H3DSAMPLE s, float level) {
    (void)s; (void)level;
}

// ---------------------------------------------------------------------------
// 3D positioning
// ---------------------------------------------------------------------------

void AIL_set_3D_position(H3DPOBJECT o, float x, float y, float z) {
    if (o == LISTENER_HANDLE) {
        fprintf(stderr, "[audio] set_3d_position: LISTENER (%.2f, %.2f, %.2f)\n", x, y, z);
        if (g_engine_initialized) ma_engine_listener_set_position(&g_engine, 0u, x, y, z);
        return;
    }
    fprintf(stderr, "[audio] set_3d_position: handle=%u (%.2f, %.2f, %.2f)\n", (uint32_t)o, x, y, z);
    Sample3D* smp = get_sample3d((H3DSAMPLE)o);
    if (smp && smp->sound_initialized) ma_sound_set_position(&smp->sound, x, y, z);
}

void AIL_set_3D_velocity(H3DPOBJECT o, float x, float y, float z, float s) {
    (void)s;
    if (o == LISTENER_HANDLE) {
        if (g_engine_initialized) ma_engine_listener_set_velocity(&g_engine, 0u, x, y, z);
        return;
    }
    Sample3D* smp = get_sample3d((H3DSAMPLE)o);
    if (smp && smp->sound_initialized) ma_sound_set_velocity(&smp->sound, x, y, z);
}

void AIL_set_3D_velocity_vector(H3DSAMPLE s, float x, float y, float z) {
    Sample3D* smp = get_sample3d(s);
    if (smp && smp->sound_initialized) ma_sound_set_velocity(&smp->sound, x, y, z);
}

void AIL_set_3D_orientation(H3DPOBJECT o, float fx, float fy, float fz, float ux, float uy, float uz) {
    if (o == LISTENER_HANDLE && g_engine_initialized) {
        ma_engine_listener_set_direction(&g_engine, 0u, fx, fy, fz);
        ma_engine_listener_set_world_up(&g_engine, 0u, ux, uy, uz);
    }
}

// ---------------------------------------------------------------------------
// Stream implementations
// ---------------------------------------------------------------------------

static HSTREAM open_stream_internal(const char* fname) {
    if (!fname) return (HSTREAM)-1;
    fprintf(stderr, "[audio] open_stream: \"%s\" (vfs=%s)\n", fname, g_file_open_cb ? "yes" : "no");
    for (int32_t i = 0; i < MAX_STREAMS; i++) {
        if (!g_streams[i].in_use) {
            Stream* str = &g_streams[i];
            *str = Stream{};
            str->in_use = true;
            ma_result res;
            if (g_file_open_cb)
                res = ma_decoder_init_vfs(&g_mss_vfs, fname, nullptr, &str->decoder);
            else
                res = ma_decoder_init_file(fname, nullptr, &str->decoder);
            fprintf(stderr, "[audio] open_stream: \"%s\" decoder=%s\n", fname, (res == MA_SUCCESS) ? "OK" : "FAILED");
            if (res != MA_SUCCESS) { str->in_use = false; return (HSTREAM)-1; }
            str->decoder_initialized = true;
            ma_uint32 rate = 44100u;
            ma_data_source_get_data_format(&str->decoder, nullptr, nullptr, &rate, nullptr, 0u);
            str->native_rate  = (int32_t)rate;
            str->current_rate = str->native_rate;
            res = ma_sound_init_from_data_source(&g_engine, &str->decoder,
                MA_SOUND_FLAG_NO_SPATIALIZATION, nullptr, &str->sound);
            fprintf(stderr, "[audio] open_stream: \"%s\" sound_init=%s handle=%d\n",
                fname, (res == MA_SUCCESS) ? "OK" : "FAILED", i + 1);
            if (res != MA_SUCCESS) {
                ma_decoder_uninit(&str->decoder);
                str->decoder_initialized = false;
                str->in_use = false;
                return (HSTREAM)-1;
            }
            str->sound_initialized = true;
            ma_sound_set_volume(&str->sound, miles_vol_to_ma(str->volume));
            ma_sound_set_pan(&str->sound, miles_pan_to_ma(str->pan));
            return (HSTREAM)(uint32_t)(i + 1);
        }
    }
    fprintf(stderr, "[audio] open_stream: POOL FULL for \"%s\"\n", fname);
    return (HSTREAM)-1;
}

HSTREAM AIL_open_stream(HDIGDRIVER h, const char* fname, INT32 type) {
    (void)h; (void)type;
    return open_stream_internal(fname);
}

HSTREAM AIL_open_stream_by_sample(HDIGDRIVER h, HSAMPLE samp, const char* fname, INT32 type) {
    (void)h; (void)samp; (void)type;
    return open_stream_internal(fname);
}

void AIL_close_stream(HSTREAM s) {
    Stream* str = get_stream(s);
    if (!str) return;
    uninit_stream(str);
    str->in_use = false;
}

void AIL_start_stream(HSTREAM s) {
    Stream* str = get_stream(s);
    fprintf(stderr, "[audio] start_stream: handle=%u sound_initialized=%s\n",
        (uint32_t)s, (str && str->sound_initialized) ? "true" : "false");
    if (!str || !str->sound_initialized) return;
    ma_sound_seek_to_pcm_frame(&str->sound, 0u);
    ma_sound_set_looping(&str->sound, (str->loop_count == 0) ? MA_TRUE : MA_FALSE);
    ma_sound_start(&str->sound);
}

void AIL_pause_stream(HSTREAM s, INT32 onoff) {
    Stream* str = get_stream(s);
    if (!str || !str->sound_initialized) return;
    if (onoff == 0) ma_sound_start(&str->sound);
    else            ma_sound_stop(&str->sound);
}

INT32 AIL_stream_status(HSTREAM s) {
    Stream* str = get_stream(s);
    if (!str || !str->sound_initialized) return SMP_DONE;
    if (ma_sound_is_playing(&str->sound)) return SMP_PLAYING;
    return ma_sound_at_end(&str->sound) ? SMP_DONE : SMP_STOPPED;
}

void AIL_set_stream_volume(HSTREAM s, INT32 vol) {
    Stream* str = get_stream(s);
    if (!str) return;
    if (str->volume == vol) return;
    str->volume = vol;
    if (str->sound_initialized) ma_sound_set_volume(&str->sound, miles_vol_to_ma(vol));
}

INT32 AIL_stream_volume(HSTREAM s) {
    Stream* str = get_stream(s);
    return str ? str->volume : 0;
}

void AIL_set_stream_pan(HSTREAM s, INT32 pan) {
    Stream* str = get_stream(s);
    if (!str) return;
    str->pan = pan;
    if (str->sound_initialized) ma_sound_set_pan(&str->sound, miles_pan_to_ma(pan));
}

INT32 AIL_stream_pan(HSTREAM s) {
    Stream* str = get_stream(s);
    return str ? str->pan : 64;
}

void AIL_set_stream_loop_count(HSTREAM s, INT32 n) {
    Stream* str = get_stream(s);
    if (!str) return;
    str->loop_count = n;
    if (str->sound_initialized)
        ma_sound_set_looping(&str->sound, (n == 0) ? MA_TRUE : MA_FALSE);
}

INT32 AIL_stream_loop_count(HSTREAM s) {
    Stream* str = get_stream(s);
    return str ? str->loop_count : 0;
}

void AIL_set_stream_playback_rate(HSTREAM s, INT32 rate) {
    Stream* str = get_stream(s);
    if (!str) return;
    str->current_rate = rate;
    if (str->sound_initialized && str->native_rate > 0)
        ma_sound_set_pitch(&str->sound, (float)rate / (float)str->native_rate);
}

INT32 AIL_stream_playback_rate(HSTREAM s) {
    Stream* str = get_stream(s);
    return str ? str->current_rate : 44100;
}

void AIL_set_stream_ms_position(HSTREAM s, UINT32 ms) {
    Stream* str = get_stream(s);
    if (!str || !str->sound_initialized || str->native_rate <= 0) return;
    ma_uint64 frame = (ma_uint64)ms * (ma_uint64)str->native_rate / 1000u;
    ma_sound_seek_to_pcm_frame(&str->sound, frame);
}

void AIL_stream_ms_position(HSTREAM s, INT32* len, INT32* pos) {
    Stream* str = get_stream(s);
    if (!str || !str->sound_initialized || str->native_rate <= 0) {
        if (len) *len = 0;
        if (pos) *pos = 0;
        return;
    }
    ma_uint64 cursor = 0u, total = 0u;
    ma_sound_get_cursor_in_pcm_frames(&str->sound, &cursor);
    ma_sound_get_length_in_pcm_frames(&str->sound, &total);
    if (pos) *pos = (INT32)(cursor * 1000u / (ma_uint64)str->native_rate);
    if (len) *len = (INT32)(total  * 1000u / (ma_uint64)str->native_rate);
}

void AIL_set_named_stream_attribute(HSTREAM s, const char* name, INT32 val) {
    (void)s; (void)name; (void)val;
}

void AIL_set_stream_loop_block(HSTREAM s, INT32 start, INT32 end) {
    (void)s; (void)start; (void)end;
}

// ---------------------------------------------------------------------------
// File callbacks
// ---------------------------------------------------------------------------

void AIL_set_file_callbacks(
    U32  (*open_cb)(char const*, uintptr_t*),
    void (*close_cb)(uintptr_t),
    S32  (*seek_cb)(uintptr_t, S32, U32),
    U32  (*read_cb)(uintptr_t, void*, U32))
{
    fprintf(stderr, "[audio] set_file_callbacks: open=%p close=%p seek=%p read=%p\n",
        (void*)open_cb, (void*)close_cb, (void*)seek_cb, (void*)read_cb);
    g_file_open_cb  = open_cb;
    g_file_close_cb = close_cb;
    g_file_seek_cb  = seek_cb;
    g_file_read_cb  = read_cb;
}

// ---------------------------------------------------------------------------
// Timer implementations
// ---------------------------------------------------------------------------

HTIMER AIL_register_timer(void* fn) {
    for (int32_t i = 0; i < MAX_TIMERS; i++) {
        if (!g_timers[i].in_use) {
            g_timers[i] = Timer{};
            g_timers[i].in_use = true;
            g_timers[i].cb     = (AILTIMERCB)fn;
            fprintf(stderr, "[audio] register_timer: slot=%d fn=%p\n", i, fn);
            return (HTIMER)(uint32_t)(i + 1);
        }
    }
    fprintf(stderr, "[audio] register_timer: POOL FULL\n");
    return (HTIMER)-1;
}

void AIL_release_timer_handle(HTIMER t) {
    if (t == 0u || t == (HTIMER)-1) return;
    size_t idx = (size_t)t - 1u;
    if (idx >= (size_t)MAX_TIMERS) return;
    g_timers[idx].running = false;
    g_timers[idx].in_use  = false;
}

void AIL_start_timer(HTIMER t) {
    if (t == 0u || t == (HTIMER)-1) return;
    size_t idx = (size_t)t - 1u;
    if (idx >= (size_t)MAX_TIMERS || !g_timers[idx].in_use || g_timers[idx].running) return;
    fprintf(stderr, "[audio] start_timer: slot=%zu period_ms=%u\n", idx, g_timers[idx].period_ms);
    g_timers[idx].running = true;
    pthread_t th;
    pthread_create(&th, nullptr, timer_thread_func, &g_timers[idx]);
    pthread_detach(th);
}

void AIL_stop_timer(HTIMER t) {
    if (t == 0u || t == (HTIMER)-1) return;
    size_t idx = (size_t)t - 1u;
    if (idx >= (size_t)MAX_TIMERS) return;
    g_timers[idx].running = false;
}

void AIL_set_timer_period(HTIMER t, UINT32 period) {
    if (t == 0u || t == (HTIMER)-1) return;
    size_t idx = (size_t)t - 1u;
    if (idx >= (size_t)MAX_TIMERS) return;
    g_timers[idx].period_ms = period;
}

// ---------------------------------------------------------------------------
// Error functions
// ---------------------------------------------------------------------------

char* AIL_last_error() {
    return (char*)"";
}

INT32 AIL_error_string(INT32 err) {
    (void)err;
    return 0;
}
