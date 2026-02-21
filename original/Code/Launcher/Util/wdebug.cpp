#include <stdlib.h>
#include "wdebug.h"
#include "streamer.h"
#include "odevice.h"


static MsgManager         *msg_manager=NULL;

static int                debug_enabled=0;
static ostream           *debug_ostream=NULL;
static Streamer           debug_streamer;

static int                info_enabled=0;
static ostream           *info_ostream=NULL;
static Streamer           info_streamer;

static int                warn_enabled=0;
static ostream           *warn_ostream=NULL;
static Streamer           warn_streamer;

static int                error_enabled=0;
static ostream           *error_ostream=NULL;  
static Streamer           error_streamer;


// Don't dare touch this semaphore in application code!
Sem4                      DebugLibSemaphore;


int MsgManager::setAllStreams(OutputDevice *device)
{
  if (device==NULL)
    return(1);

  DebugLibSemaphore.Wait();
  debug_streamer.setOutputDevice(device);
  delete(debug_ostream);
  debug_ostream=new ostream(&debug_streamer);

  info_streamer.setOutputDevice(device);
  delete(info_ostream);
  info_ostream=new ostream(&info_streamer);

  warn_streamer.setOutputDevice(device);
  delete(warn_ostream);
  warn_ostream=new ostream(&warn_streamer);

  error_streamer.setOutputDevice(device);
  delete(error_ostream);
  error_ostream=new ostream(&error_streamer);

  DebugLibSemaphore.Post();

  return(0);
}


int MsgManager::setDebugStream(OutputDevice *device)
{
  if (device==NULL)
    return(1);

  DebugLibSemaphore.Wait();
  debug_streamer.setOutputDevice(device);
  delete(debug_ostream);
  debug_ostream=new ostream(&debug_streamer);
  DebugLibSemaphore.Post();
  return(0);
}

int MsgManager::setInfoStream(OutputDevice *device)
{
  if (device==NULL)
    return(1);

  DebugLibSemaphore.Wait();
  info_streamer.setOutputDevice(device);
  delete(info_ostream);
  info_ostream=new ostream(&info_streamer);
  DebugLibSemaphore.Post();
  return(0);
}

int MsgManager::setWarnStream(OutputDevice *device)
{
  if (device==NULL)
    return(1);

  DebugLibSemaphore.Wait();
  warn_streamer.setOutputDevice(device);
  delete(warn_ostream);
  warn_ostream=new ostream(&warn_streamer);
  DebugLibSemaphore.Post();
  return(0);
}

int MsgManager::setErrorStream(OutputDevice *device)
{
  if (device==NULL)
    return(1);

  DebugLibSemaphore.Wait();
  error_streamer.setOutputDevice(device);
  delete(error_ostream);
  error_ostream=new ostream(&error_streamer);
  DebugLibSemaphore.Post();
  return(0);
}



ostream *MsgManager::debugStream(void)
{
  return(debug_ostream);
}   

ostream *MsgManager::infoStream(void)
{
  return(info_ostream);
}   

ostream *MsgManager::warnStream(void)
{
  return(warn_ostream);
}

ostream *MsgManager::errorStream(void)
{
  return(error_ostream);
}   
