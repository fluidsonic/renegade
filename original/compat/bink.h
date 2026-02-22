#pragma once

#include "global.h"

// bink.h compat shim for macOS - Bink video stubs


typedef struct _BINK* HBINK;
typedef struct _BINKTRACK* HBINKTRACK;

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

// Bink API -- implemented via FFmpeg in compat/bink_impl.cpp
extern HBINK     BinkOpen(const char* name, UINT flags);
extern void      BinkClose(HBINK bink);
extern int32_t   BinkDoFrame(HBINK bink);
extern void      BinkNextFrame(HBINK bink);
extern int32_t   BinkWait(HBINK bink);
extern int32_t   BinkCopyToBuffer(HBINK bink, void* dest, int32_t destpitch, UINT destheight, UINT destx, UINT desty, UINT flags);
extern void      BinkGoto(HBINK bink, UINT framenum, int32_t flags);
extern UINT      BinkGetTrackMaxSize(HBINK bink, UINT track);
extern UINT      BinkGetTrackData(HBINK bink, UINT track);
extern UINT      BinkGetTrackType(HBINK bink, UINT track);
extern UINT      BinkGetNumTracks(HBINK bink);
extern UINT      BinkGetTrackID(HBINK bink, UINT track);
extern void      BinkSetVolume(HBINK bink, UINT track, int32_t vol);
extern void      BinkSetPan(HBINK bink, UINT track, int32_t pan);
extern void      BinkSoundUseDirectSound(UINT device);

// Bink copy/surface flags
#define BINKSURFACE565       0x00000010L
#define BINKSURFACE555       0x00000020L
#define BINKSURFACE888       0x00000040L
#define BINKCOPYNOSCALING    0x00000100L
#define BINKCOPYNOBLENDING   0x00000200L

// Bink flags
#define BINKPRELOADALL       0x00000001L
#define BINKSNDTRACK         0x00004000L
#define BINKNOFILLIOBUF      0x00010000L
#define BINKOLDFRAMEFORMAT   0x00020000L
#define BINKRBINVERT         0x00040000L
#define BINKGRAYSCALE        0x00020000L
#define BINK_COPY_COLOR_ONLY 0x00000001L
