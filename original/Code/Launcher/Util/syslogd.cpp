#include "global.h"
#include "syslogd.h"

SyslogD::SyslogD(char *ident,int logopt,int facility,int _priority)
{
  openlog(ident,logopt,facility);
  priority=_priority;
}

int SyslogD::print(const char *str, int len)
{
  char *temp_str=new char[len+1];
  memset(temp_str,0,len+1);
  strncpy(temp_str,str,len);
  syslog(priority,temp_str);
  delete[](temp_str);
  return(len);
}
