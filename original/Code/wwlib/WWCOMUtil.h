#pragma once

#include "global.h"

#include <oaidl.h>

//! Invoke PropertyGet on IDispatch interface.
HRESULT STDMETHODCALLTYPE Dispatch_GetProperty(IDispatch* object,
		const OLECHAR* propName, VARIANT* result);

//! Invoke PropertyPut on IDispatch interface.
HRESULT STDMETHODCALLTYPE Dispatch_PutProperty(IDispatch* object,
		const OLECHAR* propName, VARIANT* propValue);

//! Invoke Method on IDispatch interface.
HRESULT STDMETHODCALLTYPE Dispatch_InvokeMethod(IDispatch* object,
		const OLECHAR* methodName, DISPPARAMS* params, VARIANT* result);

//! Register COM in-process DLL server
bool RegisterCOMServer(const char* dllName);

//! Unregister COM in-process DLL server
bool UnregisterCOMServer(const char* dllName);
