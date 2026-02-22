#pragma once

#include "global.h"


#ifndef	VECTOR3_H
	#include "vector3.h"
#endif

class	ChunkSaveClass;
class	ChunkLoadClass;

/*
**
*/
class	WeaponViewClass {

public:
	static	void	Init();
	static	void 	Shutdown();
	static	void 	Enable( bool enable );
	static	void 	Think();
	static	void 	Reset();

	static	bool	Save( ChunkSaveClass &csave );
	static	bool	Load( ChunkLoadClass &cload );

	static	Vector3	Get_Muzzle_Pos();
};
