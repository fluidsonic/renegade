// GameSpy/gcdkeyserver.h - stub
#pragma once
#ifndef GAMESPY_GCDKEYSERVER_H
#define GAMESPY_GCDKEYSERVER_H
#include "nonport.h"
typedef void* GCDKEYSData;
inline void gcd_server_init(int gameid) {}
inline void gcd_server_shutdown(void) {}
inline void gcd_server_think(GCDKEYSData data) {}
// Called with (qr_t, cdkey_id)
inline void gcd_init_qr(void* qrec, int cdkey_id) {}
inline void gcd_think(void) {}
#endif
