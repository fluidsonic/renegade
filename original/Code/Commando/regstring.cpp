//
// Filename:     regstring.cpp
// Author:       Tom Spencer-Smith
// Date:         Dec 1998
// Description:  
//

#include "regstring.h" // I WANNA BE FIRST!

#include "string.h"
#include "registry.h"
#include "wwdebug.h"

//
// Class statics
//

//-----------------------------------------------------------------------------
cRegistryString::cRegistryString(LPCSTR registry_location, LPCSTR key_name, 
	LPCSTR default_value)
{
   WWASSERT(default_value != NULL);

   if (registry_location == NULL) {
      strcpy(RegistryLocation, "");
      strcpy(KeyName, "");
      Set(default_value);
   } else {
      WWASSERT(strlen(registry_location) < sizeof(RegistryLocation));
      WWASSERT(key_name != NULL);
      WWASSERT(strlen(key_name) < sizeof(KeyName));
      strcpy(RegistryLocation, registry_location);
      strcpy(KeyName, key_name);

	   RegistryClass * registry = new RegistryClass(RegistryLocation);
	   WWASSERT(registry != NULL && registry->Is_Valid());
		registry->Get_String(KeyName, Value, sizeof(Value), default_value);
   	delete registry;

      Set(Value);
   }
}

//-----------------------------------------------------------------------------
void cRegistryString::Set(LPCSTR value)
{
   WWASSERT(value != NULL);
   WWASSERT(strlen(value) < sizeof(Value));

   strcpy(Value, value);

   if (strcmp(RegistryLocation, "")) {
	   RegistryClass * registry = new RegistryClass(RegistryLocation);
	   WWASSERT(registry != NULL && registry->Is_Valid());
		registry->Set_String(KeyName, Value);
   	delete registry;
   }
}
