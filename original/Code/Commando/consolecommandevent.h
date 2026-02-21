#ifndef __CONSOLECOMMANDEVENT_H__
#define __CONSOLECOMMANDEVENT_H__

#include "netevent.h"
#include "netclassids.h"

//-----------------------------------------------------------------------------
//
// A S->C mirrored object to represent remote console commands
//
class	cConsoleCommandEvent : public cNetEvent
{
public:

   cConsoleCommandEvent(void);

	void						Init(LPCSTR command);

	virtual void			Export_Creation(BitStreamClass &packet);
	virtual void			Import_Creation(BitStreamClass &packet);
	virtual uint32			Get_Network_Class_ID(void) const				{return NETCLASSID_CONSOLECOMMANDEVENT;}

private:

	virtual void			Act(void);

	char						Command[500];
};

//-----------------------------------------------------------------------------

#endif	// __CONSOLECOMMANDEVENT_H__
