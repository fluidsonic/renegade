#pragma once

#include "global.h"

#include <stdlib.h>
#include <stdio.h>
#include "odevice.h"

///////////////////////// WIN32 ONLY ///////////////////////////////////


class MonoD : public OutputDevice
{
 public:
                   MonoD();
                  ~MonoD();

   virtual int    print(const char *str,int len);

 private:
};
