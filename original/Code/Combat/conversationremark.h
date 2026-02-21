
#ifndef __CONVERSATIONREMARK_H
#define __CONVERSATIONREMARK_H

#include "wwstring.h"
#include "simplevec.h"
#include "refcount.h"
#include "orator.h"
#include "soldier.h"

////////////////////////////////////////////////////////////////
//	Forward declarations
////////////////////////////////////////////////////////////////
class ChunkSaveClass;
class ChunkLoadClass;

////////////////////////////////////////////////////////////////
//
//	ConversationRemarkClass
//
////////////////////////////////////////////////////////////////
class ConversationRemarkClass
{
public:
	
	////////////////////////////////////////////////////////////////
	//	Public constructors/destructors
	////////////////////////////////////////////////////////////////
	ConversationRemarkClass (void);
	ConversationRemarkClass (const ConversationRemarkClass &src);
	virtual ~ConversationRemarkClass (void);

	////////////////////////////////////////////////////////////////
	//	Public operators
	////////////////////////////////////////////////////////////////
	const ConversationRemarkClass &	operator= (const ConversationRemarkClass &src);
	bool	operator== (const ConversationRemarkClass &src)		{ return (TextID == src.TextID) && (OratorID == src.OratorID); }
	bool	operator!= (const ConversationRemarkClass &src)		{ return (TextID != src.TextID) || (OratorID != src.OratorID); }

	////////////////////////////////////////////////////////////////
	//	Public methods
	////////////////////////////////////////////////////////////////

	//
	//	Save/Load methods
	//
	bool				Save (ChunkSaveClass &csave);
	bool				Load (ChunkLoadClass &cload);

	//
	//	Accessors
	//
	int				Get_Orator_ID (void) const			{ return OratorID; }
	void				Set_Orator_ID (int id)				{ OratorID = id; }

	int				Get_Text_ID (void) const			{ return TextID; }
	void				Set_Text_ID (int id)					{ TextID = id; }

	const StringClass &	Get_Animation_Name (void) const				{ return AnimationName; }
	void						Set_Animation_Name (const char *name)		{ AnimationName = name; }

protected:

	////////////////////////////////////////////////////////////////
	//	Protected methods
	////////////////////////////////////////////////////////////////
	void				Load_Variables (ChunkLoadClass &cload);

	////////////////////////////////////////////////////////////////
	//	Protected data types
	////////////////////////////////////////////////////////////////
	int				OratorID;
	int				TextID;
	StringClass		AnimationName;
};

#endif //__CONVERSATIONREMARK_H
