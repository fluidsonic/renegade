#ifndef GDSKIRMISH_H
#define GDSKIRMISH_H

#include "gamedata.h"
#include "basecontroller.h"


class	cGameDataSkirmish : public cGameData {
public:

   cGameDataSkirmish (void);
   ~cGameDataSkirmish (void);

   cGameDataSkirmish & operator = (const cGameDataSkirmish &rhs);

	enum {MAX_CREDITS	= 999999};

	virtual bool	Is_Skirmish(void) const							{return true;}
	virtual cGameDataSkirmish *	As_Skirmish(void)				{return this;}
	static const WCHAR* Get_Static_Game_Name(void);
	virtual void	On_Game_Begin (void);
	virtual void	On_Game_End (void);
	virtual void	Soldier_Added (SoldierGameObj *soldier);
	virtual const WCHAR* Get_Game_Name(void)				const	{return this->Get_Static_Game_Name();}
   virtual GameTypeEnum	Get_Game_Type(void)				const	{return GAME_TYPE_SKIRMISH;}		
	virtual void	Think(void);
	virtual void	Load_From_Server_Config(void);
	virtual void	Save_To_Server_Config(void);
	//virtual bool	Is_Team_Game(void)						const	{return true;}
//	virtual int		Choose_Player_Type(cPlayer* player, int team_choice, bool is_grunt) {return Choose_Smallest_Team();}
	virtual bool	Is_Game_Over(void);
	virtual void	Show_Game_Settings_Limits(void);
	virtual bool	Is_Limited(void)							const	{return true;}
	virtual bool	Is_Editable_Teaming(void)				const {return true;}
	virtual bool	Is_Editable_Clan_Game(void)			const {return true;}
	virtual bool	Is_Editable_Friendly_Fire(void)		const {return true;}
	virtual void	Reset_Game(bool is_reloaded);
	int				Get_Starting_Credits(void)				const	{return StartingCredits;}
	void				Set_Starting_Credits(int credits);
	void				Show_My_Money(void);
	virtual	void	Get_Description(WideStringClass & description);

	cBoolean					BaseDestructionEndsGame;
	cBoolean					BeaconPlacementEndsGame;

private:
	void						Base_Destruction_Score_Tweaking(void);
	void						Filter_Soldiers(void);

	int						StartingCredits;
	BaseControllerClass	BaseGDI;
	BaseControllerClass	BaseNOD;
	bool						IsPlaying;
};

#endif	// GDSKIRMISH_H
