#pragma once

#include "global.h"


class	ChunkSaveClass;
class	ChunkLoadClass;
class	AmmoDefinitionClass;
class	Vector3;
class	ArmedGameObj;
class DamageableGameObj;

/*
** BulletManager
*/
class BulletManager {

public:
	static	void	Init( void );
	static	void	Shutdown( void );
	static	void	Update( void );

	static	bool	Save( ChunkSaveClass &csave );
	static	bool	Load( ChunkLoadClass &cload );

	static	void	Create_Bullet( const AmmoDefinitionClass * def, const Vector3 & position, 
					const Vector3 & velocity, const ArmedGameObj * owner, float progress_time, const Vector3 & target, DamageableGameObj * target_object = NULL );
};
