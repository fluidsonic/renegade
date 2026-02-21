// WWOnline/RefPtr.h - stub for Westwood Online reference pointer
#pragma once
#ifndef WWONLINE_REFPTR_H
#define WWONLINE_REFPTR_H

#include <cstddef>

// Minimal reference-counted pointer stub
template<typename T>
class RefPtr {
public:
    RefPtr() : mPtr(nullptr) {}
    RefPtr(T* p) : mPtr(p) {}
    RefPtr(const RefPtr& o) : mPtr(o.mPtr) {}
    // Upcasting constructor: allows RefPtr<Derived> → RefPtr<Base>
    template<typename U>
    RefPtr(const RefPtr<U>& o) : mPtr(o.Get()) {}
    RefPtr& operator=(const RefPtr& o) { mPtr = o.mPtr; return *this; }
    RefPtr& operator=(T* p) { mPtr = p; return *this; }
    T* operator->() const { return mPtr; }
    T& operator*() const { return *mPtr; }
    T* Get() const { return mPtr; }
    bool operator==(const RefPtr& o) const { return mPtr == o.mPtr; }
    bool operator!=(const RefPtr& o) const { return mPtr != o.mPtr; }
    bool operator==(std::nullptr_t) const { return mPtr == nullptr; }
    bool operator!=(std::nullptr_t) const { return mPtr != nullptr; }
    operator bool() const { return mPtr != nullptr; }
    bool IsValid() const { return mPtr != nullptr; }
    void Release() { mPtr = nullptr; }
    T* ReferencedObject() const { return mPtr; }
private:
    T* mPtr;
};

#endif // WWONLINE_REFPTR_H
