#pragma once

#include "global.h"


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
