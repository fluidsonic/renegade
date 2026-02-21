#ifndef GDSINGLEPLAYER_H
#define GDSINGLEPLAYER_H

#include "gamedata.h"

class	cGameDataSinglePlayer : public cGameData {
	public:
      cGameDataSinglePlayer(void);
      ~cGameDataSinglePlayer(void);
      cGameDataSinglePlayer& operator=(const cGameDataSinglePlayer& rhs);

		bool	Is_Single_Player(void) const						{return true;}
		cGameDataSinglePlayer * As_Single_Player(void)		{return this;}

		static const WCHAR* Get_Static_Game_Name(void);

		virtual const WCHAR*	Get_Game_Name(void)					const {return this->Get_Static_Game_Name();}
      virtual GameTypeEnum	Get_Game_Type(void)					const	{return GAME_TYPE_SINGLE_PLAYER;}
		//virtual bool	Is_Team_Game(void)							const	{return true;}
		virtual bool	Is_Limited(void)								const	{return true;}
		virtual int		Choose_Player_Type(cPlayer* player, int team_choice, bool is_grunt);
		virtual	bool	Remember_Inventory( void )	const				{ return true; }

};

#endif	// GDSINGLEPLAYER_H





