#ifndef	SNIPER_H
#define	SNIPER_H

#ifndef	ALWAYS_H
	#include "always.h"
#endif

/*
**
*/
class	SniperHUDClass {

public:
	static	void	Init( void );
	static	void 	Shutdown( void );
	static	void 	Update( void );
	static	void 	Render( void );

private:
	static	void 	Build_Base( void );
};

#endif
