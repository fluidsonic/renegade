#ifndef	WEAPONVIEW_H
#define	WEAPONVIEW_H

#ifndef	ALWAYS_H
	#include "always.h"
#endif

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

#endif
