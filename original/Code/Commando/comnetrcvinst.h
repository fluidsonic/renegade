#pragma once

#include "global.h"

#include "comnetrcv.h"
#include "gamedata.h"

/*
** Our Network Packet Receiver
*/
class CombatNetworkReceiverInstanceClass : public	CombatNetworkReceiver {

public:
	// Update Object States
	virtual bool Server_Update_Dynamic_Objects(bool is_urgent = false);
	virtual void Server_Send_Delete_Notifications(void);
	virtual bool Client_Update_Dynamic_Objects(bool is_urgent = false);

	void	Print( const char *format, ... );
	void	Print( const Vector3 & color, const char *format, ... );
};
