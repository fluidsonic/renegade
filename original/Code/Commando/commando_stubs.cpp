// commando_stubs.cpp - Global definitions for removed/stubbed systems
// Provides definitions for globals that were in deleted or excluded source files

#include "always.h"
#include "natter.h"          // compat/natter.h
#include "gamespybanlist.h"
#include "GameSpy_QnR.h"
#include "stackdump.h"       // compat/stackdump.h
#include "verchk.h"          // compat/verchk.h
#include "BandTest/BandTest.h"

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
