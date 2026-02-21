#if defined(_MSC_VER)
#pragma once
#endif

#ifndef __ORATOR_H
#define __ORATOR_H

#include "vector3.h"
#include "gameobjref.h"


////////////////////////////////////////////////////////////////
//	Forward declarations
////////////////////////////////////////////////////////////////
class ChunkSaveClass;
class ChunkLoadClass;
class PhysicalGameObj;
class ActiveConversationClass;


////////////////////////////////////////////////////////////////
//
//	OratorClass
//
////////////////////////////////////////////////////////////////
class OratorClass
{
public:

	////////////////////////////////////////////////////////////////
	//	Public flags
	////////////////////////////////////////////////////////////////
	typedef enum
	{
		FLAG_DONT_MOVE				= 0x00000001,
		FLAG_DONT_TURN_HEAD		= 0x00000002,
		FLAG_TEMP_DONT_FACE		= 0x00000004,
		FLAG_DONT_FACE				= 0x00000008,

	} FLAGS;
	
	////////////////////////////////////////////////////////////////
	//	Public constructors/destructors
	////////////////////////////////////////////////////////////////
	OratorClass (void);
	~OratorClass (void);

	////////////////////////////////////////////////////////////////
	//	Public operators
	////////////////////////////////////////////////////////////////
	bool	operator== (const OratorClass &src)		{ return (ID == src.ID); }
	bool	operator!= (const OratorClass &src)		{ return (ID != src.ID); }

	////////////////////////////////////////////////////////////////
	//	Public methods
	////////////////////////////////////////////////////////////////

	//
	//	Initialization
	//
	void						Initialize (PhysicalGameObj *game_obj);

	//
	//	Inline accessors
	//
	void						Set_Position (const Vector3 &pos)	{ Position = pos; }
	const Vector3 &		Get_Position (void) const				{ return Position; }

	void						Set_Has_Arrived (bool has_arrived)	{ HasArrived = has_arrived; }
	bool						Has_Arrived (void) const				{ return HasArrived; }

	PhysicalGameObj *		Get_Game_Obj (void) const;

	int						Get_Orator_Type (void) const			{ return OratorType; }
	void						Set_Orator_Type (int type)				{ OratorType = type; }

	bool						Is_Invisible (void) const				{ return IsInvisible; }
	void						Set_Is_Invisible (bool onoff)			{ IsInvisible = onoff; }

	void						Set_Look_At_Obj (int obj_id)			{ LookAtObjID = obj_id; }
	int						Get_Look_At_Obj (void) const			{ return LookAtObjID; }

	//
	//	Save/load methods
	//
	bool						Save (ChunkSaveClass &csave);
	bool						Load (ChunkLoadClass &cload);

	//
	//	Identification
	//
	int						Get_ID (void) const	{ return ID; }
	void						Set_ID (int id)		{ ID = id; }

	//
	//	Flags
	//
	void						Set_Flags (int flags);
	void						Set_Flag (int flag, bool onoff);
	bool						Get_Flag (int flag);

	//
	//	Conversation access
	//
	ActiveConversationClass *	Peek_Conversation (void) const										{ return Conversation; }
	void								Set_Conversation (ActiveConversationClass *conversation)		{ Conversation = conversation; }

private:


	////////////////////////////////////////////////////////////////
	//	Private methods
	////////////////////////////////////////////////////////////////	
	void						Load_Variables (ChunkLoadClass &cload);

	////////////////////////////////////////////////////////////////
	//	Private member data
	////////////////////////////////////////////////////////////////	
	ActiveConversationClass *	Conversation;
	GameObjReference				GameObj;
	Vector3							Position;
	bool								HasArrived;
	int								Flags;
	int								ID;
	int								OratorType;
	int								LookAtObjID;
	bool								IsInvisible;
};


#endif //__ORATOR_H
