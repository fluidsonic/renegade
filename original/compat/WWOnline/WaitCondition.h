// WWOnline/WaitCondition.h - stub for Westwood Online wait condition
#pragma once
#ifndef WWONLINE_WAITCONDITION_H
#define WWONLINE_WAITCONDITION_H

#include "RefPtr.h"

class WaitCondition {
public:
    enum WaitResult {
        Waiting = 0,
        ConditionMet,
        UserCancel,
        TimeOut,
        Error
    };

    WaitCondition() {}
    virtual ~WaitCondition() {}

    virtual WaitResult GetResult(void) const { return Waiting; }
    virtual void EndWait(WaitResult result, const wchar_t* msg = nullptr) {}
    virtual void WaitBeginning(void) {}
    virtual void Think(void) {}
    const wchar_t* GetResultText(void) const { return L""; }
    const wchar_t* GetWaitText(void) const { return L""; }
    unsigned long GetTimeout(void) const { return 0; }

    // Reference counting stubs
    void AddRef() {}
    void Release() {}
};

// SingleWait - base class for single-step wait operations (from WWOnline/WaitCondition.h)
class SingleWait : public WaitCondition {
public:
    typedef WaitCondition::WaitResult WaitResult;

    // Constructors: (waitText, timeoutMs)
    SingleWait() : mTimeout(0), mEndResult(WaitCondition::Waiting), mEndText(nullptr) {}
    SingleWait(const wchar_t* text, unsigned long timeout = 0)
        : mTimeout(timeout), mEndResult(WaitCondition::Waiting), mEndText(nullptr) {}

    virtual ~SingleWait() {}

    // Factory - returns a RefPtr<SingleWait>
    static RefPtr<SingleWait> Create(const wchar_t* text, unsigned long timeout = 0) {
        return RefPtr<SingleWait>();
    }

    virtual WaitResult GetResult(void) const { return mEndResult; }
    virtual void EndWait(WaitResult result, const wchar_t* msg = nullptr) {
        mEndResult = result;
        mEndText = msg;
    }
    virtual void WaitBeginning(void) {}

    unsigned long GetTimeout(void) const { return mTimeout; }

    template<typename T>
    void SetWaitText(const T& text) {}

protected:
    unsigned long     mTimeout;
    WaitResult        mEndResult;
    const wchar_t*    mEndText;
};

// SerialWait - sequences multiple wait conditions
class SerialWait : public WaitCondition {
public:
    static RefPtr<SerialWait> Create(void) { return RefPtr<SerialWait>(); }
    void Add(const RefPtr<WaitCondition>&) {}
    int  RemainingWaits(void) const { return 0; }
    virtual void WaitBeginning(void) {}
    virtual WaitResult GetResult(void) const { return WaitCondition::ConditionMet; }
    const wchar_t* GetResultText(void) const { return L""; }
    void GetWaitText(void*) {}
};

// PingProfileWait
class PingProfileWait : public WaitCondition {
public:
    static RefPtr<PingProfileWait> Create(void) { return RefPtr<PingProfileWait>(); }
    static RefPtr<WaitCondition>   Start(void)  { return RefPtr<WaitCondition>(); }
};

// GetUserWait - waits until a specific user appears in the user list
class GetUserWait : public WaitCondition {
public:
    // Create(session, userName) - waits for user to appear
    template<typename SessionT>
    static RefPtr<GetUserWait> Create(const SessionT& session, const wchar_t* userName) {
        return RefPtr<GetUserWait>();
    }
};

// SingleWait::Create factory
inline RefPtr<SingleWait> SingleWait_Create(const wchar_t* text, unsigned long timeout = 0) {
    return RefPtr<SingleWait>();
}

#endif // WWONLINE_WAITCONDITION_H
