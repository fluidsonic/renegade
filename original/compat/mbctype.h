#pragma once

#include "global.h"

// mbctype.h compat shim for macOS - Multi-byte char type stubs

#include <ctype.h>
#include <string.h>

// MBCS code page constants
#define _UNKNOWN_CODEPAGE   0
#define _OEM_UNITED_STATES  437
#define _ANSI_UNITED_STATES 1252

// Multi-byte character type values
#define _MS        0x01  // single-byte katakana
#define _MP        0x02  // two-byte character prefix (lead byte)
#define _M1        0x04  // two-byte character suffix (trail byte 1)
#define _M2        0x08  // two-byte character suffix (trail byte 2)
#define _SBUP      0x10  // single-byte uppercase character
#define _SBLOW     0x20  // single-byte lowercase character
#define _MBC_SINGLE     0
#define _MBC_LEAD       1
#define _MBC_TRAIL      2
#define _MBC_ILLEGAL    (-1)

// MBCS functions - all stub to ANSI on macOS
inline int _ismbblead(unsigned int c) { return 0; }
inline int _ismbbtrail(unsigned int c) { return 0; }
inline int _ismbblead_l(unsigned int c, void*) { return 0; }
inline int _ismbslead(const unsigned char* str, const unsigned char* cur) { return 0; }
inline int _ismbstrail(const unsigned char* str, const unsigned char* cur) { return 0; }
inline int _ismbcalnum(unsigned int c) { return isalnum((int)(c & 0xFF)); }
inline int _ismbcalpha(unsigned int c) { return isalpha((int)(c & 0xFF)); }
inline int _ismbcdigit(unsigned int c) { return isdigit((int)(c & 0xFF)); }
inline int _ismbcspace(unsigned int c) { return isspace((int)(c & 0xFF)); }
inline int _ismbclower(unsigned int c) { return islower((int)(c & 0xFF)); }
inline int _ismbcupper(unsigned int c) { return isupper((int)(c & 0xFF)); }
inline int _ismbcprint(unsigned int c) { return isprint((int)(c & 0xFF)); }
inline int _ismbcpunct(unsigned int c) { return ispunct((int)(c & 0xFF)); }
inline int _getmbcp() { return 0; }
inline int _setmbcp(int cp) { return 0; }

// MBCS string functions (treating all chars as single-byte on macOS)
inline size_t _mbslen(const unsigned char* str) {
    return str ? strlen((const char*)str) : 0;
}
inline unsigned char* _mbsinc(const unsigned char* current) {
    return (unsigned char*)current + 1;
}
inline size_t _mbsnccnt(const unsigned char* str, size_t cnt) {
    // Returns number of chars in first cnt bytes (single-byte: same as cnt)
    if (!str) return 0;
    size_t len = strlen((const char*)str);
    return cnt < len ? cnt : len;
}
inline unsigned char* _mbsdec(const unsigned char* start, const unsigned char* current) {
    if (current > start) return (unsigned char*)current - 1;
    return (unsigned char*)current;
}
inline int _mbsbtype(const unsigned char* str, size_t cnt) { return _MBC_SINGLE; }
inline unsigned char* _mbsninc(const unsigned char* str, size_t cnt) {
    if (!str) return NULL;
    for (size_t i = 0; i < cnt && *str; i++, str++) {}
    return (unsigned char*)str;
}
inline size_t _mbsnbcnt(const unsigned char* str, size_t cnt) {
    if (!str) return 0;
    size_t len = strlen((const char*)str);
    return cnt < len ? cnt : len;
}

// IsDBCSLeadByte - always false on macOS (no DBCS support)
inline BOOL IsDBCSLeadByte(BYTE TestChar) { return FALSE; }
inline BOOL IsDBCSLeadByteEx(UINT CodePage, BYTE TestChar) { return FALSE; }
