#include "mpsettingsmgr.h"
#include "registry.h"
#include "_globals.h"
#include "time.h"

////////////////////////////////////////////////////////////////
//	Static member initialization
////////////////////////////////////////////////////////////////
StringClass								MPSettingsMgrClass::SelectedLogin;
StringClass								MPSettingsMgrClass::LastLogin;
StringClass								MPSettingsMgrClass::AutoLogin;
StringClass								MPSettingsMgrClass::AutoPassword;
bool										MPSettingsMgrClass::HasMOTDBeenViewed				= false;
bool										MPSettingsMgrClass::DisplaySidebarHelp				= true;
bool										MPSettingsMgrClass::IsAutoLoginPromptEnabled		= true;
bool										MPSettingsMgrClass::AreSkinsUnlocked				= false;

#ifdef OBSOLETE
MPSettingsMgrClass::GameModeMap	MPSettingsMgrClass::_mModePrefs;
#endif

int										MPSettingsMgrClass::OptionFlags	= OPTION_DEFAULTS;

////////////////////////////////////////////////////////////////
//	Constants
////////////////////////////////////////////////////////////////
static const int MAX_PERSONA_LEN			= 64;

////////////////////////////////////////////////////////////////////////////////////////////////
//	Registry key names
////////////////////////////////////////////////////////////////////////////////////////////////
//static const char *QUICKMATCH_SUB_KEY	= APPLICATION_SUB_KEY_NAME_MP_SETTINGS "\\QuickMatch";

////////////////////////////////////////////////////////////////////////////////////////////////
//	Registry value names
////////////////////////////////////////////////////////////////////////////////////////////////
static const char *REG_VALUE_LAST_LOGIN			= "LastLogin";
static const char *REG_VALUE_AUTOLOGIN				= "AutoLogin";
static const char *REG_VALUE_AUTOPASSWORD			= "AutoPassword";
static const char *REG_VALUE_SIDEBAR_HELP			= "SidebarHelp";
static const char *REG_VALUE_AUTOLOGIN_PROMPT	= "AutoLoginPrompt";
static const char *REG_VALUE_OPTIONS				= "Options";
static const char *REG_VALUE_ARE_SKINS_UNLOCKED	= "PrimeSocket";

////////////////////////////////////////////////////////////////
//
//	Load_Settings
//
////////////////////////////////////////////////////////////////
void
MPSettingsMgrClass::Load_Settings (void)
{
	//
	//	Attempt to open the MP settings sub-key
	//
	RegistryClass registry (APPLICATION_SUB_KEY_NAME_MP_SETTINGS, false);
	if (registry.Is_Valid ()) {

		//
		//	Read the simple data from the registry
		//
		registry.Get_String(REG_VALUE_LAST_LOGIN, LastLogin.Get_Buffer(MAX_PERSONA_LEN), MAX_PERSONA_LEN, "");
		registry.Get_String(REG_VALUE_AUTOLOGIN, AutoLogin.Get_Buffer(MAX_PERSONA_LEN), MAX_PERSONA_LEN, "");
		registry.Get_String(REG_VALUE_AUTOPASSWORD, AutoPassword.Get_Buffer(MAX_PERSONA_LEN), MAX_PERSONA_LEN, "");

		DisplaySidebarHelp			= registry.Get_Bool (REG_VALUE_SIDEBAR_HELP, true);
		IsAutoLoginPromptEnabled	= registry.Get_Bool (REG_VALUE_AUTOLOGIN_PROMPT, true);
		AreSkinsUnlocked				= registry.Get_Bool (REG_VALUE_ARE_SKINS_UNLOCKED, false);

		// WOL removed - always use Latin/Western defaults
		int defaultOptions = OPTION_DEFAULTS_LATIN;

		OptionFlags = registry.Get_Int (REG_VALUE_OPTIONS, defaultOptions);
	}

	return ;
}

////////////////////////////////////////////////////////////////
//
//	Set_Last_Logon
//
////////////////////////////////////////////////////////////////
void
MPSettingsMgrClass::Set_Last_Login(const char *name)
{
	LastLogin = name;

	RegistryClass registry(APPLICATION_SUB_KEY_NAME_MP_SETTINGS);

	if (registry.Is_Valid()) {
		registry.Set_String(REG_VALUE_LAST_LOGIN, LastLogin);
	}
}

////////////////////////////////////////////////////////////////
//
//	Get_Last_Logon
//
////////////////////////////////////////////////////////////////
const char*
MPSettingsMgrClass::Get_Last_Login(void)
{
	return (const char*)LastLogin;
}

////////////////////////////////////////////////////////////////
//
//	Set_Auto_Login
//
////////////////////////////////////////////////////////////////
void
MPSettingsMgrClass::Set_Auto_Login(const char* login)
{
	AutoLogin = login;

	RegistryClass registry(APPLICATION_SUB_KEY_NAME_MP_SETTINGS);

	if (registry.Is_Valid()) {
		registry.Set_String(REG_VALUE_AUTOLOGIN, AutoLogin);
	}
}

