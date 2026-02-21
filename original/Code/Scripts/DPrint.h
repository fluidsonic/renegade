#ifndef _DPRINT_H_
#define _DPRINT_H_

#ifdef __cplusplus
extern "C"
{
#endif

#ifdef _DEBUG
//! Ouput debug print messages to the debugger and log file.
void __cdecl DebugPrint(const char* string, ...);
#else
#define DebugPrint
#endif


#ifdef __cplusplus
}
#endif

#endif // _DPRINT_H_
