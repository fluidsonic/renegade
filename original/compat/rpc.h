// rpc.h compat stub for macOS - RPC is not available, stub for COM interface compilation
#pragma once
#ifndef RPC_H_COMPAT
#define RPC_H_COMPAT

// Required version check bypass
#define __REQUIRED_RPCNDR_H_VERSION__ 440
#define __RPCNDR_H_VERSION__          440

// Minimal COM-compatible types
#ifndef __RPCNDR_H__
#define __RPCNDR_H__
#include "rpcndr.h"
#endif

#endif // RPC_H_COMPAT
