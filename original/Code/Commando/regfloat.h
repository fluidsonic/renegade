#pragma once

#include "global.h"

//
// Filename:     regfloat.h
// Author:       Tom Spencer-Smith
// Date:         Dec 1998
// Description:  
//
//-----------------------------------------------------------------------------



//-----------------------------------------------------------------------------
class cRegistryFloat
{
	public:
      cRegistryFloat(LPCSTR registry_location, LPCSTR key_name, float initial_value);

      void Set(float value);
      float Get(void) const {return Value;}

	private:

      float	Value;
      char	RegistryLocation[400];
      char	KeyName[100];
};

//-----------------------------------------------------------------------------
