//
// Filename:     regint.cpp
// Author:       Tom Spencer-Smith
// Date:         Dec 1998
// Description:  
//

#include "regint.h" // I WANNA BE FIRST!

#include "string.h"
#include "registry.h"
#include "wwdebug.h"
#include "wwmemlog.h"

//
// Class statics
//

//-----------------------------------------------------------------------------
cRegistryInt::cRegistryInt(LPCSTR registry_location, LPCSTR key_name, int default_value)
{
   WWMEMLOG(MEM_GAMEDATA);
	if (registry_location == NULL) {
      strcpy(RegistryLocation, "");
      strcpy(KeyName, "");
      Set(default_value);
   } else {
      WWASSERT(key_name != NULL);
      WWASSERT(strlen(registry_location) < sizeof(RegistryLocation));
      WWASSERT(strlen(key_name) < sizeof(KeyName));
      strcpy(RegistryLocation, registry_location);
      strcpy(KeyName, key_name);

	   RegistryClass * registry = new RegistryClass(RegistryLocation);
	   WWASSERT(registry != NULL && registry->Is_Valid());
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
	   WWASSERT(registry != NULL && registry->Is_Valid());
      registry->Set_Int(KeyName, Value);
   	delete registry;
   }
}
