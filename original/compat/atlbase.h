// atlbase.h compat shim for macOS - minimal ATL stubs
#pragma once
#ifndef ATLBASE_H_COMPAT
#define ATLBASE_H_COMPAT

#include "windows.h"

// COM support
#ifndef __IUnknown_INTERFACE_DEFINED__
#define __IUnknown_INTERFACE_DEFINED__
struct IUnknown {
    virtual HRESULT QueryInterface(const GUID& riid, void** ppvObject) = 0;
    virtual uint32_t AddRef() = 0;
    virtual uint32_t Release() = 0;
    virtual ~IUnknown() {}
};
#endif

// CComPtr - minimal smart pointer for COM interfaces
template<class T>
class CComPtr {
public:
    T* p;
    CComPtr() : p(nullptr) {}
    CComPtr(T* pT) : p(pT) { if (p) p->AddRef(); }
    CComPtr(const CComPtr<T>& lp) : p(lp.p) { if (p) p->AddRef(); }
    ~CComPtr() { if (p) p->Release(); }
    T* operator->() const { return p; }
    T& operator*() const { return *p; }
    operator T*() const { return p; }
    T** operator&() { return &p; }
    bool operator!() const { return p == nullptr; }
    bool operator==(T* pT) const { return p == pT; }
    CComPtr<T>& operator=(T* pT) {
        if (p) p->Release();
        p = pT;
        if (p) p->AddRef();
        return *this;
    }
    void Release() { if (p) { p->Release(); p = nullptr; } }
};

// ATL module stub
class CAtlModule {};
class CAtlModuleT {};
extern CAtlModule* _pAtlModule;

// USES_CONVERSION macro stub
#define USES_CONVERSION
#define A2W(x) (u"")
#define W2A(x) ("")
#define T2A(x) (x)
#define A2T(x) (x)
#define OLE2T(x) ("")
#define T2OLE(x) (u"")

// ATL string conversion macros
#define CA2CT(x)  (x)
#define CT2CA(x)  (x)

#endif // ATLBASE_H_COMPAT
