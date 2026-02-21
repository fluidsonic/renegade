//
// Filename:     regstring.cpp
// Author:       Tom Spencer-Smith
// Date:         Dec 1998
// Description:  
//

#include "regstring.h" // I WANNA BE FIRST!

#include "string.h"
#include "registry.h"

//
// Class statics
//

//-----------------------------------------------------------------------------
cRegistryString::cRegistryString(LPCSTR registry_location, LPCSTR key_name, 
	LPCSTR default_value)
{

   if (registry_location == NULL) {
      strcpy(RegistryLocation, "");
      strcpy(KeyName, "");
      Set(default_value);
   } else {
      strcpy(RegistryLocation, registry_location);
      strcpy(KeyName, key_name);

	   RegistryClass * registry = new RegistryClass(RegistryLocation);
		registry->Get_String(KeyName, Value, sizeof(Value), default_value);
   	delete registry;

      Set(Value);
   }
}

//-----------------------------------------------------------------------------
void cRegistryString::Set(LPCSTR value)
{

   strcpy(Value, value);

   if (strcmp(RegistryLocation, "")) {
	   RegistryClass * registry = new RegistryClass(RegistryLocation);
		registry->Set_String(KeyName, Value);
   	delete registry;
   }
}
