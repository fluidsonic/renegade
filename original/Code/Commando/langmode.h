#if defined(_MSV_VER)
#pragma once
#endif

#ifndef LANGMODE_H
#define LANGMODE_H

#include "GameMode.h"
#include "LanChat.h"

#define PLC LanGameModeClass::Get_Lan_Interface()

// 
// Game Mode to do LAN interface
//
class	LanGameModeClass :
		public GameModeClass
{
	public:
		static cLanChat * Get_Lan_Interface(void);

		virtual	const char *Name(void)	{ return "LAN"; }	// the name of this mode
		virtual	void	Init(void);		 	// called when the mode is activated
		virtual	void 	Shutdown(void); 	// called when the mode is deactivated
		virtual	void 	Think(void);		// called each time through the main loop
		virtual	void 	Render(void) {};		// called each time through the main loop

	private:
		static cLanChat * PLanChat;
};

#endif	//	LANGMODE_H
