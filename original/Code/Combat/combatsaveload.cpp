#include "combatsaveload.h"
#include "chunkio.h"
#include "gameobjmanager.h"
#include "combat.h"
#include "debug.h"
#include "spawn.h"
#include "timemgr.h"
#include "scripts.h"
#include "persistentgameobjobserver.h"
#include "cover.h"
#include "objectives.h"
#include "radar.h"
#include "building.h"
#include "bullet.h"
#include "backgroundmgr.h"
#include "weathermgr.h"
#include "weaponview.h"
#include "hud.h"
#include "screenfademanager.h"

/*
**
*/
CombatSaveLoadClass	_CombatSaveLoad;

enum	{
	CHUNKID_GAMEOBJMANAGER					=	916991654,
	CHUNKID_COMBAT_GAME_MODE,
	XXX_CHUNKID_TRANSITIONS,
	CHUNKID_SPAWNERS,
	XXXCHUNKID_TIME,
	CHUNKID_SCRIPTS,
	CHUNKID_PERSISTENT_GAME_OBJ_OBSERVERS,
	CHUNKID_COVER,
	CHUNKID_OBJECTIVES,
	CHUNKID_RADAR,
	XXXCHUNKID_BUILDINGS,
	CHUNKID_GAME_OBJ_OBSERVERS,
	CHUNKID_BULLETS,
	CHUNKID_WEAPON_VIEW,
	CHUNKID_DYNAMIC_BACKGROUND,
	CHUNKID_DYNAMIC_WEATHER,
	CHUNKID_HUD,
	CHUNKID_SCREEN_FADE,
};

/*
**
*/
bool	CombatSaveLoadClass::Save( ChunkSaveClass &csave )
{

	csave.Begin_Chunk( CHUNKID_GAMEOBJMANAGER );
	GameObjManager::Save( csave );
	csave.End_Chunk();

	// CombatManager should load before scripts for SyncTime
	csave.Begin_Chunk( CHUNKID_COMBAT_GAME_MODE );
	CombatManager::Save( csave );
	csave.End_Chunk();

	csave.Begin_Chunk( CHUNKID_SPAWNERS );
	SpawnManager::Save( csave );
	csave.End_Chunk();

	csave.Begin_Chunk( CHUNKID_SCRIPTS );
	ScriptManager::Save( csave );
	csave.End_Chunk();

	csave.Begin_Chunk( CHUNKID_PERSISTENT_GAME_OBJ_OBSERVERS );
	PersistentGameObjObserverManager::Save( csave );
	csave.End_Chunk();

	csave.Begin_Chunk( CHUNKID_COVER );
	CoverManager::Save( csave );
	csave.End_Chunk();

	csave.Begin_Chunk( CHUNKID_OBJECTIVES );
	ObjectiveManager::Save( csave );
	csave.End_Chunk();

	csave.Begin_Chunk( CHUNKID_RADAR );
	RadarManager::Save( csave );
	csave.End_Chunk();

	csave.Begin_Chunk( CHUNKID_GAME_OBJ_OBSERVERS );
	GameObjObserverManager::Save( csave );
	csave.End_Chunk();

	csave.Begin_Chunk( CHUNKID_BULLETS );
	BulletManager::Save( csave );
	csave.End_Chunk();

	csave.Begin_Chunk( CHUNKID_WEAPON_VIEW );
	WeaponViewClass::Save( csave );
	csave.End_Chunk();

	csave.Begin_Chunk( CHUNKID_DYNAMIC_BACKGROUND );
	BackgroundMgrClass::Save_Dynamic( csave );
	csave.End_Chunk();

	csave.Begin_Chunk( CHUNKID_DYNAMIC_WEATHER );
	WeatherMgrClass::Save_Dynamic( csave );
	csave.End_Chunk();

	csave.Begin_Chunk( CHUNKID_HUD );
	HUDClass::Save( csave );
	csave.End_Chunk();

	csave.Begin_Chunk( CHUNKID_SCREEN_FADE );
	ScreenFadeManager::Save( csave );
	csave.End_Chunk();

	return true;
}

bool	CombatSaveLoadClass::Load( ChunkLoadClass &cload )
{

	while (cload.Open_Chunk()) {
		switch(cload.Cur_Chunk_ID()) {

			case CHUNKID_GAMEOBJMANAGER:
				GameObjManager::Load( cload );
				break;

			case CHUNKID_COMBAT_GAME_MODE:
				CombatManager::Load( cload );
				break;

			case CHUNKID_SPAWNERS:
				SpawnManager::Load( cload );
				break;

			case CHUNKID_SCRIPTS:
				ScriptManager::Load( cload );
				break;

			case CHUNKID_PERSISTENT_GAME_OBJ_OBSERVERS:
				PersistentGameObjObserverManager::Load( cload );
				break;

			case CHUNKID_COVER:
				CoverManager::Load( cload );
				break;

			case CHUNKID_OBJECTIVES:
				ObjectiveManager::Load( cload );
				break;

			case CHUNKID_RADAR:
				RadarManager::Load( cload );
				break;
			
			case CHUNKID_GAME_OBJ_OBSERVERS:
				GameObjObserverManager::Load( cload );
				break;

			case CHUNKID_BULLETS:
				BulletManager::Load( cload );
				break;

			case CHUNKID_WEAPON_VIEW:
				WeaponViewClass::Load( cload );
				break;

			case CHUNKID_DYNAMIC_BACKGROUND:
				BackgroundMgrClass::Load_Dynamic( cload );
				break;

			case CHUNKID_DYNAMIC_WEATHER:
				WeatherMgrClass::Load_Dynamic( cload );
				break;

			case CHUNKID_HUD:
				HUDClass::Load( cload );
				break;

			case CHUNKID_SCREEN_FADE:
				ScreenFadeManager::Load( cload );
				break;

			default:
				Debug_Say(( "Unrecognized CombatSaveLoad chunkID\n" ));
				break;

		}
		cload.Close_Chunk();
	}

	SaveLoadSystemClass::Register_Post_Load_Callback(this);

	return true;
}


void	CombatSaveLoadClass::On_Post_Load(void) 
{
}
