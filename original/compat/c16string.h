// c16string.h — char16_t string functions for macOS/clang
// Provides equivalents of wcs* functions that operate on char16_t (2-byte UTF-16)
// instead of wchar_t (4-byte UTF-32 on macOS).
#pragma once
#ifndef C16STRING_H_COMPAT
#define C16STRING_H_COMPAT

#include <stddef.h>

// c16slen — equivalent of wcslen
inline size_t c16slen(const char16_t* s) {
    const char16_t* p = s;
    while (*p) p++;
    return (size_t)(p - s);
}

// c16scpy — equivalent of wcscpy
inline char16_t* c16scpy(char16_t* dst, const char16_t* src) {
    char16_t* d = dst;
    while ((*d++ = *src++) != 0) {}
    return dst;
}

// c16sncpy — equivalent of wcsncpy
inline char16_t* c16sncpy(char16_t* dst, const char16_t* src, size_t n) {
    char16_t* d = dst;
    while (n > 0 && *src) { *d++ = *src++; n--; }
    while (n > 0) { *d++ = 0; n--; }
    return dst;
}

// c16scat — equivalent of wcscat
inline char16_t* c16scat(char16_t* dst, const char16_t* src) {
    char16_t* d = dst;
    while (*d) d++;
    while ((*d++ = *src++) != 0) {}
    return dst;
}

// c16scmp — equivalent of wcscmp
inline int c16scmp(const char16_t* a, const char16_t* b) {
    while (*a && *a == *b) { a++; b++; }
    return (int)(unsigned short)*a - (int)(unsigned short)*b;
}

// c16sncmp — equivalent of wcsncmp
inline int c16sncmp(const char16_t* a, const char16_t* b, size_t n) {
    while (n > 0 && *a && *a == *b) { a++; b++; n--; }
    if (n == 0) return 0;
    return (int)(unsigned short)*a - (int)(unsigned short)*b;
}

// c16_tolower / c16_toupper — ASCII-range only (sufficient for game data)
inline char16_t c16_tolower(char16_t c) {
    if (c >= u'A' && c <= u'Z') return (char16_t)(c + 32);
    return c;
}
inline char16_t c16_toupper(char16_t c) {
    if (c >= u'a' && c <= u'z') return (char16_t)(c - 32);
    return c;
}

// c16sicmp — case-insensitive comparison (ASCII range), equivalent of _wcsicmp / wcscasecmp
inline int c16sicmp(const char16_t* a, const char16_t* b) {
    while (*a && c16_tolower(*a) == c16_tolower(*b)) { a++; b++; }
    return (int)c16_tolower(*a) - (int)c16_tolower(*b);
}

// c16snicmp — case-insensitive comparison with length limit, equivalent of _wcsnicmp
inline int c16snicmp(const char16_t* a, const char16_t* b, size_t n) {
    while (n > 0 && *a && c16_tolower(*a) == c16_tolower(*b)) { a++; b++; n--; }
    if (n == 0) return 0;
    return (int)c16_tolower(*a) - (int)c16_tolower(*b);
}

// c16schr — equivalent of wcschr
inline const char16_t* c16schr(const char16_t* s, char16_t c) {
    while (*s) { if (*s == c) return s; s++; }
    return (c == 0) ? s : nullptr;
}
inline char16_t* c16schr(char16_t* s, char16_t c) {
    while (*s) { if (*s == c) return s; s++; }
    return (c == 0) ? s : nullptr;
}

// c16srchr — equivalent of wcsrchr
inline const char16_t* c16srchr(const char16_t* s, char16_t c) {
    const char16_t* last = nullptr;
    while (*s) { if (*s == c) last = s; s++; }
    if (c == 0) return s;
    return last;
}
inline char16_t* c16srchr(char16_t* s, char16_t c) {
    char16_t* last = nullptr;
    while (*s) { if (*s == c) last = s; s++; }
    if (c == 0) return s;
    return last;
}

// c16sstr — equivalent of wcsstr
inline const char16_t* c16sstr(const char16_t* haystack, const char16_t* needle) {
    if (!*needle) return haystack;
    for (; *haystack; haystack++) {
        const char16_t* h = haystack;
        const char16_t* n = needle;
        while (*h && *n && *h == *n) { h++; n++; }
        if (!*n) return haystack;
    }
    return nullptr;
}
inline char16_t* c16sstr(char16_t* haystack, const char16_t* needle) {
    return const_cast<char16_t*>(c16sstr((const char16_t*)haystack, needle));
}

// c16strim — trim leading and trailing whitespace in-place (equivalent of wcstrim)
inline char16_t* c16strim(char16_t* s) {
    if (!s) return s;
    size_t len = c16slen(s);
    while (len > 0 && s[len-1] <= 32) len--;
    s[len] = 0;
    size_t start = 0;
    while (s[start] && s[start] <= 32) start++;
    if (start > 0) {
        size_t i;
        for (i = 0; s[start + i]; i++) s[i] = s[start + i];
        s[i] = 0;
    }
    return s;
}

// c16supr — convert to uppercase in-place (ASCII range), equivalent of _wcsupr
inline char16_t* c16supr(char16_t* s) {
    char16_t* p = s;
    while (*p) { *p = c16_toupper(*p); p++; }
    return s;
}

// c16slwr — convert to lowercase in-place (ASCII range), equivalent of _wcslwr
inline char16_t* c16slwr(char16_t* s) {
    char16_t* p = s;
    while (*p) { *p = c16_tolower(*p); p++; }
    return s;
}

// c16spbrk — equivalent of wcspbrk (find first char from accept set)
inline const char16_t* c16spbrk(const char16_t* s, const char16_t* accept) {
    for (; *s; s++) {
        for (const char16_t* a = accept; *a; a++) {
            if (*s == *a) return s;
        }
    }
    return nullptr;
}
inline char16_t* c16spbrk(char16_t* s, const char16_t* accept) {
    return const_cast<char16_t*>(c16spbrk((const char16_t*)s, accept));
}

// c16stoul — equivalent of wcstoul for ASCII digit strings
inline unsigned long c16stoul(const char16_t* s, char16_t** endptr, int base) {
    while (*s == u' ' || *s == u'\t') s++;
    if (base == 0) {
        if (s[0] == u'0' && (s[1] == u'x' || s[1] == u'X')) { base = 16; s += 2; }
        else { base = 10; }
    } else if (base == 16 && s[0] == u'0' && (s[1] == u'x' || s[1] == u'X')) {
        s += 2;
    }
    unsigned long result = 0;
    const char16_t* p = s;
    while (*p) {
        int digit = -1;
        if (*p >= u'0' && *p <= u'9') digit = *p - u'0';
        else if (base == 16 && *p >= u'a' && *p <= u'f') digit = *p - u'a' + 10;
        else if (base == 16 && *p >= u'A' && *p <= u'F') digit = *p - u'A' + 10;
        if (digit < 0 || digit >= base) break;
        result = result * (unsigned long)base + (unsigned long)digit;
        p++;
    }
    if (endptr) *endptr = (char16_t*)p;
    return result;
}

// c16stol — equivalent of wcstol for ASCII digit strings
inline long c16stol(const char16_t* s, char16_t** endptr, int base) {
    while (*s == u' ' || *s == u'\t') s++;
    int sign = 1;
    if (*s == u'-') { sign = -1; s++; }
    else if (*s == u'+') { s++; }
    return sign * (long)c16stoul(s, endptr, base);
}

#endif // C16STRING_H_COMPAT
