#pragma once

#include "global.h"

#include <stdlib.h>
#include <stdio.h>
#include <syslog.h>
#include <string.h>
#include "odevice.h"

class SyslogD : public OutputDevice
{
 public:
   SyslogD(char *ident,int logopt,int facility,int priority);
   virtual int print(const char *str,int len);

 private:
   int priority;
};
