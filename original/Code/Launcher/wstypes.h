#ifndef WTYPES_HEADER
#define WTYPES_HEADER

#ifndef TRUE
#define TRUE               1
#endif

#ifndef FALSE
#define FALSE              0
#endif

#ifndef MIN
#define MIN(x,y) (((x)<(y))?(x):(y))
#endif

#ifndef MAX
#define MAX(x,y) (((x)>(y))?(x):(y))
#endif

#ifndef NULL
#define NULL 0
#endif

//These are used for readability purposes mostly, when a method takes a
//  pointer or reference these help specify what will happen to the data
//  that is sent in.
#define IN
#define OUT
#define INOUT

typedef char               bit8;
typedef char               int8_t;
typedef unsigned char      uint8_t;
typedef signed short int   int16_t;
typedef unsigned short int uint16_t;
typedef signed int         int32_t;
typedef unsigned int       uint32_t;

#define MAX_BIT8    0x1
#define MAX_UINT32  0xFFFFFFFF
#define MAX_UINT16  0xFFFF
#define MAX_UINT8   0xFF
#define MAX_SINT32  0x7FFFFFFF
#define MAX_SINT16  0x7FFF
#define MAX_SINT8   0x7F   


#endif
