#pragma once

#include "global.h"

#include "gamedata.h"
#include "basecontroller.h"

class	cGameDataCnc : public cGameData {
public:

	//////////////////////////////////////////////////////////////
	//	Public constructors/destructors
	//////////////////////////////////////////////////////////////
   cGameDataCnc (void);
   ~cGameDataCnc (void);

	//////////////////////////////////////////////////////////////
	//	Public operators
	//////////////////////////////////////////////////////////////
   cGameDataCnc & operator= (const cGameDataCnc &rhs);

	//////////////////////////////////////////////////////////////
	//	Public constants
	//////////////////////////////////////////////////////////////
	enum {MAX_CREDITS	= 999999};

	//////////////////////////////////////////////////////////////
	//	Public methods
	//////////////////////////////////////////////////////////////
	virtual bool	Is_Cnc(void)								const	{return true;}
	virtual cGameDataCnc * As_Cnc(void)								{return this;}

	static const WCHAR* Get_Static_Game_Name(void);

	virtual void	On_Game_Begin (void);
	virtual void	On_Game_End (void);
	virtual void	Soldier_Added (SoldierGameObj *soldier);

	virtual const WCHAR* Get_Game_Name(void)				const	{return this->Get_Static_Game_Name();}
   virtual GameTypeEnum	Get_Game_Type(void)				const	{return GAME_TYPE_CNC;}
	virtual void	Think(void);
	virtual void	Load_From_Server_Config(void);
	virtual void	Save_To_Server_Config(void);
	//virtual bool	Is_Team_Game(void)						const	{return true;}
//	virtual int		Choose_Player_Type(cPlayer* player, int team_choice, bool is_grunt) {return Choose_Player_Type(player, team_choice, is_grunt);}
	virtual bool	Is_Game_Over(void);
	virtual void	Export_Tier_2_Data(cPacket & packet);
	virtual void	Import_Tier_2_Data(cPacket & packet);
	virtual void	Show_Game_Settings_Limits(void);
	//virtual bool	Is_Limited(void)							const	{return true;}
	virtual bool	Is_Limited(void) const;
	//virtual bool	Is_Editable_Reload_Map(void)			const {return true;}
	//virtual bool	Is_Editable_Max_Players(void)			const {return true;}
	virtual bool	Is_Editable_Teaming(void)				const {return true;}
	virtual bool	Is_Editable_Clan_Game(void)			const {return true;}
	virtual bool	Is_Editable_Friendly_Fire(void)		const {return true;}
	virtual void	Reset_Game(bool is_reloaded);
	virtual bool	Is_Valid_Settings(WideStringClass& outMsg, bool check_as_server = false);
	virtual bool	Is_Gameplay_Permitted(void);

	int				Get_Starting_Credits(void)				const	{return StartingCredits;}
	void				Set_Starting_Credits(int credits);

	void				Show_My_Money(void);

	virtual	void	Get_Description(WideStringClass & description);

	cBoolean					BaseDestructionEndsGame;
	cBoolean					BeaconPlacementEndsGame;

private:
	void				Base_Destruction_Score_Tweaking(void);

	int	StartingCredits;

	BaseControllerClass	BaseGDI;
	BaseControllerClass	BaseNOD;
	bool						IsPlaying;
};
