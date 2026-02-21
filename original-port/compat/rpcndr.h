// rpcndr.h compat stub for macOS - NDR (Network Data Representation) stubs
#pragma once
#ifndef RPCNDR_H_COMPAT
#define RPCNDR_H_COMPAT

#include "windef.h"

// COM/RPC pointer and calling convention modifiers (no-ops on macOS)
#ifndef __RPC_FAR
#define __RPC_FAR
#endif
#ifndef __RPC_USER
#define __RPC_USER
#endif
#ifndef __RPC_STUB
#define __RPC_STUB
#endif
#ifndef __RPC_API
#define __RPC_API
#endif

// EXTERN_C: extern "C" in C++, extern in C
#ifndef EXTERN_C
#ifdef __cplusplus
#define EXTERN_C extern "C"
#else
#define EXTERN_C extern
#endif
#endif

// MIDL_INTERFACE: MIDL-generated interface declaration (maps to struct)
#ifndef MIDL_INTERFACE
#define MIDL_INTERFACE(uuid) struct
#endif

// interface keyword (COM interface = pure virtual struct)
#ifndef interface
#define interface struct
#endif

// COM basic types
typedef unsigned char byte;
typedef unsigned short USHORT;

// NDR types
typedef unsigned char  BYTE;
typedef void*          PRPC_MESSAGE;

// COM error codes
#define S_OK        ((HRESULT)0L)
#define S_FALSE     ((HRESULT)1L)
#define E_NOTIMPL   ((HRESULT)0x80004001L)
#define E_NOINTERFACE ((HRESULT)0x80004002L)
#define E_POINTER   ((HRESULT)0x80004003L)
#define E_FAIL      ((HRESULT)0x80004005L)
#define E_UNEXPECTED ((HRESULT)0x8000FFFFL)

// IUnknown and IDispatch defined in oaidl.h; don't redefine here

// COM interface macros
#ifndef BEGIN_INTERFACE
#define BEGIN_INTERFACE
#define END_INTERFACE
#endif

// RPC interface handle type
typedef void* RPC_IF_HANDLE;

// DECLSPEC_UUID - MSVC attribute for COM GUIDs (no-op on clang)
#ifndef DECLSPEC_UUID
#define DECLSPEC_UUID(uuid)
#endif

// Forward-declare COM proxy/stub interfaces (defined fully in objidl.h on Windows)
struct IUnknown;
struct IRpcStubBuffer;
struct IRpcChannelBuffer;

#endif // RPCNDR_H_COMPAT
