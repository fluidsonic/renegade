#ifndef WDEBUG_HEADER
#define WDEBUG_HEADER

#include <iostream.h>
#include "sem4.h"
#include "odevice.h"
#include "streamer.h"

// This is needed because the streams return a pointer.  Every time you
//  change the output device the old stream is deleted, and a new one
//  is created.
extern Sem4 DebugLibSemaphore; 

// Print an information message
#define INFMSG(X)\
{\
  char     timebuf[40]; \
  time_t   clock=time(NULL); \
  cftime(timebuf,"%D %T",&clock); \
  DebugLibSemaphore.Wait(); \
  if (MsgManager::infoStream()) \
    (*(MsgManager::infoStream())) << "INF " << timebuf << " [" << \
        __FILE__ <<  " " << __LINE__ << "] " << X << endl; \
  DebugLibSemaphore.Post(); \
}

// Print a warning message
#define WRNMSG(X)\
{\
  char     timebuf[40]; \
  time_t   clock=time(NULL); \
  cftime(timebuf,"%D %T",&clock); \
  DebugLibSemaphore.Wait(); \
  if (MsgManager::warnStream()) \
    (*(MsgManager::warnStream())) << "WRN " << timebuf << " [" << \
        __FILE__ <<  " " << __LINE__ << "] " << X << endl; \
  DebugLibSemaphore.Post(); \
}

// Print an error message
#define ERRMSG(X)\
{\
  char     timebuf[40]; \
  time_t   clock=time(NULL); \
  cftime(timebuf,"%D %T",&clock); \
  DebugLibSemaphore.Wait(); \
  if (MsgManager::errorStream()) \
    (*(MsgManager::errorStream())) << "ERR " << timebuf << " [" << \
        __FILE__ <<  " " << __LINE__ << "] " << X << endl; \
  DebugLibSemaphore.Post(); \
}

// Just get a stream to the information device, no extra junk
#define INFSTREAM(X)\
{\
  DebugLibSemaphore.Wait(); \
  if (MsgManager::infoStream()) \
    (*(MsgManager::infoStream())) << X;\
  DebugLibSemaphore.Post(); \
}    

// Just get a stream to the warning device, no extra junk
#define WRNSTREAM(X)\
{\
  DebugLibSemaphore.Wait(); \
  if (MsgManager::warnStream()) \
    (*(MsgManager::warnStream())) << X;\
  DebugLibSemaphore.Post(); \
}    

// Just get a stream to the error device, no extra junk
#define ERRSTREAM(X)\
{\
  DebugLibSemaphore.Wait(); \
  if (MsgManager::errorStream()) \
    (*(MsgManager::errorStream())) << X;\
  DebugLibSemaphore.Post(); \
}    

#ifndef DEBUG

// No debugging, no debug messages.
// Note that anything enclosed in "DBG()" will NOT get executed
// unless DEBUG is defined.
// They are defined to {} for consistency when DEBUG is defined

#define DBG(X)
#define DBGSTREAM(X)  {}
#define PVAR(v)       {}
#define DBGMSG(X)     {}
#define VERBOSE(X)    {}

#else  // DEBUG _is_ defined

// Execute only if in debugging mode
#define DBG(X) X

// Print a variable
#define PVAR(v) \
{ \
  DebugLibSemaphore.Wait(); \
  if (MsgManager::debugStream()) \
    (*(MsgManager::debugStream())) << __FILE__ << "[" << __LINE__ << \
       "]: " << ##V << " = " << V << endl; \
  DebugLibSemaphore.Post(); \
}

#define DBGMSG(X)\
{\
  DebugLibSemaphore.Wait(); \
  if (MsgManager::debugStream()) \
    (*(MsgManager::debugStream())) << "DBG [" << __FILE__ <<  \
    " " << __LINE__ << "] " << X << endl;\
  DebugLibSemaphore.Post(); \
}

// Just get a stream to the debugging device, no extra junk
#define DBGSTREAM(X)\
{\
  DebugLibSemaphore.Wait(); \
  if (MsgManager::debugStream()) \
    (*(MsgManager::debugStream())) << X;\
  DebugLibSemaphore.Post(); \
}    

// Verbosely execute a statement
#define VERBOSE(X)\
{ \
  DebugLibSemaphore.Wait(); \
  if (MsgManager::debugStream()) \
    (*(DebugManager::debugStream())) << __FILE__ << "[" << __LINE__ << \
     "]: " << ##X << endl; X \
  DebugLibSemaphore.Post(); \
}

#endif  // DEBUG

class MsgManager
{
 protected:
   MsgManager();

 public:
   static int                 setAllStreams(OutputDevice *device);
   static int                 setDebugStream(OutputDevice *device);
   static int                 setInfoStream(OutputDevice *device);
   static int                 setWarnStream(OutputDevice *device);
   static int                 setErrorStream(OutputDevice *device);

   static void                enableDebug(int flag);
   static void                enableInfo(int flag);
   static void                enableWarn(int flag);
   static void                enableError(int flag);

   static ostream            *debugStream(void);
   static ostream            *infoStream(void);
   static ostream            *warnStream(void);
   static ostream            *errorStream(void);
};

#endif
