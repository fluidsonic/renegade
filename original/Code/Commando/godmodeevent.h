#ifndef __GODMODEEVENT_H__
#define __GODMODEEVENT_H__

#include "netevent.h"
#include "netclassids.h"

//-----------------------------------------------------------------------------
//
// A C->S mirrored event object to inform server about changes in our god mode status
//
class	cGodModeEvent : public cNetEvent
{
public:
   cGodModeEvent(void);

	void						Init(StringClass & password);

	virtual void			Export_Creation(BitStreamClass &packet);
	virtual void			Import_Creation(BitStreamClass &packet);

	virtual uint32			Get_Network_Class_ID(void) const				{return NETCLASSID_GODMODEEVENT;}

private:

	virtual void			Act(void);

	int						SenderId;
	StringClass				Password;
};

//-----------------------------------------------------------------------------

#endif	// __GODMODEEVENT_H__
