#include "global.h"
#include "multihud.h"

#include <stdio.h>

#include "soldier.h"
#include "assets.h"
#include "playermanager.h"
#include "teammanager.h"
#include "cnetwork.h"
#include "timemgr.h"
#include "miscutil.h"
#include "gamedata.h"
#include "combat.h"
#include "ccamera.h"
#include "gameobjmanager.h"
#include "smartgameobj.h"
#include "vehicle.h"
#include "physcoltest.h"
#include "pscene.h"
#include "phys.h"
#include "damage.h"
#include "movephys.h"
#include "humanphys.h"
#include "weapons.h"
#include "useroptions.h"
#include "devoptions.h"
#include "overlay.h"
#include "spawn.h"
#include "string_ids.h"
#include "translatedb.h"
#include "texture.h"
#include "render2d.h"
#include "render2dsentence.h"
#include "networkobjectmgr.h"
#include "networkobject.h"
#include "building.h"
#include "powerup.h"
#include "staticnetworkobject.h"
#include "widestring.h"
#include "font3d.h"
#include "apppackettypes.h"
#include "bandwidthgraph.h"
#include "priority.h"
#include "consolemode.h"
#include "stylemgr.h"
#include "demosupport.h"

//
// MultiHUDClass statics
//
const float					MultiHUDClass::MAX_OVERLAY_DISTANCE_M		= 50;
const float					MultiHUDClass::Y_INCREMENT_FACTOR			= 1.2f;

Render2DSentenceClass* MultiHUDClass::NameRenderer					= NULL;
float							MultiHUDClass::BottomTextYPos					= 0;
//bool							MultiHUDClass::VerboseLists					= false;
PlayerlistFormatEnum		MultiHUDClass::PlayerlistFormat				= PLAYERLIST_FORMAT_TINY;
bool							MultiHUDClass::IsOn								= false;

//-----------------------------------------------------------------------------
void MultiHUDClass::Init(void)
{
	if (!ConsoleBox.Is_Exclusive()) {

		NameRenderer = new Render2DSentenceClass;
		StyleMgrClass::Assign_Font(NameRenderer, StyleMgrClass::FONT_INGAME_TXT);
		NameRenderer->Set_Mono_Spaced(true);

		IsOn = true;
	}
}

//-----------------------------------------------------------------------------
void MultiHUDClass::Shutdown(void)
{

	if (NameRenderer) {
		delete NameRenderer;
		NameRenderer = NULL;
	}

	IsOn = false;
}

//-----------------------------------------------------------------------------
void MultiHUDClass::Render_Text(WideStringClass & text, float x, float y, uint32_t color)
{
	if (NameRenderer) {
		//
		// Text comes out blurry with the new text system if the text position
		// is non-integral.
		//

		x = cMathUtil::Round(x);
		y = cMathUtil::Round(y);

		NameRenderer->Set_Location(Vector2(x, y));
		NameRenderer->Build_Sentence(text);
		NameRenderer->Draw_Sentence(color);
	}
}

//-----------------------------------------------------------------------------
void MultiHUDClass::Toggle(void)
{
	if (IsOn) {
		Shutdown();
	} else {
		Init();
	}
}

//-----------------------------------------------------------------------------
bool MultiHUDClass::Is_On(void)
{
	return IsOn;
}

//-----------------------------------------------------------------------------
void MultiHUDClass::Render(void)
{

	if (NameRenderer != NULL) {
		NameRenderer->Render();
	}
}

