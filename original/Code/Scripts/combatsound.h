#pragma once

#include "global.h"


#ifndef	VECTOR3_H
	#include "vector3.h"
#endif

class		ScriptableGameObj;

/*
**
*/
typedef enum {
	SOUND_TYPE_NONE,
	SOUND_TYPE_OLD_WEAPON,
	SOUND_TYPE_FOOTSTEPS,
	SOUND_TYPE_VEHICLE,
	SOUND_TYPE_GUNSHOT,
	SOUND_TYPE_BULLET_HIT,	
	NUM_DEFINED_SOUND_TYPES,

	SOUND_TYPE_DESIGNER01	= 991,
	SOUND_TYPE_DESIGNER02,
	SOUND_TYPE_DESIGNER03,
	SOUND_TYPE_DESIGNER04,
	SOUND_TYPE_DESIGNER05,
	SOUND_TYPE_DESIGNER06,
	SOUND_TYPE_DESIGNER07,
	SOUND_TYPE_DESIGNER08,
	SOUND_TYPE_DESIGNER09,

} CombatSoundType;

/*
**
*/
class CombatSound {
public:
	Vector3				Position;
	CombatSoundType	Type;
	ScriptableGameObj * Creator;
};

/*
**
*/
class	CombatSoundManager {
public:
	static	void	Init( void );
	static	void	Shutdown( void );
};
