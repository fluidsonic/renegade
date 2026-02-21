//
// Filename:     regfloat.cpp
// Author:       Tom Spencer-Smith
// Date:         Dec 1998
// Description:  
//

#include "regfloat.h"

#include "string.h"
#include "registry.h"

//
// Class statics
//

//-----------------------------------------------------------------------------
cRegistryFloat::cRegistryFloat(LPCSTR registry_location, LPCSTR key_name, float default_value)
{
	if (registry_location == NULL) {
      strcpy(RegistryLocation, "");
      strcpy(KeyName, "");
      Set(default_value);
   } else {
      strcpy(RegistryLocation, registry_location);
      strcpy(KeyName, key_name);

	   RegistryClass * registry = new RegistryClass(RegistryLocation);
		int temp_1 = 0;
		::memcpy(&temp_1, &default_value, sizeof(default_value));
		int temp_2 = registry->Get_Int(KeyName, temp_1);
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
		int temp = 0;
		::memcpy(&temp, &Value, sizeof(Value));
      registry->Set_Int(KeyName, temp);
   	delete registry;
   }
}
