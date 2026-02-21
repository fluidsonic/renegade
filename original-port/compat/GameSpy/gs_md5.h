// GameSpy/gs_md5.h - stub
#pragma once
#ifndef GAMESPY_GS_MD5_H
#define GAMESPY_GS_MD5_H
#include "nonport.h"
typedef struct { unsigned char digest[16]; } MD5_CTX;
inline void MD5Init(MD5_CTX* ctx) {}
inline void MD5Update(MD5_CTX* ctx, const unsigned char* buf, unsigned int len) {}
inline void MD5Final(unsigned char* digest, MD5_CTX* ctx) { memset(digest, 0, 16); }
// MD5Digest(data, len, hex_out) - stub: produce zeroed hex string
inline void MD5Digest(const unsigned char* data, unsigned int len, char* hex_out) {
    if (hex_out) { for(int i=0;i<32;i++) hex_out[i]='0'; hex_out[32]=0; }
}
#endif
