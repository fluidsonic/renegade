#ifndef	BONES_H
#define	BONES_H

#ifndef	ALWAYS_H
	#include "always.h"
#endif

/*
** BONES MANAGER
*/
class BonesManager {

public:
	static void Init( void );
	static void Shutdown( void );

	static const char *	Get_Bone_Screen_Name( const char * bone_name );
	static float			Get_Bone_Damage_Scale( const char * bone_name );

};

#endif

