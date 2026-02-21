// registry.cpp - INI-file based registry replacement for macOS
// Stores all registry values in "renegade_registry.ini" in the working directory.
// Section names = registry sub-keys, entry names = value names.

#include "registry.h"
#include "rawfile.h"
#include "ini.h"
#include <assert.h>
#include <string.h>
#include <wchar.h>

bool RegistryClass::IsLocked = false;

static const char * REGISTRY_FILENAME = "renegade_registry.ini";

static INIClass & Get_Registry_INI()
{
	static INIClass s_ini;
	static bool s_loaded = false;
	if (!s_loaded) {
		s_loaded = true;
		RawFileClass f(REGISTRY_FILENAME);
		if (f.Is_Available()) {
			s_ini.Load(f);
		}
	}
	return s_ini;
}

static void Save_Registry_INI()
{
	RawFileClass f(REGISTRY_FILENAME);
	Get_Registry_INI().Save(f);
}

bool RegistryClass::Exists(const char * sub_key)
{
	return Get_Registry_INI().Find_Section(sub_key) != NULL;
}

RegistryClass::RegistryClass(const char * sub_key, bool create) : IsValid(false)
{
	if (sub_key && *sub_key) {
		strncpy(SubKey, sub_key, sizeof(SubKey) - 1);
		SubKey[sizeof(SubKey) - 1] = '\0';
		IsValid = true;
	}
}

RegistryClass::~RegistryClass(void)
{
	IsValid = false;
}

int RegistryClass::Get_Int(const char * name, int def_value)
{
	assert(IsValid);
	return Get_Registry_INI().Get_Int(SubKey, name, def_value);
}

void RegistryClass::Set_Int(const char * name, int value)
{
	assert(IsValid);
	if (IsLocked) return;
	Get_Registry_INI().Clear(SubKey, name);
	Get_Registry_INI().Put_Int(SubKey, name, value);
	Save_Registry_INI();
}

bool RegistryClass::Get_Bool(const char * name, bool def_value)
{
	assert(IsValid);
	return Get_Registry_INI().Get_Bool(SubKey, name, def_value);
}

void RegistryClass::Set_Bool(const char * name, bool value)
{
	assert(IsValid);
	if (IsLocked) return;
	Get_Registry_INI().Clear(SubKey, name);
	Get_Registry_INI().Put_Bool(SubKey, name, value);
	Save_Registry_INI();
}

float RegistryClass::Get_Float(const char * name, float def_value)
{
	assert(IsValid);
	return Get_Registry_INI().Get_Float(SubKey, name, def_value);
}

void RegistryClass::Set_Float(const char * name, float value)
{
	assert(IsValid);
	if (IsLocked) return;
	Get_Registry_INI().Clear(SubKey, name);
	Get_Registry_INI().Put_Float(SubKey, name, value);
	Save_Registry_INI();
}

char * RegistryClass::Get_String(const char * name, char * value, int value_size,
	const char * default_string)
{
	assert(IsValid);
	const char * def = (default_string != NULL) ? default_string : "";
	Get_Registry_INI().Get_String(SubKey, name, def, value, value_size);
	return value;
}

void RegistryClass::Get_String(const char * name, StringClass & string, const char * default_string)
{
	assert(IsValid);
	const char * def = (default_string != NULL) ? default_string : "";
	Get_Registry_INI().Get_String(string, SubKey, name, def);
}

void RegistryClass::Set_String(const char * name, const char * value)
{
	assert(IsValid);
	if (IsLocked) return;
	Get_Registry_INI().Clear(SubKey, name);
	Get_Registry_INI().Put_String(SubKey, name, value);
	Save_Registry_INI();
}

void RegistryClass::Get_String(const WCHAR * name, WideStringClass & string, const WCHAR * default_string)
{
	assert(IsValid);
	// Convert WCHAR key name to narrow for INI lookup (key names are always ASCII)
	char narrow_name[256];
	int i = 0;
	while (name[i] && i < 255) { narrow_name[i] = (char)name[i]; i++; }
	narrow_name[i] = '\0';

	const WCHAR * def = (default_string != NULL) ? default_string : (const WCHAR*)u"";
	Get_Registry_INI().Get_Wide_String(string, SubKey, narrow_name, def);
}

void RegistryClass::Set_String(const WCHAR * name, const WCHAR * value)
{
	assert(IsValid);
	if (IsLocked) return;
	char narrow_name[256];
	int i = 0;
	while (name[i] && i < 255) { narrow_name[i] = (char)name[i]; i++; }
	narrow_name[i] = '\0';

	Get_Registry_INI().Clear(SubKey, narrow_name);
	Get_Registry_INI().Put_Wide_String(SubKey, narrow_name, value);
	Save_Registry_INI();
}

int RegistryClass::Get_Bin_Size(const char * name)
{
	assert(IsValid);
	unsigned char tmp[8192];
	return Get_Registry_INI().Get_UUBlock(SubKey, name, tmp, sizeof(tmp));
}

void RegistryClass::Get_Bin(const char * name, void * buffer, int buffer_size)
{
	assert(IsValid);
	assert(buffer != NULL);
	assert(buffer_size > 0);
	Get_Registry_INI().Get_UUBlock(SubKey, name, buffer, buffer_size);
}

void RegistryClass::Set_Bin(const char * name, const void * buffer, int buffer_size)
{
	assert(IsValid);
	assert(buffer != NULL);
	assert(buffer_size > 0);
	if (IsLocked) return;
	Get_Registry_INI().Clear(SubKey, name);
	Get_Registry_INI().Put_UUBlock(SubKey, name, buffer, buffer_size);
	Save_Registry_INI();
}

void RegistryClass::Get_Value_List(DynamicVectorClass<StringClass> & list)
{
	int count = Get_Registry_INI().Entry_Count(SubKey);
	for (int i = 0; i < count; i++) {
		const char * entry = Get_Registry_INI().Get_Entry(SubKey, i);
		if (entry) {
			list.Add(entry);
		}
	}
}

void RegistryClass::Delete_Value(const char * name)
{
	if (IsLocked) return;
	Get_Registry_INI().Clear(SubKey, name);
	Save_Registry_INI();
}

void RegistryClass::Deleta_All_Values(void)
{
	if (IsLocked) return;
	// Clear all entries in section but keep the section itself
	DynamicVectorClass<StringClass> value_list;
	Get_Value_List(value_list);
	for (int i = 0; i < value_list.Count(); i++) {
		Get_Registry_INI().Clear(SubKey, value_list[i]);
	}
	Save_Registry_INI();
}

// Bulk operations - no-op on macOS (these were Windows registry import/export utilities)
void RegistryClass::Save_Registry(const char * filename, char * path) {}
void RegistryClass::Load_Registry(const char * filename, char * old_path, char * new_path) {}
void RegistryClass::Delete_Registry_Tree(char * path) {}
