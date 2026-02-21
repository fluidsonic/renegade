#include "gamemode.h"
#include "ww3d.h"
#include "miscutil.h"
#include "slist.h"
//#include "menu.h"
#include "textdisplay.h"
#include "thread.h"
#include <stdio.h>
#include "backgroundmgr.h"
#include "render2d.h"
#include "dazzle.h"
#include "combat.h"
#include "meshmdl.h"
#include "rinfo.h"
#include "objectives.h"
#include "messagewindow.h"
#include "dialogmgr.h"
#include "pscene.h"
#include "phys.h"
#include "ccamera.h"
#include "diagnostics.h"
#include "dx8wrapper.h"
#include "sortingrenderer.h"
#include "textureloader.h"
#include "binkmovie.h"
//#include "helptext.h"
#include "cnetwork.h"
#include "dx8rendererdebugger.h"
#include "consolemode.h"


/*
** The gamemode list
*/
SList<GameModeClass>	  	GameModeList;
Vector3						GameModeManager::BackgroundColor(0,0,0);

int GameMajorModeClass::NumActiveMajorModes = 0;
static unsigned _HiddenFrameCount=0;

/*
** make sure we only have 1 active majormode
*/
void GameMajorModeClass::Activate()
{
	if ( State == GAME_MODE_INACTIVE ) {
		NumActiveMajorModes++;
		assert ( NumActiveMajorModes == 1 );
	}
	GameModeClass::Activate();
}

void GameMajorModeClass::Deactivate()
{
	if ( !Is_Inactive() ) {
		NumActiveMajorModes--;
		assert ( NumActiveMajorModes == 0 );
	}
	GameModeClass::Deactivate();
}

/*
**
*/
void	GameModeManager::Destroy( GameModeClass *mode )
{
	assert( mode != NULL );

	mode->Deactivate();
	mode->Safely_Deactivate();
	Remove( mode );
	delete mode;
}


/*
**
*/
GameModeClass *GameModeManager::Add( GameModeClass *mode )
{
	GameModeList.Add_Tail( mode );
	return mode;
}

void	GameModeManager::Remove( GameModeClass *mode )
{
	GameModeList.Remove( mode );
}

int	GameModeManager::Count()
{
	return GameModeList.Get_Count();
}

/*
**
*/
void	GameModeManager::Destroy_All( void )
{
	while ( Count() ) {
		Destroy( GameModeList.Remove_Tail() );
	}
}

void GameModeManager::List_Active_Game_Modes(void)
{
	if (!Get_Text_Display()) {
		return;
	}

	Get_Text_Display()->Print_System("Active game modes:");

	for (SLNode<GameModeClass> * game_mode_node = GameModeList.Head();
		game_mode_node != NULL; game_mode_node = game_mode_node->Next()) {
		GameModeClass * p_mode = game_mode_node->Data();

		if (p_mode->Is_Active()) {
			Get_Text_Display()->Print_System( "  %s", p_mode->Name() );
			//Debug_Say((mode_str));
		}
	}
}



/*
** let all non inactive game modes think
*/
void	GameModeManager::Think( void )
{
	for (	SLNode<GameModeClass> *game_mode_node = GameModeList.Head();
			game_mode_node != NULL;
			game_mode_node = game_mode_node->Next()) {
		GameModeClass *mode = game_mode_node->Data();

		//if ( mode->Get_State() != GAME_MODE_INACTIVE &&
		//	  mode->Get_State() != GAME_MODE_INACTIVE_PENDING ) {
		//if ( mode->Get_State() != GAME_MODE_INACTIVE &&
		if ( !mode->Is_Inactive() ) {

//			char name[80];
//			sprintf( name, "Think - %s", mode->Name() );
			mode->Think();
		}

		mode->Safely_Deactivate(); // if required
	}
	BINKMovie::Update();
}

void GameModeManager::Safely_Deactivate(void)
{
	//
	// This method safely deactivates any inactive pending mode without
	// attempting a think
	//

	for (SLNode<GameModeClass> *game_mode_node = GameModeList.Head();
		game_mode_node != NULL; game_mode_node = game_mode_node->Next()) {

		GameModeClass * p_mode = game_mode_node->Data();
		p_mode->Safely_Deactivate(); // if required
	}
}

