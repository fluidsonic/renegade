#pragma once

#include "global.h"


#define	DIAG_LOG( a ) 		DiagLogClass::Log_Timed a 

/*
**
*/
class	DiagLogClass {

public:
	static	void	Init( void );
	static	void	Shutdown( void );

	static	void	Log_Timed( const char * type, const char * format, ... );
};
