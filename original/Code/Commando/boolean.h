//
// Filename:     boolean.h
// Author:       Tom Spencer-Smith
// Date:         Nov 1999
// Description:  
//
//-----------------------------------------------------------------------------
#if defined(_MSV_VER)
#pragma once
#endif

#ifndef BOOLEAN_H
#define BOOLEAN_H

//-----------------------------------------------------------------------------
class cBoolean
{
	public:
      cBoolean(bool value = false)  {Value = value;}
      bool Toggle(void)             {Value = !Value; return Value;}
      bool Set(bool value)          {Value = value; return Value;}
      bool Get(void)       const    {return Value;}
      bool Is_True(void)   const    {return Value == true;}
      bool Is_False(void)  const    {return Value == false;}

	private:

      bool Value;
};

//-----------------------------------------------------------------------------
#endif // BOOLEAN_H
