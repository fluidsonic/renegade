// snmp.h compat stub for macOS - SNMP is not used, stub for compilation
#pragma once
#ifndef SNMP_H_COMPAT
#define SNMP_H_COMPAT

#include "windef.h"

// SAL annotations — no-ops on macOS
#ifndef IN
#define IN
#endif
#ifndef OUT
#define OUT
#endif
#ifndef OPTIONAL
#define OPTIONAL
#endif

// SNMP basic types (from winsnmp.h / snmp.h on Windows)
typedef unsigned char  AsnOctetString;
typedef long           AsnInteger32;
typedef unsigned long  AsnUnsignedInteger32;
typedef long           AsnInteger;  // same as AsnInteger32 (long on both platforms)

// AsnObjectIdentifier: OID structure
typedef struct {
    unsigned int  idLength;
    unsigned int* ids;
} AsnObjectIdentifier;

// AsnAny: holds any ASN.1 value (simplified stub)
typedef struct {
    unsigned char asnType;
    union {
        long          number;      // integer
        unsigned long uNumber;     // unsigned integer
        AsnOctetString octetString;
        AsnObjectIdentifier object;
        struct {                   // network address (tcpConnLocalAddress etc.)
            unsigned char stream[4];
            unsigned int  length;
        } address;
    } asnValue;
} AsnAny;

// RFC 1157 variable binding
typedef struct {
    AsnObjectIdentifier  name;
    AsnAny               value;
} RFC1157VarBind;

typedef struct {
    RFC1157VarBind* list;
    unsigned int    len;
} RFC1157VarBindList;

// WinSNMP SnmpVarBind (alias for RFC1157)
typedef RFC1157VarBind SnmpVarBind;
typedef struct {
    SnmpVarBind* list;
    unsigned int  len;
} SnmpVarBindList;

// SNMP PDU type constants (from snmp.h)
#define ASN_RFC1157_GETNEXTREQUEST  0xA1
#define ASN_RFC1157_GETREQUEST      0xA0
#define ASN_RFC1157_SETREQUEST      0xA3
#define ASN_RFC1157_GETRESPONSE     0xA2

// MIB-II TCP connection state constants are defined locally in NAT.cpp via enum,
// so we do NOT define them here as macros (would conflict with case labels).

#endif // SNMP_H_COMPAT
