// ocidl.h compat stub for macOS - OLE Controls interface stubs (unused on macOS)
#pragma once
#ifndef OCIDL_H_COMPAT
#define OCIDL_H_COMPAT

#include "oaidl.h"

// IPersist and related interfaces (minimal stubs)
struct IPersist : public IUnknown {
    virtual HRESULT GetClassID(GUID* pClassID) = 0;
};

struct IPersistStream : public IPersist {
    virtual HRESULT IsDirty(void) = 0;
    virtual HRESULT Load(void* pStm) = 0;
    virtual HRESULT Save(void* pStm, BOOL fClearDirty) = 0;
    virtual HRESULT GetSizeMax(ULARGE_INTEGER* pcbSize) = 0;
};

// IConnectionPoint / IConnectionPointContainer (stubs)
struct IConnectionPoint : public IUnknown {};
struct IConnectionPointContainer : public IUnknown {
    virtual HRESULT EnumConnectionPoints(void** ppEnum) = 0;
    virtual HRESULT FindConnectionPoint(REFIID riid, IConnectionPoint** ppCP) = 0;
};

#endif // OCIDL_H_COMPAT
