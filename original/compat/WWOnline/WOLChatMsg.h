// WWOnline/WOLChatMsg.h - Westwood Online stub
#pragma once
#ifndef WWONLINE_WOLCHATMSG_H
#define WWONLINE_WOLCHATMSG_H
#include "wwonline_types.h"

namespace WWOnline {
// IsAnsiText - returns true if all characters are in the ASCII/Latin-1 range
inline bool IsAnsiText(const wchar_t* text) {
    if (!text) return true;
    while (*text) {
        if (*text > 0x00FF) return false;
        ++text;
    }
    return true;
}
} // namespace WWOnline

#endif // WWONLINE_WOLCHATMSG_H
