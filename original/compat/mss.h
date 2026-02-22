#pragma once

#include "global.h"

// mss.h compat shim for macOS - Miles Sound System stubs

#include "winnt.h"
#include "tchar.h"
#include "mmsystem.h"
#include <string.h>

// MSS basic types (Miles Sound System uses these names)
typedef signed   char   INT8;
typedef unsigned char   UINT8;
typedef signed   short  INT16;
typedef unsigned short  UINT16;
typedef signed   int    INT32;
typedef unsigned int    UINT32;
typedef float           FLOAT32;
typedef float           F32;

// Short-form aliases
typedef unsigned int    U32;
typedef signed   int    S32;
typedef unsigned short  U16;
typedef signed   short  S16;
typedef unsigned char   U8;
typedef signed   char   S8;

// Base Miles handle type (code uses MILES_HANDLE for casts)
typedef uintptr_t MILES_HANDLE;

// AILCALLBACK calling convention (empty on non-Windows)
#define AILCALLBACK

// HDIGDRIVER is a real pointer to an internal MSS struct (code accesses ->emulated_ds)
struct _AIL_DIG_DRIVER_STRUCT {
    UINT32 emulated_ds;
};
typedef struct _AIL_DIG_DRIVER_STRUCT* HDIGDRIVER;

// Other MSS handle types - opaque handles (uintptr_t)
typedef uintptr_t   HDLSDEVICE;
typedef uintptr_t   HMDIDRIVER;
typedef uintptr_t   HSAMPLE;
typedef uintptr_t   HSEQUENCE;
typedef uintptr_t   H3DPOBJECT;
typedef uintptr_t   H3DSAMPLE;
typedef uintptr_t   HPROVIDER;
typedef uintptr_t   HASIDRIVER;
typedef uintptr_t   HTIMER;
typedef uintptr_t   HSTREAM;
typedef uintptr_t   HSSDECODER;
typedef uintptr_t   HSSENCODER;
typedef uintptr_t   HATTRIB;
typedef uintptr_t   HPROEFSYS;

// Provider enumeration handle
typedef UINT32      HPROENUM;
#define HPROENUM_FIRST  0

// MSS result codes
typedef int     MSSERROR;
#define MSS_NO_ERROR    0
#define MSS_ERROR       (-1)
#define AIL_NO_ERROR    MSS_NO_ERROR

// 3D provider result codes
#define M3D_NOERR       0

// Sample attributes
#define SMP_PLAYING     1
#define SMP_STOPPED     0
#define SMP_DONE        2

// Volume/pan constants
#define MSS_VOL_MAX     127
#define MSS_VOL_0       0

// 3D speaker types
#define AIL_3D_2_SPEAKER    0
#define AIL_3D_HEADPHONE    1
#define AIL_3D_SURROUND     2
#define AIL_3D_4_SPEAKER    3

// 3D room types
#define ENVIRONMENT_GENERIC     0

// Digital processing filter type
#define DP_FILTER               1

// Preference constants
#define AIL_LOCK_PROTECTION     1
#define DIG_USE_WAVEOUT         2
#define NO                      0

// 3D provider constants
#define MILES_PROVIDER_NONE ((HPROVIDER)0)

// AILSOUNDINFO - sound file info struct
typedef struct {
    UINT32 format;
    void*  data_ptr;
    UINT32 data_len;
    UINT32 rate;
    INT32  bits;
    INT32  channels;
    UINT32 samples;
    UINT32 block_size;
    void*  initial_ptr;
} AILSOUNDINFO;

// Wave format tags
#define WAVE_FORMAT_IMA_ADPCM   0x0011

// Core API
MSSERROR    AIL_startup();
void        AIL_shutdown();
void        AIL_lock();
void        AIL_unlock();
HDIGDRIVER  AIL_open_digital_driver(UINT32 freq, INT32 bits, INT32 chans, UINT32 flags);
void        AIL_close_digital_driver(HDIGDRIVER h);

// WaveOut device open/close (2D driver)
S32         AIL_waveOutOpen(HDIGDRIVER* driver, void* hwnd, UINT32 num, LPWAVEFORMAT pwf);
void        AIL_waveOutClose(HDIGDRIVER h);

// Preference
S32         AIL_set_preference(UINT32 pref, INT32 val);

