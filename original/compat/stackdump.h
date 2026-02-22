#pragma once

#include "global.h"

#define LOG_CALL_STACK cStackDump::Print_Call_Stack()

class cStackDump {
public:
    static void Print_Call_Stack(void);
};
