#include "combatsound.h"
#include "wwaudio.h"

/*
**
*/
const char * CombatSoundTypeNames[NUM_DEFINED_SOUND_TYPES] = 
{
	"None",
	"Old Weapon",
	"Footsteps",
	"Vehicle",
	"Gunshot",
	"Bullet Hit"
};

/*
**
*/
void	CombatSoundManager::Init( void )
{
	for ( int i = 0; i < NUM_DEFINED_SOUND_TYPES; i++ ) {
		WWAudioClass::Get_Instance()->Add_Logical_Type( i, CombatSoundTypeNames[i] );
	}

	for ( int i = SOUND_TYPE_DESIGNER01; i <= SOUND_TYPE_DESIGNER09; i++ ) {
		
		StringClass type_name;
		type_name.Format ("Designer%02d", (i - SOUND_TYPE_DESIGNER01) + 1);
		WWAudioClass::Get_Instance()->Add_Logical_Type( i, type_name );
	}
}

void	CombatSoundManager::Shutdown( void )
{
	WWAudioClass::Get_Instance()->Reset_Logical_Types();
}


