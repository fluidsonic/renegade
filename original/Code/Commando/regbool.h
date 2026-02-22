//
// Filename:     regbool.h
// Author:       Tom Spencer-Smith
// Date:         Dec 1998
// Description:  
//
//-----------------------------------------------------------------------------
#if defined(_MSV_VER)
#pragma once
#endif

#ifndef REGBOOL_H
#define REGBOOL_H

#include "global.h"

//-----------------------------------------------------------------------------
class cRegistryBool
{
	public:
      cRegistryBool(LPCSTR registry_location, LPCSTR key_name, 
			bool initial_value = false);

      bool Toggle(void);
      bool Set(bool value);
      bool Get(void) const          {return Value == 1;}
      bool Is_True(void) const      {return Value == 1;}
      bool Is_False(void) const     {return Value == 0;}

	private:

      int Value;
      char RegistryLocation[400];
      char KeyName[100];
};

//-----------------------------------------------------------------------------
#endif // REGBOOL_H
