#if defined(_MSC_VER)
#pragma once
#endif

#ifndef SCREENFADEMANAGER_H
#define SCREENFADEMANAGER_H

#include "always.h"

class Vector3;
class	ChunkSaveClass;
class	ChunkLoadClass;


/**
** ScreenFadeManager
** This class renders letterbox polys and full-screen "tint" polys.  It should get to
** think once per frame and get rendered last.  
*/
class	ScreenFadeManager 
{
public:

	/*
	** Infrastructure
	*/
	static	void	Init();
	static	void 	Shutdown();
	static	void 	Think();
	static	void 	Render();

	static	bool	Save( ChunkSaveClass & csave );
	static	bool	Load( ChunkLoadClass & cload );

	/*
	** Interface for controlling the overlay effects
	*/
	static	void	Enable_Letterbox(bool onoff, float time = 1.0f);
	static	void	Set_Screen_Overlay_Color(const Vector3 & color,float time = 0.0f);
	static	void	Set_Screen_Overlay_Color(float r,float g, float b,float time = 0.0f);
	static	void	Set_Screen_Overlay_Opacity(float opacity,float time = 0.0f);

};


#endif //SCREENFADEMANAGER_H

