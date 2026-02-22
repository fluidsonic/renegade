#pragma once

#include "global.h"

#include "gamemode.h"

class SmartGameObj;
class Render2DTextClass;
class Render2DSentenceClass;
class Font3DInstanceClass;
class NetworkObjectClass;
class WideStringClass;

enum PlayerlistFormatEnum
{
	PLAYERLIST_FORMAT_TINY,
	PLAYERLIST_FORMAT_MEDIUM,
	PLAYERLIST_FORMAT_FULL,
};

//-----------------------------------------------------------------------------
class	MultiHUDClass {
public:
	static void		Init(void);
	static void		Shutdown(void);
	static void		Think(void);
	static void		Render(void);

	static void		Toggle();
	static bool		Is_On();
	static void		Render_Text(WideStringClass & text, float x, float y, uint32_t color = 0xFFFFFFFF);
   static void		Show_Player_Names(void);

	static void		Set_Bottom_Text_Y_Pos(float y)				{BottomTextYPos = y;}
	static float	Get_Bottom_Text_Y_Pos(void)					{return BottomTextYPos;}

	/*
	static void		Set_Verbose_Lists(bool flag)					{VerboseLists = flag;}
	static bool		Get_Verbose_Lists(void)							{return VerboseLists;}
	static bool		Toggle_Verbose_Lists(void)						{VerboseLists = !VerboseLists; return VerboseLists;}
	*/
	static void							Set_Playerlist_Format(PlayerlistFormatEnum playerlist_format)				{PlayerlistFormat = playerlist_format;}
	static PlayerlistFormatEnum	Get_Playerlist_Format(void)															{return PlayerlistFormat;}
	//static void							Next_Playerlist_Format(void)															{PlayerlistFormat++;}
	static void							Next_Playerlist_Format(void);

private:
	static const float				MAX_OVERLAY_DISTANCE_M;
   static const float				Y_INCREMENT_FACTOR;
	static float						BottomTextYPos;
	static Render2DSentenceClass*	NameRenderer;
	//static bool							VerboseLists;
	static bool							IsOn;
	static PlayerlistFormatEnum	PlayerlistFormat;
};

//-----------------------------------------------------------------------------
