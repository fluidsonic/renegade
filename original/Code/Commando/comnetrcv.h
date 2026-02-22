#pragma once

#include "global.h"

class cPacket;

/*
** There functions will be called when corresponding packet types are receicved
*/
class	CombatNetworkReceiver {

public:
	// This may not fit here, but...
	// Server Update function allows the Host to generate any state update packets it wishes.
	virtual bool Server_Update_Dynamic_Objects(bool is_urgent = false)	= 0;
	virtual void Server_Send_Delete_Notifications(void)						= 0;
	virtual bool Client_Update_Dynamic_Objects(bool is_urgent = false)	= 0;
};
