#pragma once

#include "global.h"

// mmsystem.h compat shim for macOS

#include <sys/time.h>
#include <time.h>

typedef UINT    MMRESULT;
typedef UINT    MMTIME;
typedef UINT*   LPMMTIME;

#define MMSYSERR_NOERROR    0
#define MMSYSERR_ERROR      1
#define MMSYSERR_BADDEVICEID 2

// WAVEFORMAT (original, without the EX extension)
#ifndef WAVEFORMAT_DEFINED
#define WAVEFORMAT_DEFINED
typedef struct _WAVEFORMAT {
    WORD  wFormatTag;
    WORD  nChannels;
    DWORD nSamplesPerSec;
    DWORD nAvgBytesPerSec;
    WORD  nBlockAlign;
} WAVEFORMAT, *PWAVEFORMAT, *NPWAVEFORMAT, *LPWAVEFORMAT;

// PCMWAVEFORMAT (WAVEFORMAT + wBitsPerSample)
typedef struct _PCMWAVEFORMAT {
    WAVEFORMAT wf;
    WORD       wBitsPerSample;
} PCMWAVEFORMAT, *PPCMWAVEFORMAT, *LPPCMWAVEFORMAT;
#endif

// WAVEFORMATEX (also needed by dsound)
#ifndef WAVEFORMATEX_DEFINED
#define WAVEFORMATEX_DEFINED
typedef struct _WAVEFORMATEX {
    WORD  wFormatTag;
    WORD  nChannels;
    DWORD nSamplesPerSec;
    DWORD nAvgBytesPerSec;
    WORD  nBlockAlign;
    WORD  wBitsPerSample;
    WORD  cbSize;
} WAVEFORMATEX, *PWAVEFORMATEX, *LPWAVEFORMATEX;
typedef const WAVEFORMATEX* LPCWAVEFORMATEX;
#define WAVE_FORMAT_PCM         1
#define WAVE_FORMAT_IMA_ADPCM   0x0011
#endif

// timeGetTime in milliseconds
inline DWORD timeGetTime() {
    struct timeval tv;
    gettimeofday(&tv, NULL);
    return (DWORD)(tv.tv_sec * 1000 + tv.tv_usec / 1000);
}
#define TIMEGETTIME() timeGetTime()

// Timer callbacks (stubs)
typedef void (*LPTIMECALLBACK)(UINT, UINT, DWORD, DWORD, DWORD);
inline MMRESULT timeSetEvent(UINT delay, UINT resolution, LPTIMECALLBACK fn, DWORD user, UINT flags) { return 0; }
inline MMRESULT timeKillEvent(UINT id) { return MMSYSERR_NOERROR; }
inline MMRESULT timeBeginPeriod(UINT period) { return MMSYSERR_NOERROR; }
inline MMRESULT timeEndPeriod(UINT period) { return MMSYSERR_NOERROR; }

// Multimedia timer flags
#define TIME_ONESHOT    0x0000
#define TIME_PERIODIC   0x0001
#define TIME_CALLBACK_FUNCTION 0x0000