// Sample allocation
HSAMPLE     AIL_allocate_sample_handle(HDIGDRIVER h);
void        AIL_release_sample_handle(HSAMPLE s);
void        AIL_init_sample(HSAMPLE s);
void        AIL_start_sample(HSAMPLE s);
void        AIL_stop_sample(HSAMPLE s);
void        AIL_resume_sample(HSAMPLE s);
void        AIL_end_sample(HSAMPLE s);
INT32       AIL_sample_status(HSAMPLE s);
void        AIL_set_sample_volume(HSAMPLE s, INT32 vol);
INT32       AIL_sample_volume(HSAMPLE s);
void        AIL_set_sample_pan(HSAMPLE s, INT32 pan);
INT32       AIL_sample_pan(HSAMPLE s);
void        AIL_set_sample_loop_count(HSAMPLE s, INT32 n);
INT32       AIL_sample_loop_count(HSAMPLE s);
void        AIL_set_sample_address(HSAMPLE s, const void* addr, UINT32 len);
void        AIL_set_sample_type(HSAMPLE s, INT32 format, UINT32 flags);
void        AIL_set_sample_playback_rate(HSAMPLE s, INT32 rate);
INT32       AIL_sample_playback_rate(HSAMPLE s);
UINT32      AIL_sample_position(HSAMPLE s);
INT32       AIL_WAV_info(const void* data, AILSOUNDINFO* info, size_t buf_size);
void        AIL_set_named_sample_file(HSAMPLE s, const char* name, const void* data, UINT32 len, INT32 flags);
void        AIL_set_sample_ms_position(HSAMPLE s, UINT32 ms);
void        AIL_sample_ms_position(HSAMPLE s, INT32* len, INT32* pos);
void        AIL_set_sample_user_data(HSAMPLE s, INT32 index, void* val);
void*       AIL_sample_user_data(HSAMPLE s, INT32 index);
void        AIL_set_sample_processor(HSAMPLE s, INT32 proc_type, HPROVIDER hp);
void        AIL_set_filter_sample_preference(HSAMPLE s, const char* name, void* val);

// 3D provider enumeration and management (HPROVIDER-based)
S32         AIL_enumerate_3D_providers(HPROENUM* next, HPROVIDER* p, char** name);
S32         AIL_enumerate_filters(HPROENUM* next, HPROVIDER* p, char** name);
MSSERROR    AIL_open_3D_provider(HPROVIDER p);
void        AIL_close_3D_provider(HPROVIDER p);
void        AIL_set_3D_speaker_type(HPROVIDER p, S32 type);

// 3D listener/sample (HPROVIDER-based allocation, AIL_3D_ naming convention)
H3DPOBJECT  AIL_3D_open_listener(HPROVIDER p);
H3DSAMPLE   AIL_allocate_3D_sample_handle(HPROVIDER p);
void        AIL_release_3D_sample_handle(H3DSAMPLE s);
void        AIL_start_3D_sample(H3DSAMPLE s);
void        AIL_stop_3D_sample(H3DSAMPLE s);
void        AIL_end_3D_sample(H3DSAMPLE s);
INT32       AIL_3D_sample_status(H3DSAMPLE s);

// 3D object user data (stores void* associated with H3DSAMPLE)
void*       AIL_3D_object_user_data(H3DSAMPLE s, UINT32 index);
void        AIL_set_3D_object_user_data(H3DSAMPLE s, UINT32 index, void* val);

// 3D sample file loading and playback control
U32         AIL_set_3D_sample_file(H3DSAMPLE s, void* data);
void        AIL_resume_3D_sample(H3DSAMPLE s);
void        AIL_set_3D_sample_offset(H3DSAMPLE s, U32 bytes);
U32         AIL_3D_sample_offset(H3DSAMPLE s);
U32         AIL_3D_sample_length(H3DSAMPLE s);

