#include "scripts.h"

DECLARE_SCRIPT(BMG_Test_Script, "" )
{
	void Created(GameObject* obj)
	{
		Commands->Debug_Message( "BMG Test Script\n" );
//		Commands->Start_Timer( obj, this, 5, 0 );

/*		ActionParamsStruct params;
		params.Set_Basic( this, 80, 0 );
		params.Set_Attack( Vector3( 0,0,10 ), 100, 15, 1 );
		params.AttackCheckBlocked = false;
		Commands->Action_Attack( obj, params );*/
	}

	void	Sound_Heard( GameObject * obj, const CombatSound & sound )
	{
		Commands->Debug_Message("Heard Sound %d\n", sound.Type );
	}

	void Timer_Expired( GameObject * obj, int timer_id )
	{
		Commands->Start_Timer( obj, this, 5, 0 );

		GameObject * star = Commands->Get_The_Star();
		if ( star ) {
			Commands->Clear_Weapons( star );
			Commands->Give_PowerUp(star, "POW_LaserRifle_Player");
			Commands->Select_Weapon(star, "Weapon_LaserRifle_Player" );
		}
	}

};

////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////                                   /////////////////////////////////////////
////////////////////  ALL THE REST ARE COMMENTED OUT   /////////////////////////////////////////
////////////////////                                   /////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////