//-----------------------------------------------------------------------------
//
// Simple but presumably hugely inefficient code follows
//
void MultiHUDClass::Show_Player_Names(void)
{
	if (NameRenderer == NULL) {
		return;
	}

   if (GameModeManager::Find("Menu")->Is_Active() ||
		COMBAT_CAMERA == NULL ||
		!cNetwork::I_Am_Client() ||
      cUserOptions::ShowNamesOnSoldier.Is_False()) {

		return;
   }

   //
   // Project name of each player onto his commando
	//

   for (
		SLNode<SmartGameObj> * smart_objnode = GameObjManager::Get_Smart_Game_Obj_List()->Head();
		smart_objnode;
		smart_objnode = smart_objnode->Next()) {

		SmartGameObj * p_smart_obj = smart_objnode->Data();

		//
		// We will eliminate candidates with sequential tests. Want to
		// avoid deep conditional nesting here
		//

		if (p_smart_obj->Is_Delete_Pending()) {
			continue;
      }

      //
		// We are only interested in soldiers
		//
		if (p_smart_obj->As_SoldierGameObj() == NULL) {
			continue;
      }

      //
		// ... living soldiers
		//
      float health = p_smart_obj->Get_Defense_Object()->Get_Health();
		if (health < WWMATH_EPSILON) {
			continue;
		}

		//

		// ... with player objects
		//
		cPlayer * p_player = NULL;
      if (p_smart_obj->Has_Player()) {
         p_player = cPlayerManager::Find_Player(p_smart_obj->Get_Control_Owner());
      }
		if (p_player == NULL) {
			continue;
		}

      //
		// Is this guy inside our frustrum?
		//
		Vector3 text_position_3d;
		p_smart_obj->Get_Position(&text_position_3d);
      text_position_3d.Z += 1.3f;

	   Vector3 text_position_2d;
      if (COMBAT_CAMERA->Project(text_position_2d, text_position_3d) != CameraClass::INSIDE_FRUSTUM) {
			continue;
		}

		//
		// Take stealthing into account.
		//
		bool is_stealthed = false;
		SoldierGameObj * p_soldier = p_smart_obj->As_SoldierGameObj();
		VehicleGameObj * p_vehicle = NULL;
		if (p_soldier->Is_Stealthed()) {
			is_stealthed = true;
		} else {
			p_vehicle = p_soldier->Get_Vehicle();
			if (p_vehicle != NULL && p_vehicle->Is_Stealthed()) {
				is_stealthed = true;
			}
		}

		if (COMBAT_STAR != NULL && p_soldier->Is_Teammate(COMBAT_STAR)) {
			//
			// Don't skip for teammates...
			//
		} else if (is_stealthed) {
			continue;
		}

		//
		// adjust x,y from range -1 to 1
		//
		text_position_2d.X = (text_position_2d.X + 1) * Render2DClass::Get_Screen_Resolution().Width() / 2;
		text_position_2d.Y = (1 - text_position_2d.Y) * Render2DClass::Get_Screen_Resolution().Height() / 2;

      //
		// Is this guy nearby?
		//
		Vector3 ray_start = COMBAT_CAMERA->Get_Transform().Get_Translation();
		Vector3 delta = text_position_3d - ray_start;
		float apparent_distance = delta.Length();
		if (COMBAT_CAMERA->Is_Star_Sniping()) {
			//
			// adjust the apparent distance
			//
			float zoom = COMBAT_CAMERA->Get_Profile_Zoom();
			apparent_distance /= zoom;
		}
		if (apparent_distance > MAX_OVERLAY_DISTANCE_M) {
			continue;
		}

		if (CombatManager::Is_First_Person() && COMBAT_STAR != NULL) {
			//
			// Ignore collision between ray and own commando
			//
			COMBAT_STAR->Peek_Physical_Object()->Inc_Ignore_Counter();
		}

      //
      // Ok! This guy deserves a ray-cast for visibility...
      //
		Vector3 ray_end = ray_start + delta * 1.1f;
		LineSegClass ray(ray_start, ray_end);
		CastResultStruct res;
		PhysRayCollisionTestClass raytest(ray, &res,
			BULLET_COLLISION_GROUP, COLLISION_TYPE_PHYSICAL);

		{
			COMBAT_SCENE->Cast_Ray(raytest);
		}

		if (CombatManager::Is_First_Person() && COMBAT_STAR != NULL) {
			COMBAT_STAR->Peek_Physical_Object()->Dec_Ignore_Counter();
		}

		if (raytest.CollidedPhysObj == NULL) {
			//
			// I think this means that the ray collided with the terrain.
			//
			continue;
		}

		if (raytest.CollidedPhysObj->Get_Observer() == NULL) {
			continue;
		} else {
			PhysicalGameObj * p_blocker = ((CombatPhysObserverClass *)raytest.CollidedPhysObj->Get_Observer())->As_PhysicalGameObj();
	      //if (p_blocker != p_smart_obj) {
	      if ((p_blocker == NULL) ||
				 ((p_blocker != p_smart_obj) && (p_blocker != p_vehicle))) {
			   //
				// The ray failed to collide with the soldier in question
				//
	         continue;
		   }
		}

		//
		// OK, we should go ahead and draw the name
		//

      //
      // Show name text
      //
      if (cUserOptions::ShowNamesOnSoldier.Is_True()) {

			WideStringClass text(0,true);
			text += p_player->Get_Name();

			//
			// MVP carries over into next game
			//
			WideStringClass mvp_name = The_Game()->Get_Mvp_Name();
			if (!p_player->Get_Name().Compare_No_Case(mvp_name.Peek_Buffer())) {
				text += u"\n";
				text += TRANSLATE(IDS_MP_MVP);
				if (The_Game()->Get_Mvp_Count() > 1) {
					WideStringClass consecutives_text;
					consecutives_text.Format(u" * %d", The_Game()->Get_Mvp_Count());
					text += consecutives_text;
				}
			}

			Vector2 textExtent = NameRenderer->Get_Text_Extents(text);
			float x = text_position_2d.X - textExtent.X / 2.0f;
			float y = text_position_2d.Y;

			float scale = (MAX_OVERLAY_DISTANCE_M - apparent_distance) / MAX_OVERLAY_DISTANCE_M;

			int color = (((int)(scale * 255) & 0xFF) << 24) |
				(VRGB_TO_INT32(p_player->Get_Color()) & 0x00FFFFFF);

			Render_Text(text, x, y, color);
      }
   }
}

//-----------------------------------------------------------------------------
void MultiHUDClass::Think(void)
{

	if (NameRenderer == NULL) {
		return;
	}

	NameRenderer->Reset();

	DEMO_SECURITY_CHECK;

	if (!GameModeManager::Find("Combat")->Is_Active() ||
		  GameModeManager::Find("Menu")->Is_Active()) {
		return;
	}

   Show_Player_Names();

//	BottomTextYPos = Render2DClass::Get_Screen_Resolution().Bottom - 15;

	//
	// From here down its all diagnostics
	//

}

//-----------------------------------------------------------------------------
void MultiHUDClass::Next_Playerlist_Format(void)
{
	switch (PlayerlistFormat) {
		case PLAYERLIST_FORMAT_TINY:
			PlayerlistFormat = PLAYERLIST_FORMAT_MEDIUM;
			break;

		case PLAYERLIST_FORMAT_MEDIUM:
			PlayerlistFormat = PLAYERLIST_FORMAT_FULL;
			break;

		case PLAYERLIST_FORMAT_FULL:
			PlayerlistFormat = PLAYERLIST_FORMAT_TINY;
			break;

		default:
			break;
	}
}