// 3D positioning (H3DPOBJECT covers both listener and sample objects)
void        AIL_set_3D_position(H3DPOBJECT o, float x, float y, float z);
void        AIL_set_3D_velocity(H3DPOBJECT o, float x, float y, float z, float s);
void        AIL_set_3D_velocity_vector(H3DSAMPLE s, float x, float y, float z);
void        AIL_set_3D_orientation(H3DPOBJECT o, float fx, float fy, float fz, float ux, float uy, float uz);
void        AIL_set_3D_sample_volume(H3DSAMPLE s, INT32 vol);
INT32       AIL_3D_sample_volume(H3DSAMPLE s);
void        AIL_set_3D_sample_distances(H3DSAMPLE s, float min_d, float max_d);
void        AIL_set_3D_sample_cone(H3DSAMPLE s, float inner, float outer, INT32 outer_vol);
INT32       AIL_3D_sample_playback_rate(H3DSAMPLE s);
void        AIL_set_3D_sample_playback_rate(H3DSAMPLE s, INT32 rate);
UINT32      AIL_3D_sample_position(H3DSAMPLE s);
void        AIL_set_3D_sample_loop_count(H3DSAMPLE s, INT32 n);
INT32       AIL_3D_sample_loop_count(H3DSAMPLE s);
void        AIL_set_3D_sample_address(H3DSAMPLE s, const void* addr, UINT32 len);
void        AIL_set_3D_sample_type(H3DSAMPLE s, INT32 format, UINT32 flags);
void        AIL_set_3D_user_data(H3DSAMPLE s, UINT32 index, INT32 val);
INT32       AIL_3D_user_data(H3DSAMPLE s, UINT32 index);
void        AIL_set_3D_sample_obstruction(H3DSAMPLE s, float val);
void        AIL_set_3D_sample_occlusion(H3DSAMPLE s, float val);
float       AIL_3D_sample_obstruction(H3DSAMPLE s);
float       AIL_3D_sample_occlusion(H3DSAMPLE s);
void        AIL_set_3D_room_type(HPROVIDER p, INT32 type);
INT32       AIL_3D_room_type(HPROVIDER p);
void        AIL_set_3D_sample_effects_level(H3DSAMPLE s, float level);

// Timer
HTIMER      AIL_register_timer(void* fn);
void        AIL_release_timer_handle(HTIMER t);
void        AIL_start_timer(HTIMER t);
void        AIL_stop_timer(HTIMER t);
void        AIL_set_timer_period(HTIMER t, UINT32 period);
UINT32      AIL_ms_count();

// Stream
HSTREAM     AIL_open_stream(HDIGDRIVER h, const char* fname, INT32 type);
HSTREAM     AIL_open_stream_by_sample(HDIGDRIVER h, HSAMPLE samp, const char* fname, INT32 type);
void        AIL_close_stream(HSTREAM s);
void        AIL_start_stream(HSTREAM s);
void        AIL_pause_stream(HSTREAM s, INT32 onoff);
INT32       AIL_stream_status(HSTREAM s);
void        AIL_set_stream_volume(HSTREAM s, INT32 vol);
INT32       AIL_stream_volume(HSTREAM s);
void        AIL_set_stream_pan(HSTREAM s, INT32 pan);
INT32       AIL_stream_pan(HSTREAM s);
void        AIL_set_stream_loop_count(HSTREAM s, INT32 n);
INT32       AIL_stream_loop_count(HSTREAM s);
void        AIL_set_stream_playback_rate(HSTREAM s, INT32 rate);
INT32       AIL_stream_playback_rate(HSTREAM s);
void        AIL_set_named_stream_attribute(HSTREAM s, const char* name, INT32 val);
void        AIL_set_stream_loop_block(HSTREAM s, INT32 start, INT32 end);
void        AIL_set_stream_ms_position(HSTREAM s, UINT32 ms);
void        AIL_stream_ms_position(HSTREAM s, INT32* len, INT32* pos);

// File seek type constants (used in AIL file callbacks)
#define AIL_FILE_SEEK_BEGIN     0
#define AIL_FILE_SEEK_CURRENT   1
#define AIL_FILE_SEEK_END       2

// File I/O callbacks for MSS streaming.
// open_cb:  returns file size (0 = failure), writes opaque file handle into *file_handle
// close_cb: releases the handle
// seek_cb:  seeks to offset/type; returns new position
// read_cb:  reads bytes; returns bytes actually read
// All handles are uintptr_t so that FileClass* can be stored without truncation on 64-bit.
void AIL_set_file_callbacks(
    U32  (*open_cb)(char const*, uintptr_t*),
    void (*close_cb)(uintptr_t),
    S32  (*seek_cb)(uintptr_t, S32, U32),
    U32  (*read_cb)(uintptr_t, void*, U32));

// Error
char*       AIL_last_error();
INT32       AIL_error_string(INT32 err);
