#pragma once

#include "global.h"

#include "netevent.h"
#include "netclassids.h"
#include "wwstring.h"

#include "sctextobj.h"

//-----------------------------------------------------------------------------
//
// A C->S mirrored object for ingame text communications.
//
class	cCsTextObj : public cNetEvent
{
public:
   cCsTextObj(void);

	void						Init(WideStringClass & text, TextMessageEnum type, int sender_id, int recipient);

	virtual void			Export_Creation(BitStreamClass &packet);
	virtual void			Import_Creation(BitStreamClass &packet);

	virtual uint32_t			Get_Network_Class_ID(void) const				{return NETCLASSID_CSTEXTOBJ;}

private:

	virtual void			Act(void);

	int						SenderId;
	TextMessageEnum		Type;
	WideStringClass		Text;
	int						Recipient;
};

//-----------------------------------------------------------------------------
