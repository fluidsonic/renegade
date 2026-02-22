#ifndef __SCTEXTOBJ_H__
#define __SCTEXTOBJ_H__

#include "netevent.h"
#include "netclassids.h"

enum TextMessageEnum
{
	TEXT_MESSAGE_PUBLIC,
	TEXT_MESSAGE_TEAM,
	TEXT_MESSAGE_PRIVATE,
};

enum 
{
	HOST_TEXT_SENDER = -1
};

//-----------------------------------------------------------------------------
//
// A S->C mirrored object for ingame text communications.
//
class	cScTextObj : public cNetEvent
{
public:
   cScTextObj(void);

	void						Init(const WideStringClass & text, TextMessageEnum type, bool is_host_admin_message, int sender_id, int recipient_id = -1);

	virtual void			Export_Creation(BitStreamClass &packet);
	virtual void			Import_Creation(BitStreamClass &packet);
	virtual uint32_t			Get_Network_Class_ID(void) const				{return NETCLASSID_SCTEXTOBJ;}

private:

	virtual void			Act(void);
	void						Set_Dirty_Bit_For_Team(DIRTY_BIT bit, int team);

	int						SenderId;
	int						RecipientId;
	TextMessageEnum		Type;
	WideStringClass		Text;
	bool						IsHostAdminMessage;
};

//-----------------------------------------------------------------------------

#endif	// __SCTEXTOBJ_H__
