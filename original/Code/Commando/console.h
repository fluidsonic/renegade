#ifndef	CONSOLE_H
#define	CONSOLE_H

#ifndef	ALWAYS_H
	#include "always.h"
#endif

#ifndef	GAMEMODE_H
	#include "gamemode.h"
#endif

#ifndef	BITTYPE_H
	#include "bittype.h"
#endif


#define	Get_Console()		ConsoleGameModeClass::Get_Instance()

/*
** Game (Sub) Mode to display console & fps
*/
class	ConsoleGameModeClass : public GameModeClass {
public:

	virtual	const char *Name()	{ return "Console"; }	// the name of this mode
	virtual	void	Init();		 	// called when the mode is activated
	virtual	void 	Shutdown(); 	// called when the mode is deactivated
	virtual	void 	Think();			// called each time through the main loop
	virtual	void 	Render() {}		// called each time through the main loop

	void 		Parse_Input( char * string );
	//void 		Toggle_FPS( void )	{ FPSActive = !FPSActive; }
	bool		Is_FPS_Active( void )	{ return FPSActive; }
	void		Set_FPS_Active( bool onoff )	{ FPSActive = onoff; }

	void		Toggle_Player_Position( void )							{ ShowPlayerPosition = !ShowPlayerPosition; }
	void		Set_Player_Position( Vector3 & pos, float facing )	{ PlayerPosition = pos; PlayerFacing = facing; }

	static	ConsoleGameModeClass * Get_Instance() { return Instance; }

	static void Load_Registry_Keys(void);
	static void Save_Registry_Keys(void);

private:

	void		Clear_Suggestion(void);
	void		Accept_Suggestion(char * cmd);
	void		Update_Suggestion(char * cmd,bool go_to_next);

	enum {
		MAX_INPUT_LINE_LENGTH		= 100,
		MAX_DRAWER_CHARS				= 4000,
	};

	char							InputLine[MAX_INPUT_LINE_LENGTH];
	char							HelpLine[MAX_INPUT_LINE_LENGTH];
	char							Suggestion[MAX_INPUT_LINE_LENGTH];
	bool							InputActive;

	bool	FPSActive;
	int	FPSFrames;
	float FPSTime;
	float FPSLastTime;
	float FPS;

	bool		ShowPlayerPosition;
	Vector3	PlayerPosition;
	float		PlayerFacing;

	bool	PerformanceSamplingActive;
   DWORD ConsoleInputType;
   int   PromptLength;
   static const float LeftMargin;

	static ConsoleGameModeClass * Instance;
};

#endif
