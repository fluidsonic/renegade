#ifndef __BIOEVENT_H__
#define __BIOEVENT_H__

#include "netevent.h"
#include "netclassids.h"

//-----------------------------------------------------------------------------
//
// A C->S mirrored event object for transmitting initial player bio data.
//
class	cBioEvent : public cNetEvent
{
public:
   cBioEvent(void);

	void						Init(int team_choice, unsigned long clanID);

	virtual void			Export_Creation(BitStreamClass &packet);
	virtual void			Import_Creation(BitStreamClass &packet);

	virtual uint32_t			Get_Network_Class_ID(void) const				{return NETCLASSID_BIOEVENT;}

private:

	virtual void			Act(void);

	int						SenderId;
	WideStringClass		Nickname;
	char						MapName[256];
	int TeamChoice;
	unsigned int ClanID;
};

//-----------------------------------------------------------------------------

#endif	// __BIOEVENT_H__

