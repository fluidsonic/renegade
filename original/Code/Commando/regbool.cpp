//
// Filename:     regbool.cpp
// Author:       Tom Spencer-Smith
// Date:         Dec 1998
// Description:  
//

#include "regbool.h" // I WANNA BE FIRST!

#include "string.h"
#include "registry.h"

//
// Class statics
//

//-----------------------------------------------------------------------------
cRegistryBool::cRegistryBool(LPCSTR registry_location, LPCSTR key_name, bool default_value)
{

   strcpy(RegistryLocation, registry_location);
   strcpy(KeyName, key_name);

	RegistryClass * registry = new RegistryClass(RegistryLocation);
   Value = registry->Get_Int(KeyName, default_value == 1);
   delete registry;

   Set(Value == 1);
}

//-----------------------------------------------------------------------------
bool cRegistryBool::Toggle(void)
{
   return Set(!Value);
}

//-----------------------------------------------------------------------------
bool cRegistryBool::Set(bool value)
{
   Value = value;

	RegistryClass * registry = new RegistryClass(RegistryLocation);
   registry->Set_Int(KeyName, Value);
   delete registry;

   return Value == 1;
}
