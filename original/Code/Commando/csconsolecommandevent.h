#pragma once

#include "global.h"

#include "netevent.h"
#include "netclassids.h"

//-----------------------------------------------------------------------------
//
// A C->S mirrored object to act a console command on the server
//
class	cCsConsoleCommandEvent : public cNetEvent
{
public:

   cCsConsoleCommandEvent(void);

	void						Init(LPCSTR command);

	virtual void			Export_Creation(BitStreamClass &packet);
	virtual void			Import_Creation(BitStreamClass &packet);
	virtual uint32_t			Get_Network_Class_ID(void) const				{return NETCLASSID_CSCONSOLECOMMANDEVENT;}

private:

	virtual void			Act(void);

	char						Command[500];
};

//-----------------------------------------------------------------------------