/*
** let all non inactive game modes draw
*/
void	GameModeManager::Render( void )
{

	if (!ConsoleBox.Is_Exclusive()) {

		bool clear=true;
		bool old_enable_draw=DX8Wrapper::_Is_Triangle_Draw_Enabled();
		bool old_enable_sorting_draw=SortingRendererClass::_Is_Triangle_Draw_Enabled();
		if (_HiddenFrameCount) {
			DX8Wrapper::_Enable_Triangle_Draw(false);
			SortingRendererClass::_Enable_Triangle_Draw(false);
			clear=false;
		}

		// Update the mesh debugger. This doesn't do anything at all unless the debugger is
		// enabled, so it is safe to always call it
		DX8RendererDebugger::Update();

		bool do_pscene = (COMBAT_SCENE != NULL) && !cNetwork::I_Am_Only_Server();
		if (!GameInFocus) do_pscene=false;	// Don't render the game scene if the applicationisn't active
		if (do_pscene) {

			//
			//	Don't pre-process the combat scene (does VIS and stuff) if
			// the game isn't active.  (This gives us a menu performance boost).
			//
			if (Find( "Combat" )->Is_Active()) {
				COMBAT_SCENE->Pre_Render_Processing(*COMBAT_CAMERA);
			}
		}

		{

			WW3D::Begin_Render (clear, clear, BackgroundMgrClass::Get_Clear_Color());
		}

		if (GameInFocus) {
			for (	SLNode<GameModeClass> *game_mode_node = GameModeList.Head();
					game_mode_node != NULL;
					game_mode_node = game_mode_node->Next()) {
				GameModeClass *mode = game_mode_node->Data();

				if ( mode->Get_State() != GAME_MODE_INACTIVE ) {
					mode->Render();
				}
			}
		}

		{
			if (CombatManager::Get_Message_Window () != NULL) {
				CombatManager::Get_Message_Window ()->Render();
			}
		}

		{
			ObjectiveManager::Render_Viewer();
		}

		{
			DialogMgrClass::Render();
		}

		{
			cDiagnostics::Render();
		}

		/*
		{
			cHelpText::Render();
		}
		*/

		// Only update the movie when the application is active
		if (GameInFocus) {
			BINKMovie::Render();
		}

		{
			WW3D::End_Render();
		}


		if (do_pscene) {
			COMBAT_SCENE->Post_Render_Processing();
		}

		{
			ThreadClass::Switch_Thread(); // This is important when working with multiple threads!
		}

		if (_HiddenFrameCount) {
			_HiddenFrameCount--;
			DX8Wrapper::_Enable_Triangle_Draw(old_enable_draw);
			SortingRendererClass::_Enable_Triangle_Draw(old_enable_sorting_draw);
			TextureLoader::Flush_Pending_Load_Tasks();
		}
	}
}

void GameModeManager::Hide_Render_Frames(unsigned frame_count)
{
	_HiddenFrameCount=frame_count;
}

/*
** find a registered game mode by name
*/
GameModeClass	*GameModeManager::Find( const char * name )
{
	for (	SLNode<GameModeClass> *game_mode_node = GameModeList.Head();
			game_mode_node != NULL;
			game_mode_node = game_mode_node->Next()) {
		GameModeClass *mode = game_mode_node->Data();

		if ( !stricmp( name, mode->Name() ) ) {
			return mode;
		}
	}
	return NULL;
}

//-----------------------------------------------------------------------------
void GameModeClass::Activate()
{
	if (State == GAME_MODE_INACTIVE) {
		Init();
		State = GAME_MODE_ACTIVE;
	}

	if (State == GAME_MODE_INACTIVE_PENDING) {
		State = GAME_MODE_ACTIVE;
	}
}

//-----------------------------------------------------------------------------
void GameModeClass::Deactivate()
{
	if (!Is_Inactive()) {
		State = GAME_MODE_INACTIVE_PENDING;
	}
}

//-----------------------------------------------------------------------------
void GameModeClass::Safely_Deactivate()
{
	if (State == GAME_MODE_INACTIVE_PENDING) {
		Shutdown();
		State = GAME_MODE_INACTIVE;
	}
}

//-----------------------------------------------------------------------------
void GameModeClass::Suspend()
{
	if (State == GAME_MODE_ACTIVE) {
		State = GAME_MODE_SUSPENDED;
	}
}

//-----------------------------------------------------------------------------
void GameModeClass::Resume()
{
	if (State == GAME_MODE_SUSPENDED) {
		State = GAME_MODE_ACTIVE;
	}
}