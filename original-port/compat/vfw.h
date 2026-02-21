// vfw.h compat shim for macOS - Video for Windows stub
#pragma once
#ifndef VFW_H_COMPAT
#define VFW_H_COMPAT
#define _INC_VFW

#include "windows.h"

// Video for Windows - all functionality stubbed (not needed on macOS)
typedef struct _AVISTREAMINFOA {
    DWORD fccType;
    DWORD fccHandler;
    DWORD dwFlags;
    DWORD dwCaps;
    WORD  wPriority;
    WORD  wLanguage;
    DWORD dwScale;
    DWORD dwRate;
    DWORD dwStart;
    DWORD dwLength;
    DWORD dwInitialFrames;
    DWORD dwSuggestedBufferSize;
    DWORD dwQuality;
    DWORD dwSampleSize;
    RECT  rcFrame;
    DWORD dwEditCount;
    DWORD dwFormatChangeCount;
    char  szName[64];
} AVISTREAMINFOA, *LPAVISTREAMINFOA;

typedef AVISTREAMINFOA AVISTREAMINFO;
typedef LPAVISTREAMINFOA LPAVISTREAMINFO;

typedef struct _AVIFILEINFOA {
    DWORD dwMaxBytesPerSec;
    DWORD dwFlags;
    DWORD dwCaps;
    DWORD dwStreams;
    DWORD dwSuggestedBufferSize;
    DWORD dwWidth, dwHeight;
    DWORD dwScale, dwRate, dwLength;
    DWORD dwEditCount;
    char  szFileType[64];
} AVIFILEINFOA;

typedef void* PAVIFILE;
typedef void* PAVISTREAM;
typedef void* PGETFRAME;

#define AVIIF_KEYFRAME  0x00000010L
#define streamtypeVIDEO mmioFOURCC('v','i','d','s')
#define OF_WRITE        0x00000001
#define OF_CREATE       0x00001000

// Stub macros
#ifndef MAKEFOURCC
#define MAKEFOURCC(ch0,ch1,ch2,ch3) \
    ((DWORD)(BYTE)(ch0) | ((DWORD)(BYTE)(ch1)<<8) | ((DWORD)(BYTE)(ch2)<<16) | ((DWORD)(BYTE)(ch3)<<24))
#endif
#ifndef mmioFOURCC
#define mmioFOURCC(c0,c1,c2,c3) MAKEFOURCC(c0,c1,c2,c3)
#endif

// GlobalAllocPtr / GlobalFreePtr macros from windowsx.h
#ifndef GlobalAllocPtr
#define GlobalAllocPtr(flags, cb)  ((void*)::GlobalAlloc((flags), (cb)))
#define GlobalFreePtr(lp)          (::GlobalFree((HGLOBAL)(lp)) == NULL)
#endif

inline HRESULT AVIFileInit(void) { return S_OK; }
inline void    AVIFileExit(void) {}
inline HRESULT AVIFileOpen(PAVIFILE*, const char*, UINT, void*) { return E_NOTIMPL; }
inline ULONG   AVIFileRelease(PAVIFILE) { return 0; }
inline HRESULT AVIFileGetStream(PAVIFILE, PAVISTREAM*, DWORD, LONG) { return E_NOTIMPL; }
inline HRESULT AVIFileCreateStream(PAVIFILE, PAVISTREAM*, void*) { return E_NOTIMPL; }
inline ULONG   AVIStreamRelease(PAVISTREAM) { return 0; }
inline HRESULT AVIStreamInfo(PAVISTREAM, LPAVISTREAMINFOA, LONG) { return E_NOTIMPL; }
inline LONG    AVIStreamStart(PAVISTREAM) { return 0; }
inline LONG    AVIStreamLength(PAVISTREAM) { return 0; }
inline HRESULT AVIStreamRead(PAVISTREAM, LONG, LONG, void*, LONG, LONG*, LONG*) { return E_NOTIMPL; }
inline HRESULT AVIStreamSetFormat(PAVISTREAM, LONG, void*, LONG) { return E_NOTIMPL; }
inline HRESULT AVIStreamWrite(PAVISTREAM, LONG, LONG, void*, LONG, DWORD, LONG*, LONG*) { return E_NOTIMPL; }
inline LONG    AVIStreamSampleToTime(PAVISTREAM, LONG) { return 0; }
inline LONG    AVIStreamTimeToSample(PAVISTREAM, LONG) { return 0; }
inline PGETFRAME AVIStreamGetFrameOpen(PAVISTREAM, void*) { return NULL; }
inline void*   AVIStreamGetFrame(PGETFRAME, LONG) { return NULL; }
inline HRESULT AVIStreamGetFrameClose(PGETFRAME) { return E_NOTIMPL; }

#endif // VFW_H_COMPAT
