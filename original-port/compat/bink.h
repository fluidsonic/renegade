// bink.h compat shim for macOS - Bink video stubs
#pragma once
#ifndef BINK_H_COMPAT
#define BINK_H_COMPAT

#include "windef.h"

typedef void* HBINK;
typedef void* HBINKTRACK;

typedef struct _BINK {
    UINT Width, Height;
    UINT Frames, FrameNum;
    UINT FrameRate, FrameRateDiv;
    UINT ReadError;
    UINT OpenFlags;
} BINK, *LPBINK;

typedef struct _BINKTRACK {
    UINT MaxSize;
    UINT ReadError;
    UINT Frequency;
    UINT Bits;
    UINT Channels;
} BINKTRACK, *LPBINKTRACK;

// All Bink functions are no-ops
inline HBINK     BinkOpen(const char* name, UINT flags) { return NULL; }
inline void      BinkClose(HBINK bink) {}
inline int       BinkDoFrame(HBINK bink) { return 0; }
inline void      BinkNextFrame(HBINK bink) {}
inline int       BinkWait(HBINK bink) { return 0; }
inline int       BinkCopyToBuffer(HBINK bink, void* dest, long destpitch, UINT destheight, UINT destx, UINT desty, UINT flags) { return 0; }
inline void      BinkGoto(HBINK bink, UINT framenum, int flags) {}
inline UINT      BinkGetTrackMaxSize(HBINK bink, UINT track) { return 0; }
inline UINT      BinkGetTrackData(HBINK bink, UINT track) { return 0; }
inline UINT      BinkGetTrackType(HBINK bink, UINT track) { return 0; }
inline UINT      BinkGetNumTracks(HBINK bink) { return 0; }
inline UINT      BinkGetTrackID(HBINK bink, UINT track) { return 0; }
inline void      BinkSetVolume(HBINK bink, UINT track, int vol) {}
inline void      BinkSetPan(HBINK bink, UINT track, int pan) {}

// Bink flags
#define BINKPRELOADALL       0x00000001L
#define BINKSNDTRACK         0x00004000L
#define BINKNOFILLIOBUF      0x00010000L
#define BINKOLDFRAMEFORMAT   0x00020000L
#define BINKRBINVERT         0x00040000L
#define BINKGRAYSCALE        0x00020000L
#define BINK_COPY_COLOR_ONLY 0x00000001L

#endif // BINK_H_COMPAT
