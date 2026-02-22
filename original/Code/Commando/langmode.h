#ifndef LANGMODE_H
#define LANGMODE_H

#include "GameMode.h"

//
// Game Mode for LAN multiplayer.
// Manages nickname/side-preference persistence in the registry.
//
class LanGameModeClass : public GameModeClass
{
public:
	virtual const char *Name(void) { return "LAN"; }
	virtual void Init(void);
	virtual void Shutdown(void);
	virtual void Think(void) {}
	virtual void Render(void) {}

	static void Load_Lan_Registry_Keys(void);
	static void Save_Lan_Registry_Keys(void);
};

#endif // LANGMODE_H
