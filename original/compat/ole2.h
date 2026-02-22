// ole2.h compat stub for macOS - OLE2 / COM interface stubs (dead WOL browser code)
#pragma once
#ifndef OLE2_H_COMPAT
#define OLE2_H_COMPAT

#include "oaidl.h"
#include "windef.h"

// Prevent duplicate definition
#ifndef COM_NO_WINDOWS_H
#define COM_NO_WINDOWS_H
#endif

// COM initialization stubs
inline HRESULT OleInitialize(void* reserved) { return S_OK; }
inline void    OleUninitialize(void) {}
inline HRESULT CoInitialize(void* reserved) { return S_OK; }
inline HRESULT CoInitializeEx(void* reserved, DWORD flags) { return S_OK; }
inline void    CoUninitialize(void) {}

// Object creation stubs
inline HRESULT CoCreateInstance(const GUID& clsid, void* outer, DWORD context,
                                 const GUID& iid, void** ppv) {
    if (ppv) *ppv = nullptr;
    return E_NOINTERFACE;
}

// IStream (minimal stub for OLE streaming)
struct IStream : public IUnknown {
    virtual HRESULT Read(void* buf, uint32_t cb, uint32_t* read) = 0;
    virtual HRESULT Write(const void* buf, uint32_t cb, uint32_t* written) = 0;
    virtual HRESULT Seek(LARGE_INTEGER dlibMove, DWORD origin, ULARGE_INTEGER* pos) = 0;
    virtual HRESULT SetSize(ULARGE_INTEGER libNewSize) = 0;
    virtual HRESULT CopyTo(IStream* pstm, ULARGE_INTEGER cb, ULARGE_INTEGER* read, ULARGE_INTEGER* written) = 0;
    virtual HRESULT Commit(DWORD flags) = 0;
    virtual HRESULT Revert(void) = 0;
    virtual HRESULT LockRegion(ULARGE_INTEGER offset, ULARGE_INTEGER cb, DWORD type) = 0;
    virtual HRESULT UnlockRegion(ULARGE_INTEGER offset, ULARGE_INTEGER cb, DWORD type) = 0;
    virtual HRESULT Stat(void* pstatstg, DWORD flags) = 0;
    virtual HRESULT Clone(IStream** ppstm) = 0;
};

// OLE string helpers
inline BSTR SysAllocStringLen(const OLECHAR* src, UINT len) { return nullptr; }
inline UINT SysStringLen(BSTR str) { return 0; }

#endif // OLE2_H_COMPAT
