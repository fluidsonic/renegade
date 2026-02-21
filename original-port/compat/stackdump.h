#pragma once
#ifndef __STACKDUMP_H__
#define __STACKDUMP_H__

#define LOG_CALL_STACK cStackDump::Print_Call_Stack()

class cStackDump {
public:
    static void Print_Call_Stack(void);
};

#endif // __STACKDUMP_H__
