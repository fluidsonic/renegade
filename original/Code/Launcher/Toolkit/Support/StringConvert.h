#pragma once

#include "global.h"

#include "UTypes.h"

class UString;

Char* UStringToANSI(const UString& string, Char* buffer, UInt bufferLength);
Char* UnicodeToANSI(const WChar* string, Char* buffer, UInt bufferLength);
