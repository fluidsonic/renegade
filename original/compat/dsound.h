// dsound.h compat shim for macOS - DirectSound stubs
#pragma once
#ifndef DSOUND_H_COMPAT
#define DSOUND_H_COMPAT

#include "windef.h"
#include "d3d8types.h"
#include "mmsystem.h"

typedef struct _DSBUFFERDESC {
    DWORD dwSize, dwFlags, dwBufferBytes, dwReserved;
    LPWAVEFORMATEX lpwfxFormat;
} DSBUFFERDESC, *LPDSBUFFERDESC;

#define DSBCAPS_PRIMARYBUFFER       0x00000001
#define DSBCAPS_CTRLFREQUENCY       0x00000020
#define DSBCAPS_CTRLPAN             0x00000040
#define DSBCAPS_CTRLVOLUME          0x00000080
#define DSBCAPS_CTRLDEFAULT         0x000000E0
#define DSBCAPS_GLOBALFOCUS         0x00008000
#define DSBCAPS_GETCURRENTPOSITION2 0x00010000
#define DSSCL_NORMAL                0x00000001
#define DSSCL_PRIORITY              0x00000002
#define DSSCL_EXCLUSIVE             0x00000003

#define DSBPLAY_LOOPING             0x00000001
#define DSBSTATUS_PLAYING           0x00000001
#define DSBSTATUS_LOOPING           0x00000004

struct IDirectSoundBuffer : public IUnknown {
    virtual HRESULT GetCaps(void* p) { return E_NOTIMPL; }
    virtual HRESULT GetCurrentPosition(LPDWORD pPlay, LPDWORD pWrite) { return E_NOTIMPL; }
    virtual HRESULT GetFormat(LPWAVEFORMATEX pwfx, DWORD size, LPDWORD pWritten) { return E_NOTIMPL; }
    virtual HRESULT GetVolume(LPLONG plVol) { return E_NOTIMPL; }
    virtual HRESULT GetPan(LPLONG plPan) { return E_NOTIMPL; }
    virtual HRESULT GetFrequency(LPDWORD pdwFreq) { return E_NOTIMPL; }
    virtual HRESULT GetStatus(LPDWORD pdwStatus) { return E_NOTIMPL; }
    virtual HRESULT Initialize(void* pDS, LPDSBUFFERDESC pcDesc) { return E_NOTIMPL; }
    virtual HRESULT Lock(DWORD offset, DWORD bytes, LPVOID* ppv1, LPDWORD pdw1, LPVOID* ppv2, LPDWORD pdw2, DWORD flags) { return E_NOTIMPL; }
    virtual HRESULT Play(DWORD res1, DWORD prio, DWORD flags) { return E_NOTIMPL; }
    virtual HRESULT SetCurrentPosition(DWORD pos) { return E_NOTIMPL; }
    virtual HRESULT SetFormat(const WAVEFORMATEX* pwfx) { return E_NOTIMPL; }
    virtual HRESULT SetVolume(LONG lVol) { return E_NOTIMPL; }
    virtual HRESULT SetPan(LONG lPan) { return E_NOTIMPL; }
    virtual HRESULT SetFrequency(DWORD dwFreq) { return E_NOTIMPL; }
    virtual HRESULT Stop() { return E_NOTIMPL; }
    virtual HRESULT Unlock(LPVOID pv1, DWORD dw1, LPVOID pv2, DWORD dw2) { return E_NOTIMPL; }
    virtual HRESULT Restore() { return E_NOTIMPL; }
};
typedef IDirectSoundBuffer* LPDIRECTSOUNDBUFFER;

struct IDirectSound : public IUnknown {
    virtual HRESULT CreateSoundBuffer(LPDSBUFFERDESC pcDesc, IDirectSoundBuffer** ppBuf, IUnknown* pUnk) { return E_NOTIMPL; }
    virtual HRESULT GetCaps(void* p) { return E_NOTIMPL; }
    virtual HRESULT DuplicateSoundBuffer(IDirectSoundBuffer* pSrc, IDirectSoundBuffer** ppDup) { return E_NOTIMPL; }
    virtual HRESULT SetCooperativeLevel(HWND hwnd, DWORD level) { return E_NOTIMPL; }
    virtual HRESULT Compact() { return E_NOTIMPL; }
    virtual HRESULT GetSpeakerConfig(LPDWORD pdw) { return E_NOTIMPL; }
    virtual HRESULT SetSpeakerConfig(DWORD dw) { return E_NOTIMPL; }
    virtual HRESULT Initialize(const GUID* pGuid) { return E_NOTIMPL; }
};
typedef IDirectSound* LPDIRECTSOUND;

inline HRESULT DirectSoundCreate(const GUID* pGuid, IDirectSound** ppDS, IUnknown* pUnk) {
    if (ppDS) *ppDS = NULL; return E_NOTIMPL;
}

#endif // DSOUND_H_COMPAT
