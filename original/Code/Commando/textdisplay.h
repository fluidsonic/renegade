#ifndef	TEXTDISPLAY_H
#define	TEXTDISPLAY_H

#ifndef	ALWAYS_H
	#include "always.h"
#endif

#ifndef	GAMEMODE_H
	#include "gamemode.h"
#endif

#ifndef	SLIST_H
	#include "slist.h"
#endif

#ifndef	WWSTRING_H
	#include "wwstring.h"
#endif

#ifndef	WIDESTRING_H
	#include "widestring.h"
#endif


#ifndef VECTOR2_H
	#include "vector2.h"
#endif

#ifndef VECTOR4_H
	#include "vector4.h"
#endif

class	Font3DInstanceClass;
class	Render2DTextClass;

/*
** TextDisplayLine
*/
class	TextDisplayLine {

public:
	TextDisplayLine( const WideStringClass & text, unsigned long color );

	float	Update_Time( float seconds )	{ Time += seconds; return Time; }
	int	Get_Line_Count( void )			{ return LineCount; }

	WideStringClass	Text;
	unsigned long		Color;
	float					Time;
	int					LineCount;
};

/*
** TextDisplayGameMode - Game (Sub) Mode to display text
*/
#define	Get_Text_Display()		TextDisplayGameModeClass::Get_Instance()

class	TextDisplayGameModeClass : public GameModeClass {
public:
	virtual	const char *Name()	{ return "TextDisplay"; }	// the name of this mode
	virtual	void	Init();		 	// called when the mode is activated
	virtual	void 	Shutdown(); 	// called when the mode is deactivated
	virtual	void 	Think();			// called each time through the main loop
	virtual	void 	Render();		// called each time through the main loop

	static	TextDisplayGameModeClass * Get_Instance() { return Instance; }

	void 		Flush( void );

	/*
	** Scrolling Lines
	*/
	void		Set_Max_Scroll_Lines( int count )				{ MaxScrollLines = count; }
	int		Get_Max_Scroll_Lines( void )						{ return MaxScrollLines; }
	void		Set_Scroll_Lines_Persist_Time( float time )	{ ScrollLinesPersistTime = time; }
	float		Get_Scroll_Lines_Persist_Time( void )			{ return ScrollLinesPersistTime; }

	void		Print( const char * text, const Vector4 & color = Vector4( 1,1,1,1 ) );
	void		Print( const WideStringClass & text, const Vector4 & color = Vector4( 1,1,1,1 ) );
	void		Print( const char * text, const Vector3 & color = Vector3( 1,1,1 ) );
	void		Print( const WideStringClass & text, const Vector3 & color = Vector3( 1,1,1 ) );

	void		Print_System( const char * text, ... );
	void		Print_System( const WideStringClass & string );
	void		Print_Informational( const char * text, ... );
	void		Print_Informational( const WideStringClass & string );

	void		Set_Input_Text( const char * text )
	{
		TextChanged|=InputText!=text;
		InputText = text;
	}
	void		Set_Help_Text( const char * text )
	{
		TextChanged|=HelpText!=text;
		HelpText = text;
	}
	void		Set_Verbose_Help_Text( const char * text )
	{
		VerboseTextChanged|=VerboseHelpText!=text;
		VerboseHelpText = text;
	}
	void		Display_Vis_Warning( bool on )					{ DisplayVisWarning = on; }

private:
	Font3DInstanceClass	*	Font;
	Font3DInstanceClass	*	MonoFont;
	Render2DTextClass	*		Display;
	Render2DTextClass	*		VerboseDisplay;
	Render2DTextClass	*		StatisticsDisplay;
	DynamicVectorClass<WideStringClass> RendererLines;
	DynamicVectorClass<unsigned long> RendererColors;
	float							DisplayY;

	SList<TextDisplayLine>	ScrollLines;
	StringClass					InputText;
	StringClass					HelpText;
	StringClass					VerboseHelpText;
	bool							TextChanged;
	bool							VerboseTextChanged;
	bool							DisplayVisWarning;

	int							MaxScrollLines;
	float							ScrollLinesPersistTime;
	float							VerticalScroll;

	static TextDisplayGameModeClass * Instance;

	/*
	*/
	int							Count_Scroll_Lines( void );
	void							Load_Registry_Keys(void);
	void							Save_Registry_Keys(void);
};

/*
** Statistics Display
*/
class	StatisticsDisplayManager {
public:
	static	void	Render( Render2DTextClass * renderer );
	static	void	Set_Display( const char * title );
	static	bool	Is_Current_Display( const char* title); // Returns true if "title" is currently active
	static	void	Set_Stat( const char * title, const char * text, unsigned long color = 0xffffffff, const Vector2& location = Vector2( 0, -240 ) );
};

#endif
