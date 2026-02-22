#include "global.h"
#include "thread.h"

ThreadClass::ThreadClass(const char *thread_name, ExceptionHandlerType exception_handler) : handle(0), running(false), thread_priority(0)
{
	if (thread_name) {
		assert(strlen(thread_name) < sizeof(ThreadName) - 1);
		strcpy(ThreadName, thread_name);
	} else {
		strcpy(ThreadName, "No name");
	}

	ExceptionHandler = exception_handler;
}

ThreadClass::~ThreadClass()
{
	Stop();
}

void* ThreadClass::Internal_Thread_Function(void* params)
{
	ThreadClass* tc = static_cast<ThreadClass*>(params);
	tc->running = true;
	tc->ThreadID = (unsigned)(uintptr_t)pthread_self();
	tc->Thread_Function();
	tc->handle = 0;
	tc->ThreadID = 0;
	return nullptr;
}

void ThreadClass::Execute()
{
	pthread_t tid;
	if (pthread_create(&tid, nullptr, &Internal_Thread_Function, this) == 0) {
		handle = (unsigned long)(uintptr_t)tid;
		pthread_detach(tid);
	}
}

void ThreadClass::Set_Priority(int priority)
{
	thread_priority = priority;
	// pthread priority adjustment requires SCHED_RR/FIFO and root; skip for now.
}

void ThreadClass::Stop(unsigned ms)
{
	running = false;
	unsigned elapsed = 0;
	while (handle && elapsed < ms) {
		usleep(1000);
		elapsed++;
	}
	if (handle) {
		pthread_cancel((pthread_t)(uintptr_t)handle);
		handle = 0;
	}
}

void ThreadClass::Sleep_Ms(unsigned ms)
{
	usleep(ms * 1000);
}

void ThreadClass::Switch_Thread()
{
	sched_yield();
}

unsigned ThreadClass::_Get_Current_Thread_ID()
{
	return (unsigned)(uintptr_t)pthread_self();
}

bool ThreadClass::Is_Running()
{
	return !!handle;
}
