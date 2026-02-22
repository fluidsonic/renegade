#pragma once

#include "global.h"

#include <windows.h>

// Stub declarations - implementations in commando_stubs.cpp
bool GetVersionInfo(char* filename, VS_FIXEDFILEINFO* fileInfo);
bool GetFileCreationTime(char* filename, FILETIME* createTime);
int  Compare_EXE_Version(int app_instance, const char* filename);
