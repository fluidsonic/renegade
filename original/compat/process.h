#pragma once

#include "global.h"

// process.h compat shim for macOS

#include <pthread.h>
#include <stdlib.h>
#include <unistd.h>

// _beginthread wrapper using pthreads
typedef void (*_beginthread_fn)(void*);

struct _ThreadData { _beginthread_fn fn; void* arg; };
inline void* _thread_run(void* p) {
    struct _ThreadData* td = (struct _ThreadData*)p;
    td->fn(td->arg);
    free(td);
    return NULL;
}

inline uintptr_t _beginthread(_beginthread_fn fn, unsigned stack_size, void* arg) {
    pthread_t* t = (pthread_t*)malloc(sizeof(pthread_t));
    struct _ThreadData* td = (struct _ThreadData*)malloc(sizeof(struct _ThreadData));
    td->fn = fn; td->arg = arg;
    if (pthread_create(t, NULL, _thread_run, td) != 0) { free(td); free(t); return (uintptr_t)-1; }
    // Do NOT detach — caller joins via WaitForSingleObject then CloseHandle (which free()s this ptr)
    return (uintptr_t)t;
}

inline uintptr_t _beginthreadex(void* sec, unsigned stack_size, unsigned (*fn)(void*), void* arg, unsigned init_flag, unsigned* thrdaddr) {
    return _beginthread((_beginthread_fn)fn, stack_size, arg);
}

inline void _endthread() { pthread_exit(NULL); }
inline void _endthreadex(unsigned retval) { pthread_exit((void*)(uintptr_t)retval); }

// getpid
#define _getpid getpid
