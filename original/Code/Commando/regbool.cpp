//
// Filename:     regbool.cpp
// Author:       Tom Spencer-Smith
// Date:         Dec 1998
// Description:  
//

#include "regbool.h" // I WANNA BE FIRST!

#include "string.h"
#include "registry.h"
#include "wwdebug.h"
#include "wwmemlog.h"

//
// Class statics
//

//-----------------------------------------------------------------------------
cRegistryBool::cRegistryBool(LPCSTR registry_location, LPCSTR key_name, bool default_value)
{
	WWMEMLOG(MEM_GAMEDATA);

	WWASSERT(registry_location != NULL);
   WWASSERT(key_name != NULL);
   WWASSERT(strlen(registry_location) < sizeof(RegistryLocation));
   WWASSERT(strlen(key_name) < sizeof(KeyName));
   strcpy(RegistryLocation, registry_location);
   strcpy(KeyName, key_name);

	RegistryClass * registry = new RegistryClass(RegistryLocation);
	WWASSERT(registry != NULL && registry->Is_Valid());
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

	WWASSERT(RegistryLocation != NULL);
   WWASSERT(KeyName != NULL);
	RegistryClass * registry = new RegistryClass(RegistryLocation);
	WWASSERT(registry != NULL && registry->Is_Valid());
   registry->Set_Int(KeyName, Value);
   delete registry;

   return Value == 1;
}
