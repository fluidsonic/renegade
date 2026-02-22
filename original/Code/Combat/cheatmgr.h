#pragma once

#include "global.h"

#include "gametype.h"

//////////////////////////////////////////////////////////////////////
//
//	CheatMgrClass
//
//////////////////////////////////////////////////////////////////////
class CheatMgrClass
{
public:

	///////////////////////////////////////////////////////////////////
	//	Public flags
	///////////////////////////////////////////////////////////////////
	enum
	{
		CHEAT_INVULNERABILITY	= 1,
		CHEAT_INFINITE_AMMO		= 2,
		CHEAT_ALL_WEAPONS		= 4,

		ALL_CHEATS				= 0x07,

	};

	///////////////////////////////////////////////////////////////////
	//	Public constructors/destructors
	///////////////////////////////////////////////////////////////////
	CheatMgrClass  (void);
	virtual ~CheatMgrClass (void);

	///////////////////////////////////////////////////////////////////
	//	Public methods
	///////////////////////////////////////////////////////////////////

	//
	//	Singleton access
	//
	static CheatMgrClass *	Get_Instance (void)		{ return _TheInstance; }

	//
	//	Cheat control
	//
	virtual void	Enable_Cheat (int cheat, bool onoff)	{ if (onoff) Flags |= cheat; else Flags &= (~cheat); HistoryFlags |= Flags;  }
	virtual bool	Is_Cheat_Enabled (int /*cheat*/) const	{ return false; }
	virtual bool	Is_Cheat_Set (int /*cheat*/) const		{ return false; }

	virtual bool	Was_Cheat_Used (int /*cheat*/) const	{ return false; }
	virtual void	Update_History (void)						{ HistoryFlags |= Flags; }
	virtual void	Reset_History (void)							{ HistoryFlags = Flags; }
	virtual int		Get_History (void) const					{ return HistoryFlags; }
	virtual void	Set_History (int flags)						{ HistoryFlags = flags; }

	//
	//	Notifications
	//
	virtual void	Apply_Cheats (void) {}
	
protected:
	
	///////////////////////////////////////////////////////////////////
	//	Protected methods
	///////////////////////////////////////////////////////////////////
	
	///////////////////////////////////////////////////////////////////
	//	Protected member data
	///////////////////////////////////////////////////////////////////
	int	Flags;
	int	HistoryFlags;

	//
	//	Static member data
	//
	static CheatMgrClass *_TheInstance;
};
