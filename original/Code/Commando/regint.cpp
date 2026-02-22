//
// Filename:     regint.cpp
// Author:       Tom Spencer-Smith
// Date:         Dec 1998
// Description:  
//

#include "global.h"
#include "regint.h" // I WANNA BE FIRST!

#include "string.h"
#include "registry.h"

//
// Class statics
//

//-----------------------------------------------------------------------------
cRegistryInt::cRegistryInt(LPCSTR registry_location, LPCSTR key_name, int default_value)
{
	if (registry_location == NULL) {
      strcpy(RegistryLocation, "");
      strcpy(KeyName, "");
      Set(default_value);
   } else {
      strcpy(RegistryLocation, registry_location);
      strcpy(KeyName, key_name);

	   RegistryClass * registry = new RegistryClass(RegistryLocation);
      Value = registry->Get_Int(KeyName, default_value);
   	delete registry;

      Set(Value);
   }
}

//-----------------------------------------------------------------------------
void cRegistryInt::Set(int value)
{
   Value = value;

   if (strcmp(RegistryLocation, "")) {
	   RegistryClass * registry = new RegistryClass(RegistryLocation);
      registry->Set_Int(KeyName, Value);
   	delete registry;
   }
}
