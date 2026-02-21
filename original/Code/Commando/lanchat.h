//
// Filename:     lanchat.h
// Project:      Network.lib, for Commando
// Author:       Tom Spencer-Smith
// Date:         Dec 1998
// Description:  lan chat interface
//
//-----------------------------------------------------------------------------
#if defined(_MSV_VER)
#pragma once
#endif

#ifndef LANCHAT_H
#define LANCHAT_H

#include "win.h"
#include <winsock.h>

#include "chatshre.h"

class cPacket;

enum LanMessages {   
	LAN_MESSAGE_POSITION,
};

//-----------------------------------------------------------------------------
class cLanChat
{
	public:
      cLanChat(void);
      ~cLanChat(void);

	   void						Think(void);

      void						Lan_Packet_Handler(cPacket & packet);
		void						Go_To_Location(ChatLocationEnum location);

      void						Accept_Actions(void);
      void						Refusal_Actions(void);

      void						Load_Lan_Registry_Keys(void);
      void						Save_Lan_Registry_Keys(void);

		ChatLocationEnum		Get_Current_Location(void) {return CurrentLocation;}

      void						Init_Lan_Protocol_And_Socket(void);

	private:
      cLanChat(const cLanChat& rhs);             // Disallow copy (compile/link time)
      cLanChat& operator=(const cLanChat& rhs);  // Disallow assignment (compile/link time)

      void						Send_Position_Broadcast(void);
      void						Process_Position_Broadcast(cPacket & packet);

      SOCKET					Socket;
		SOCKADDR_IN				LocalAddress;
      DWORD						LastPositionBroadcastTimeMs;
      int						PositionBroadcastNumber;
		ChatLocationEnum		CurrentLocation;
		static const USHORT	LAN_BROADCAST_INTERVAL_MS;
		static const USHORT	LAN_PORT;
};

#endif // LANCHAT_H


