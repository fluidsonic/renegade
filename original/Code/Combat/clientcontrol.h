#ifndef __CLIENTCONTROL_H__
#define __CLIENTCONTROL_H__

#include "networkobject.h"
#include "netclassids.h"
#include "control.h"

//-----------------------------------------------------------------------------
//
// A C-S mirrored object to represent client control and targeting data
//
class	CClientControl : public NetworkObjectClass
{
public:

	CClientControl();
	~CClientControl();

	void					Init(void);
	virtual uint32_t		Get_Network_Class_ID(void) const					{return NETCLASSID_CLIENTCONTROL;}
	virtual void		Delete(void)											{delete this;}

	void					Set_Update_Flag(int id);

	virtual void		Export_Creation(BitStreamClass &packet);
	virtual void		Import_Creation(BitStreamClass &packet);

	virtual void		Export_Frequent(BitStreamClass &packet);
	virtual void		Import_Frequent(BitStreamClass &packet);

private:

	int					ClientId;
	int					SmartObjId;
};

//-----------------------------------------------------------------------------

extern CClientControl *		PClientControl;

//-----------------------------------------------------------------------------

#endif	// __CLIENTCONTROL_H__
