#include "scripts.h"
#include "dprint.h"


DECLARE_SCRIPT(Dr_Mobius_Script, "")
{
	GameObject *CurrentLeader;

	////////////////////////////////////////////////////////////////////
	//	Created
	////////////////////////////////////////////////////////////////////
	void Created (GameObject *game_obj)
	{
		CurrentLeader = NULL;
		Commands->Start_Timer (game_obj, this, 0.5F, 777);
		return ;
	}

	////////////////////////////////////////////////////////////////////
	//	Timer_Expired
	////////////////////////////////////////////////////////////////////
	void Timer_Expired (GameObject *game_obj, int timer_id)
	{
		if (timer_id == 777) {
			Commands->Innate_Disable (game_obj);
			
			Vector3 pos = Commands->Get_Position(game_obj);
			GameObject * p_leader = Commands->Find_Closest_Soldier(pos, 0.1f, 2.0f, true);
			if (p_leader != NULL && p_leader != CurrentLeader) {
				ActionParamsStruct params;
				params.Set_Basic(this, 100, 100);
				params.Set_Movement(p_leader, 1.0f, 1.0f);
				params.MoveFollow = true;
				Commands->Action_Goto(game_obj, params);
				CurrentLeader = p_leader;
			}

			Commands->Start_Timer (game_obj, this, 0.5F, 777);
		}
		return ;
	}
};

