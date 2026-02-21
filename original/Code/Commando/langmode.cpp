#include "langmode.h"
#include "_globals.h"
#include "registry.h"
#include "netinterface.h"

//-----------------------------------------------------------------------------
void LanGameModeClass::Init(void)
{
	Load_Lan_Registry_Keys();
}

//-----------------------------------------------------------------------------
void LanGameModeClass::Shutdown(void)
{
	Save_Lan_Registry_Keys();
}

//-----------------------------------------------------------------------------
void LanGameModeClass::Load_Lan_Registry_Keys(void)
{
	RegistryClass registry(APPLICATION_SUB_KEY_NAME_NETOPTIONS);

	char name[200];
	registry.Get_String("MyLanName", name, sizeof(name), "");

	WideStringClass widename;
	widename.Convert_From(name);

	if (widename.Is_Empty()) {
		cNetInterface::Set_Random_Nickname();
	} else {
		cNetInterface::Set_Nickname(widename);
	}

	int sidePref = registry.Get_Int("SidePref", -1);
	cNetInterface::Set_Side_Preference(sidePref);
}

//-----------------------------------------------------------------------------
void LanGameModeClass::Save_Lan_Registry_Keys(void)
{
	RegistryClass registry(APPLICATION_SUB_KEY_NAME_NETOPTIONS);

	StringClass string;
	cNetInterface::Get_Nickname().Convert_To(string);
	registry.Set_String("MyLanName", string);

	registry.Set_Int("SidePref", cNetInterface::Get_Side_Preference());
}