////////////////////////////////////////////////////////////////
//
//	Get_Auto_Login
//
////////////////////////////////////////////////////////////////
const char*
MPSettingsMgrClass::Get_Auto_Login(void)
{
	return (const char*)AutoLogin;
}

////////////////////////////////////////////////////////////////
//
//	Set_Auto_Password
//
////////////////////////////////////////////////////////////////
void
MPSettingsMgrClass::Set_Auto_Password(const char* pass)
{
	AutoPassword = pass;

	RegistryClass registry(APPLICATION_SUB_KEY_NAME_MP_SETTINGS);

	if (registry.Is_Valid()) {
		registry.Set_String(REG_VALUE_AUTOPASSWORD, AutoPassword);
	}
}

////////////////////////////////////////////////////////////////
//
//	Get_Auto_Password
//
////////////////////////////////////////////////////////////////
const char*
MPSettingsMgrClass::Get_Auto_Password(void)
{
	return (const char*)AutoPassword;
}

#ifdef OBSOLETE
////////////////////////////////////////////////////////////////
//
//	Get_QuickMatch_Mode_Preference
//
////////////////////////////////////////////////////////////////
int
MPSettingsMgrClass::Get_QuickMatch_Mode_Preference (const char *mode)
{
	GameModeMap::iterator modePref = _mModePrefs.find(mode);

	if (modePref == _mModePrefs.end()) {
		RegistryClass registry(APPLICATION_SUB_KEY_NAME_QUICKMATCH);

		if (registry.Is_Valid()) {
			int pref = registry.Get_Int(mode, 10);
			_mModePrefs[mode] = pref;
			return pref;
		}
	}

	return (*modePref).second;
}

////////////////////////////////////////////////////////////////
//
//	Set_QuickMatch_Mode_Preference
//
////////////////////////////////////////////////////////////////
void
MPSettingsMgrClass::Set_QuickMatch_Mode_Preference (const char *mode, int preference)
{
	_mModePrefs[mode] = preference;
	return ;
}

////////////////////////////////////////////////////////////////
//
//	Save_QuickMatch_Mode_Preferences
//
////////////////////////////////////////////////////////////////
void
MPSettingsMgrClass::Save_QuickMatch_Mode_Preferences (void)
{
	RegistryClass registry(APPLICATION_SUB_KEY_NAME_QUICKMATCH);

	if (registry.Is_Valid()) {
		GameModeMap::iterator iter = _mModePrefs.begin();

		while (iter != _mModePrefs.end()) {
			registry.Set_Int((*iter).first, (*iter).second);
			iter++;
		}
	}
}
#endif // OBSOLETE

////////////////////////////////////////////////////////////////
//
//	Set_Is_Sidebar_Help_Displayed
//
////////////////////////////////////////////////////////////////
void
MPSettingsMgrClass::Set_Is_Sidebar_Help_Displayed (bool onoff)
{
	DisplaySidebarHelp = onoff;

	//
	//	Attempt to open the MP settings sub-key
	//
	RegistryClass registry (APPLICATION_SUB_KEY_NAME_MP_SETTINGS);
	if (registry.Is_Valid ()) {
		registry.Set_Bool (REG_VALUE_SIDEBAR_HELP, DisplaySidebarHelp);
	}

	return ;
}

////////////////////////////////////////////////////////////////
//
//	Enable_Auto_Login_Prompt
//
////////////////////////////////////////////////////////////////
void
MPSettingsMgrClass::Enable_Auto_Login_Prompt (bool onoff)
{
	IsAutoLoginPromptEnabled = onoff;

	//
	//	Attempt to open the MP settings sub-key
	//
	RegistryClass registry (APPLICATION_SUB_KEY_NAME_MP_SETTINGS);
	if (registry.Is_Valid ()) {
		registry.Set_Bool (REG_VALUE_AUTOLOGIN_PROMPT, IsAutoLoginPromptEnabled);
	}

	return ;
}

////////////////////////////////////////////////////////////////
//
//	Set_Option_Flag
//
////////////////////////////////////////////////////////////////
void
MPSettingsMgrClass::Set_Option_Flag (OPTION flag, bool onoff)
{
	if (onoff) {
		OptionFlags |= flag;
	} else {
		OptionFlags &= (~flag);
	}

	//
	//	Save this setting in the registry
	//
	RegistryClass registry (APPLICATION_SUB_KEY_NAME_MP_SETTINGS);
	if (registry.Is_Valid ()) {
		registry.Set_Int (REG_VALUE_OPTIONS, OptionFlags);
	}

	return ;
}

////////////////////////////////////////////////////////////////
//
//	Are_Alternate_Skins_Unlocked
//
////////////////////////////////////////////////////////////////
bool
MPSettingsMgrClass::Are_Alternate_Skins_Unlocked (void)
{
	// WOL removed - the unlock date (March 15, 2002) has long since passed, always return true
	return true;
}
