// oaidl.h compat shim for macOS - OLE Automation stubs
#pragma once
#ifndef OAIDL_H_COMPAT
#define OAIDL_H_COMPAT

#include "windef.h"
#include "winnt.h"
#include "winbase.h"

// OLECHAR is wchar-like (2-byte) on Windows; use uint16_t
typedef uint16_t OLECHAR;
typedef OLECHAR* LPOLESTR;
typedef const OLECHAR* LPCOLESTR;
#define OLESTR(str) (const OLECHAR*)(str)

// GUID/IID/REFIID — only define if not already defined by d3d8types.h / windef.h
#ifndef GUID_DEFINED
#define GUID_DEFINED
typedef struct _GUID {
    unsigned long  Data1;
    unsigned short Data2, Data3;
    unsigned char  Data4[8];
} GUID, *LPGUID;
typedef GUID IID;
typedef GUID CLSID;
typedef const GUID& REFGUID;
typedef const IID& REFIID;
typedef const CLSID& REFCLSID;
#endif // GUID_DEFINED
#ifndef IID_NULL_DEFINED
#define IID_NULL_DEFINED
static const IID IID_NULL = {0,0,0,{0,0,0,0,0,0,0,0}};
#endif

// Locale constants
#define LOCALE_SYSTEM_DEFAULT 0x0800
#define LOCALE_USER_DEFAULT   0x0400
typedef DWORD LCID;

// DISPID / DISPPARAMS
typedef LONG DISPID;
#define DISPID_VALUE      0
#define DISPID_PROPERTYPUT (-3L)
#define DISPID_NEWENUM    (-4L)

// VARIANT types
#define VT_EMPTY    0
#define VT_NULL     1
#define VT_I2       2
#define VT_I4       3
#define VT_R4       4
#define VT_R8       5
#define VT_BSTR     8
#define VT_BOOL     11
#define VT_VARIANT  12
#define VT_UNKNOWN  13
#define VT_DISPATCH 9
#define VT_UI4      19

typedef short VARIANT_BOOL;
#define VARIANT_TRUE  ((VARIANT_BOOL)-1)
#define VARIANT_FALSE ((VARIANT_BOOL)0)

typedef OLECHAR* BSTR;

// VARIANT union
typedef struct tagVARIANT {
    unsigned short vt;
    unsigned short wReserved1;
    unsigned short wReserved2;
    unsigned short wReserved3;
    union {
        int    intVal;
        float  fltVal;
        double dblVal;
        BSTR   bstrVal;
        void*  punkVal;
        void*  pdispVal;
        LONG   lVal;
    };
} VARIANT, *LPVARIANT;

// DISPPARAMS
typedef struct tagDISPPARAMS {
    VARIANT* rgvarg;
    DISPID*  rgdispidNamedArgs;
    UINT     cArgs;
    UINT     cNamedArgs;
} DISPPARAMS;

// IUnknown interface
#ifndef __IUnknown_INTERFACE_DEFINED__
#define __IUnknown_INTERFACE_DEFINED__
struct IUnknown {
    virtual HRESULT QueryInterface(REFIID riid, void** ppv) = 0;
    virtual ULONG AddRef() = 0;
    virtual ULONG Release() = 0;
    virtual ~IUnknown() {}
};
#endif // __IUnknown_INTERFACE_DEFINED__

// IDispatch interface
struct IDispatch : public IUnknown {
    virtual HRESULT GetTypeInfoCount(UINT* pctinfo) = 0;
    virtual HRESULT GetTypeInfo(UINT itinfo, LCID lcid, void** pptinfo) = 0;
    virtual HRESULT GetIDsOfNames(REFIID riid, LPOLESTR* names, UINT n,
                                  LCID lcid, DISPID* ids) = 0;
    virtual HRESULT Invoke(DISPID dispid, REFIID riid, LCID lcid, WORD flags,
                           DISPPARAMS* params, VARIANT* result,
                           void* except_info, UINT* arg_err) = 0;
};

#define DISPATCH_METHOD      0x01
#define DISPATCH_PROPERTYGET 0x02
#define DISPATCH_PROPERTYPUT 0x04
#define DISPID_UNKNOWN       (-1)

// STDMETHODCALLTYPE, etc.
#ifndef STDMETHODCALLTYPE
#define STDMETHODCALLTYPE
#endif
#ifndef STDMETHODIMP
#define STDMETHODIMP HRESULT STDMETHODCALLTYPE
#endif
#ifndef STDMETHOD
#define STDMETHOD(method) virtual HRESULT STDMETHODCALLTYPE method
#endif
#ifndef STDMETHOD_
#define STDMETHOD_(rettype, method) virtual rettype STDMETHODCALLTYPE method
#endif

// VariantInit / VariantClear stubs
inline void VariantInit(VARIANT* pv) {
    if (pv) { pv->vt = VT_EMPTY; pv->lVal = 0; }
}
inline HRESULT VariantClear(VARIANT* pv) {
    if (pv) pv->vt = VT_EMPTY;
    return S_OK;
}
inline HRESULT VariantCopy(VARIANT* dst, const VARIANT* src) {
    if (dst && src) *dst = *src;
    return S_OK;
}

// SysAllocString / SysFreeString stubs
inline BSTR SysAllocString(const OLECHAR* str) { return NULL; }
inline void SysFreeString(BSTR bstr) {}

#endif // OAIDL_H_COMPAT
