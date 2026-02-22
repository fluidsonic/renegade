//
// Filename:     regint.h
// Author:       Tom Spencer-Smith
// Date:         Dec 1998
// Description:  
//
//-----------------------------------------------------------------------------
#if defined(_MSV_VER)
#pragma once
#endif

#ifndef REGINT_H
#define REGINT_H


//-----------------------------------------------------------------------------
class cRegistryInt
{
	public:
      cRegistryInt(LPCSTR registry_location, LPCSTR key_name, int initial_value);

      void Set(int value);
      int Get(void) const {return Value;}

	private:

      int Value;
      char RegistryLocation[400];
      char KeyName[100];
};

//-----------------------------------------------------------------------------
#endif // REGINT_H
