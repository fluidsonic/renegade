#include "level.h"
#include "combat.h"
#include "pscene.h"
#include "savegame.h"
#include "WWAudio.H"
#include "SoundScene.h"
#include "gameobjmanager.h"
#include "assets.h"
#include "transition.h"
#include "conversationmgr.h"
#include "wwprofile.h"
#include "phys.h"
#include "rendobj.h"

/*
** Release all objects and resources loaded for this level
*/
void		LevelManager::Release_Level( void )
{
WWPROFILE( "Release Level" );
	SaveGameManager::Set_Map_Filename( NULL );
	ConversationMgrClass::Reset_Active_Conversations ();

	//
	// Stop (and free) any currently playing sounds
	//
   WWASSERT(WWAudioClass::Get_Instance() != NULL);
   SoundSceneClass *sound_scene = WWAudioClass::Get_Instance ()->Get_Sound_Scene ();
	if( sound_scene != NULL ) {
		sound_scene->Flush_Scene ();
	}	

	WWAudioClass::Get_Instance ()->Flush_Playlist ();

	GameObjManager::Destroy_All();		// Kill All Objects (including the Camera)

	TransitionManager::Reset();

{WWPROFILE( "Scene remove" );
	COMBAT_SCENE->Remove_All();
}

{WWPROFILE( "Free assets" );
	WW3DAssetManager::Get_Instance()->Free_Assets();	
}
}


