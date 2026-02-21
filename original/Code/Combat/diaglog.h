#ifndef	DIAGLOG_H
#define	DIAGLOG_H

#ifndef	ALWAYS_H
	#include "always.h"
#endif

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

#endif	// DIAGLOG_H
