//
// Filename:     regfloat.cpp
// Author:       Tom Spencer-Smith
// Date:         Dec 1998
// Description:  
//

#include "regfloat.h"

#include "string.h"
#include "registry.h"
#include "wwdebug.h"
#include "wwmemlog.h"

//
// Class statics
//

//-----------------------------------------------------------------------------
cRegistryFloat::cRegistryFloat(LPCSTR registry_location, LPCSTR key_name, float default_value)
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
		int temp_1 = 0;
		WWASSERT(sizeof(temp_1) == sizeof(default_value));
		::memcpy(&temp_1, &default_value, sizeof(default_value));
		int temp_2 = registry->Get_Int(KeyName, temp_1);
		WWASSERT(sizeof(temp_2) == sizeof(Value));
		::memcpy(&Value, &temp_2, sizeof(temp_2));
   	delete registry;

      Set(Value);
   }
}

//-----------------------------------------------------------------------------
void cRegistryFloat::Set(float value)
{
   Value = value;

   if (strcmp(RegistryLocation, "")) {
	   RegistryClass * registry = new RegistryClass(RegistryLocation);
	   WWASSERT(registry != NULL && registry->Is_Valid());
		int temp = 0;
		WWASSERT(sizeof(temp) == sizeof(Value));
		::memcpy(&temp, &Value, sizeof(Value));
      registry->Set_Int(KeyName, temp);
   	delete registry;
   }
}
