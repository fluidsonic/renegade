
#ifndef BITTYPE_H
#define BITTYPE_H

typedef unsigned char	uint8;
typedef unsigned short	uint16;
// On Win32, 'unsigned long' is 4 bytes. On 64-bit macOS, it is 8 bytes.
// Use 'unsigned int' for true 32-bit width on all platforms.
#if defined(__clang__) && !defined(_MSC_VER)
typedef unsigned int	uint32;
typedef signed int		sint32;
#else
typedef unsigned long	uint32;
typedef signed long		sint32;
#endif
typedef unsigned int    uint;

typedef signed char		sint8;
typedef signed short		sint16;
typedef signed int      sint;

typedef float				float32;
typedef double				float64;

// DWORD and ULONG must be 32-bit on all platforms
#if defined(__clang__) && !defined(_MSC_VER)
typedef unsigned int    DWORD;
typedef unsigned int    ULONG;
#else
typedef unsigned long   DWORD;
typedef unsigned long   ULONG;
#endif
typedef unsigned short	WORD;
typedef unsigned char   BYTE;
typedef int             BOOL;
typedef unsigned short	USHORT;
typedef const char *		LPCSTR;
typedef unsigned int    UINT;

#endif //BITTYPE_H
