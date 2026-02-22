#pragma once

#include "global.h"

#include "networkobject.h"
#include "netclassids.h"
//#include "control.h"

//-----------------------------------------------------------------------------
//
// A C-S mirrored object to inform server of client framerate
//
class	CClientFps : public NetworkObjectClass
{
public:

	CClientFps();
	~CClientFps();

	void					Init(void);
	virtual uint32_t		Get_Network_Class_ID(void) const					{return NETCLASSID_CLIENTFPS;}
	virtual void		Delete(void)											{delete this;}

	void					Set_Fps(int fps);
	void					Act(void);

	virtual void		Export_Creation(BitStreamClass &packet);
	virtual void		Import_Creation(BitStreamClass &packet);

	virtual void		Export_Frequent(BitStreamClass &packet);
	virtual void		Import_Frequent(BitStreamClass &packet);

private:

	int					ClientId;
	BYTE					Fps;
};

//-----------------------------------------------------------------------------

extern CClientFps *		PClientFps;

//-----------------------------------------------------------------------------
