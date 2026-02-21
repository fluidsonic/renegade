// BandTest/BandTest.h - compat stub (BandTest was a Windows DLL for bandwidth detection)
#pragma once

enum {
    BANDTEST_OK,
    BANDTEST_NO_WINSOCK2,
    BANDTEST_NO_RAW_SOCKET_PERMISSION,
    BANDTEST_NO_RAW_SOCKET_CREATE,
    BANDTEST_NO_UDP_SOCKET_BIND,
    BANDTEST_NO_TTL_SET,
    BANDTEST_NO_PING_RESPONSE,
    BANDTEST_NO_FINAL_PING_TIME,
    BANDTEST_NO_EXTERNAL_ROUTER,
    BANDTEST_NO_IP_DETECT,
    BANDTEST_UNKNOWN_ERROR,
    BANDTEST_WRONG_API_VERSION,
    BANDTEST_BAD_PARAM,
};

typedef struct tBandtestSettingsStruct {
    unsigned int AlwaysICMP      : 1;
    unsigned int TTLScatter      : 1;
    unsigned int FastPingPackets : 7;
    unsigned int SlowPingPackets : 7;
    unsigned int FastPingThreshold : 5;
    unsigned int PingProfile     : 1;
} BandtestSettingsStruct;

#define BANDTEST_API_VERSION 0x101

unsigned long Detect_Bandwidth(unsigned long server_ip, unsigned long my_ip, int retries,
    int& failure_code, unsigned long& downstream, unsigned long api_version,
    BandtestSettingsStruct* settings, char* regpath = NULL);
