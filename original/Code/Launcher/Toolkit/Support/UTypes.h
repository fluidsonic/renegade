#ifndef UTYPES_H
#define UTYPES_H

//! Signed integer value
typedef int Int;

//! Unsigned integer value
typedef unsigned int UInt;

//! Signed 8bit value (-127 - 128)
typedef char Int8;

//! Unsigned 8bit value (0 - 255)
typedef unsigned char UInt8;

//! Signed 16bit value (-32767 - 32768)
typedef short Int16;

//! Unsigned 16bit value (0 - 65535)
typedef unsigned short UInt16;

//! Signed 32bit value
typedef int32_t Int32;

//! Unsigned 32bit value
typedef uint32_t UInt32;

//! Signed character (ASCII)
typedef char Char;

//! Unsigned character (ANSI)
typedef unsigned char UChar;

//! Wide character (Unicode)
typedef char16_t WChar;

//! 32bit floating point value
typedef float32_t Float32;

//! 64bit floating point value
typedef float64_t Float64;

//! Floating point value
typedef float32_t Float;

//! TriState
typedef enum {OFF = false, ON = true, PENDING = -1} TriState;

//! Empty pointer
#ifndef NULL
#define NULL (0L)
#endif

#endif // UTYPES_H
