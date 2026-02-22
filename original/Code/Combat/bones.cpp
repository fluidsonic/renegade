#include "global.h"
#include "bones.h"
#include "assets.h"
#include "wwstring.h"
#include <map>

static bool _BonesMangerInitted = false;

/*
**
*/
class	BoneDataClass {
public:
	BoneDataClass( const char * screen_name, float damage_scale ) : 
		ScreenName( screen_name ), DamageScale( damage_scale ) {}

	StringClass		ScreenName;
	float				DamageScale;
};

typedef	std::map< StringClass , BoneDataClass * >  BoneMapper;
BoneMapper	BoneMap;

/*
** Database Loading
*/
#define	BONES_INI_FILENAME					"BONES.INI"
#define	SECTION_LIST							"Bones_List"
#define	ENTRY_NAME								"Name"
#define	ENTRY_SCREEN_NAME						"ScreenName"
#define	ENTRY_DAMAGE_SCALE					"DamageScale"

void Load_Bones( void )
{
	INIClass	* bonesINI = Get_INI( BONES_INI_FILENAME );
	if (bonesINI != NULL) {

		int count =  bonesINI->Entry_Count( SECTION_LIST );		// Load gang list
		for ( int entry = 0; entry < count; entry++ )	{
			char	name[80];
			char	screen_name[80];
			char	section_name[80];
			bonesINI->Get_String(	SECTION_LIST, bonesINI->Get_Entry( SECTION_LIST, entry),
										"",	section_name,		sizeof( section_name ) );

			bonesINI->Get_String(	section_name, ENTRY_NAME, "",	name,	sizeof( name ) );
			bonesINI->Get_String(	section_name, ENTRY_SCREEN_NAME, "",	screen_name,	sizeof( screen_name ) );
			float damage_scale = bonesINI->Get_Float(	section_name, ENTRY_DAMAGE_SCALE, 1.0 );
			BoneMap[ name ] = new BoneDataClass( screen_name, damage_scale );
	//		Debug_Say(( "Add bone %s %s %f\n", name, screen_name, damage_scale ));
		}

		Release_INI( bonesINI );
	} else {
		Debug_Say(("Load_Bones - Unable to load %s\n", BONES_INI_FILENAME));
	}
}

/*
**
*/
void BonesManager::Init( void )
{
	if (_BonesMangerInitted) {
		Shutdown();
	}

	Load_Bones();
	_BonesMangerInitted = true;
}

void BonesManager::Shutdown( void )
{
	for(BoneMapper::iterator it = BoneMap.begin(); it != BoneMap.end(); it++)	{
		delete it->second;
	}
	BoneMap.clear();
	_BonesMangerInitted = false;
}

const char * BonesManager::Get_Bone_Screen_Name( const char * bone_name )
{
	BoneMapper::iterator i = BoneMap.find( bone_name );
	if ( i != BoneMap.end() ) {
		return i->second->ScreenName;
	}
	return "???";
}

float BonesManager::Get_Bone_Damage_Scale( const char * bone_name )
{
	StringClass bone_name_string(bone_name,true);
	BoneMapper::iterator i = BoneMap.find( bone_name_string );
	if ( i != BoneMap.end() ) {
		return i->second->DamageScale;
	}
	return 1.0f;
}
