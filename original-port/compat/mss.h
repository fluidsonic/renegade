// mss.h compat shim for macOS - Miles Sound System stubs
#pragma once
#ifndef MSS_H_COMPAT
#define MSS_H_COMPAT

#include "windef.h"

// MSS handle types
typedef void*   HDIGDRIVER;
typedef void*   HDLSDEVICE;
typedef void*   HMDIDRIVER;
typedef void*   HSAMPLE;
typedef void*   HSEQUENCE;
typedef void*   H3DPOBJECT;
typedef void*   H3DSAMPLE;
typedef void*   HPROVIDER;
typedef void*   HASIDRIVER;
typedef void*   HTIMER;
typedef void*   HSTREAM;
typedef void*   HSSDECODER;
typedef void*   HSSENCODER;
typedef void*   HATTRIB;
typedef void*   HPROEFSYS;

// MSS result codes
typedef int     MSSERROR;
#define MSS_NO_ERROR    0
#define MSS_ERROR       (-1)

// Sample attributes
#define SMP_PLAYING     1
#define SMP_STOPPED     0
#define SMP_DONE        2

// Volume/pan constants
#define MSS_VOL_MAX     127
#define MSS_VOL_0       0

// 3D provider constants
#define MILES_PROVIDER_NONE ((HPROVIDER)NULL)

// Core API stubs
inline MSSERROR    AIL_startup()                { return MSS_NO_ERROR; }
inline void        AIL_shutdown()               {}
inline HDIGDRIVER  AIL_open_digital_driver(UINT32 freq, INT32 bits, INT32 chans, UINT32 flags) { return NULL; }
inline void        AIL_close_digital_driver(HDIGDRIVER h) {}
inline HSAMPLE     AIL_allocate_sample_handle(HDIGDRIVER h) { return NULL; }
inline void        AIL_release_sample_handle(HSAMPLE s) {}
inline void        AIL_init_sample(HSAMPLE s) {}
inline void        AIL_start_sample(HSAMPLE s) {}
inline void        AIL_stop_sample(HSAMPLE s) {}
inline void        AIL_resume_sample(HSAMPLE s) {}
inline void        AIL_end_sample(HSAMPLE s) {}
inline INT32       AIL_sample_status(HSAMPLE s) { return SMP_DONE; }
inline void        AIL_set_sample_volume(HSAMPLE s, INT32 vol) {}
inline INT32       AIL_sample_volume(HSAMPLE s) { return 0; }
inline void        AIL_set_sample_pan(HSAMPLE s, INT32 pan) {}
inline INT32       AIL_sample_pan(HSAMPLE s) { return 0; }
inline void        AIL_set_sample_loop_count(HSAMPLE s, INT32 n) {}
inline INT32       AIL_sample_loop_count(HSAMPLE s) { return 0; }
inline void        AIL_set_sample_address(HSAMPLE s, const void* addr, UINT32 len) {}
inline void        AIL_set_sample_type(HSAMPLE s, INT32 format, UINT32 flags) {}
inline void        AIL_set_sample_playback_rate(HSAMPLE s, INT32 rate) {}
inline INT32       AIL_sample_playback_rate(HSAMPLE s) { return 44100; }
inline UINT32      AIL_sample_position(HSAMPLE s) { return 0; }

// 3D stubs
inline H3DPOBJECT  AIL_open_3D_listener(HDIGDRIVER h, INT32 index) { return NULL; }
inline H3DSAMPLE   AIL_allocate_3D_sample_handle(HDIGDRIVER h) { return NULL; }
inline void        AIL_release_3D_sample_handle(H3DSAMPLE s) {}
inline void        AIL_start_3D_sample(H3DSAMPLE s) {}
inline void        AIL_stop_3D_sample(H3DSAMPLE s) {}
inline void        AIL_end_3D_sample(H3DSAMPLE s) {}
inline INT32       AIL_3D_sample_status(H3DSAMPLE s) { return SMP_DONE; }
inline void        AIL_set_3D_position(H3DPOBJECT o, float x, float y, float z) {}
inline void        AIL_set_3D_velocity(H3DPOBJECT o, float x, float y, float z, float s) {}
inline void        AIL_set_3D_orientation(H3DPOBJECT o, float fx, float fy, float fz, float ux, float uy, float uz) {}
inline void        AIL_set_3D_sample_volume(H3DSAMPLE s, INT32 vol) {}
inline INT32       AIL_3D_sample_volume(H3DSAMPLE s) { return 0; }

// Timer
inline HTIMER      AIL_register_timer(void* fn) { return NULL; }
inline void        AIL_release_timer_handle(HTIMER t) {}
inline void        AIL_start_timer(HTIMER t) {}
inline void        AIL_stop_timer(HTIMER t) {}
inline void        AIL_set_timer_period(HTIMER t, UINT32 period) {}
inline UINT32      AIL_ms_count() { return 0; }

// Type aliases MSS uses
typedef signed char     INT8;
typedef unsigned char   UINT8;
typedef signed short    INT16;
typedef unsigned short  UINT16;
typedef signed int      INT32;
typedef unsigned int    UINT32;
typedef float           FLOAT32;

#endif // MSS_H_COMPAT
