#pragma once
#ifndef _GAMESIDESERVERCONTROL_H
#define _GAMESIDESERVERCONTROL_H

#include <WWLib/wwstring.h>


#define SERVER_CONTROL_PORT_KEY		"ControlPort"
#define SERVER_CONTROL_PASSWORD_KEY	"ControlPassword"
#define SERVER_CONTROL_LOOPBACK_KEY	"ControlLoopbackOnly"
#define SERVER_CONTROL_IP_KEY			"ControlIP"

#define DEFAULT_SERVER_CONTROL_PORT 		63999
#define DEFAULT_SERVER_CONTROL_PASSWORD	"not_a_valid_password"	// Password removed per Security review requirements. LFeenanEA - 27th January 2025


/*
** This is the game side interface to the server control lib.
**
**
*/
class GameSideServerControlClass
{

	public:
		static void Init(void);
		static void Shutdown(void);
		static const char *App_Request_Callback(char *request);
		static void Print(char *text, ...);
		static void Send_Message(char *text, unsigned long ip, unsigned short port);
		static void Set_Welcome_Message(void);

	private:
		static bool Listening;
		static StringClass Response;

};





#endif //_GAMESIDESERVERCONTROL_H