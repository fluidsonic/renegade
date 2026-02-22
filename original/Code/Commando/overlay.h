#pragma once

#include "global.h"


#include "gamemode.h"

class	CameraClass;
class	SimpleSceneClass;
class	RenderObjClass;

#define		OVERLAY_SCENE		OverlayGameModeClass::Scene		
#define		OVERLAY_CAMERA		OverlayGameModeClass::Camera

/*
** Game Mode to display screen images
*/
class	OverlayGameModeClass : public GameModeClass {
public:
	virtual	const char *Name()	{ return "Overlay"; }	// the name of this mode
	virtual	void	Init();		 	// called when the mode is activated
	virtual	void 	Shutdown(); 	// called when the mode is deactivated
	virtual	void 	Think() {}		// called each time through the main loop
	virtual	void 	Render();		// called each time through the main loop

	static	SimpleSceneClass  * Scene;
	static	CameraClass			* Camera;
};

/*
** Game Mode to display W3D overlay files
*/
class	Overlay3DGameModeClass : public GameModeClass {
public:
	virtual	const char *Name()	{ return "Overlay3D"; }	// the name of this mode
	virtual	void	Init();		 	// called when the mode is activated
	virtual	void 	Shutdown(); 	// called when the mode is deactivated
	virtual	void 	Think();			// called each time through the main loop
	virtual	void 	Render();		// called each time through the main loop

	void 		Start_Intro( void );
	void 		Start_End_Screen( void );

private:

	void 	Release_Model( void );
	void 	Set_Model( const char * name, const char * anim_name );

	static	SimpleSceneClass  * Scene;
	static	CameraClass			* Camera;
	static	RenderObjClass		* Model;
	static	float					  ModelTimer;
	static	bool					  EndScreen;
};
