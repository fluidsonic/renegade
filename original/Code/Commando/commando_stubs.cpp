// commando_stubs.cpp - Global definitions for removed/stubbed systems
// Provides definitions for globals that were in deleted or excluded source files

#include "always.h"
#include "natter.h"          // compat/natter.h
#include "gamespybanlist.h"
#include "GameSpy_QnR.h"
#include "stackdump.h"       // compat/stackdump.h
#include "verchk.h"          // compat/verchk.h

// BandTest types (was a Windows DLL for bandwidth detection)
enum {
    BANDTEST_OK, BANDTEST_NO_WINSOCK2, BANDTEST_NO_RAW_SOCKET_PERMISSION,
    BANDTEST_NO_RAW_SOCKET_CREATE, BANDTEST_NO_UDP_SOCKET_BIND,
    BANDTEST_NO_TTL_SET, BANDTEST_NO_PING_RESPONSE, BANDTEST_NO_FINAL_PING_TIME,
    BANDTEST_NO_EXTERNAL_ROUTER, BANDTEST_NO_IP_DETECT, BANDTEST_UNKNOWN_ERROR,
    BANDTEST_WRONG_API_VERSION, BANDTEST_BAD_PARAM,
};
typedef struct tBandtestSettingsStruct {
    unsigned int AlwaysICMP : 1; unsigned int TTLScatter : 1;
    unsigned int FastPingPackets : 7; unsigned int SlowPingPackets : 7;
    unsigned int FastPingThreshold : 5; unsigned int PingProfile : 1;
} BandtestSettingsStruct;
#define BANDTEST_API_VERSION 0x101

// WOL NAT traversal stub global (natter.cpp excluded from build - uses dead WWOnline service)
WOLNATInterfaceClass WOLNATInterface;

// GameSpy ban list global (GameSpyBanList.cpp deleted - GameSpy service removed)
GameSpyBanListClass GameSpyBanList;

// GameSpy QnR global (GameSpy_QnR.cpp never existed; was in h file)
cGameSpyQnR GameSpyQnR;

// Stack dump stub (stackdump.cpp excluded - uses Windows imagehlp.h)
void cStackDump::Print_Call_Stack(void)
{
    // Not available on macOS
}

// Bandwidth test stub (BandTest is a Windows DLL - replaced with stub)
unsigned long Detect_Bandwidth(unsigned long server_ip, unsigned long my_ip, int retries,
    int& failure_code, unsigned long& downstream, unsigned long api_version,
    BandtestSettingsStruct* settings, char* regpath)
{
    failure_code = BANDTEST_UNKNOWN_ERROR;
    downstream = 0;
    return 0;
}

// Version/PE inspection stubs (verchk.cpp excluded - uses Windows PE APIs)
bool GetVersionInfo(char* filename, VS_FIXEDFILEINFO* fileInfo)
{
    return false;
}

bool GetFileCreationTime(char* filename, FILETIME* createTime)
{
    if (createTime) { createTime->dwLowDateTime = 0; createTime->dwHighDateTime = 0; }
    return false;
}

bool Get_Image_File_Header(const char* filename, IMAGE_FILE_HEADER* file_header)
{
    return false;
}
