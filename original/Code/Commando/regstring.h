//
// Filename:     regstring.h
// Author:       Tom Spencer-Smith
// Date:         Dec 1998
// Description:  
//
//-----------------------------------------------------------------------------
#if defined(_MSV_VER)
#pragma once
#endif

#ifndef REGSTRING_H
#define REGSTRING_H


//-----------------------------------------------------------------------------
class cRegistryString
{
	public:
      cRegistryString(LPCSTR registry_location, LPCSTR key_name, LPCSTR initial_value);

      void Set(LPCSTR value);
      LPCSTR Get(void) const {return Value;}

	private:

      char Value[200];
      char RegistryLocation[400];
      char KeyName[100];
};

//-----------------------------------------------------------------------------
#endif // REGSTRING_H
