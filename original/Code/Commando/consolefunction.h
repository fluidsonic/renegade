#pragma once

#include "global.h"


#include "vector.h"

/*
** Console Function
*/
class ConsoleFunctionClass {
public:
	virtual	const char *	Get_Name( void )	= 0;
   virtual	const char *	Get_Alias( void )	{ return NULL; }//Get_Name(); }
	virtual	const char *	Get_Help( void ) = 0;
	virtual	void				Activate( const char * input ) = 0;
	static	void				Print( const char *format, ... );

	virtual ~ConsoleFunctionClass() {}
};

/*
** Console Function Manager
*/
class ConsoleFunctionManager {

public:
	static	void	Init( void );
	static	void	Shutdown( void );

	static	void	Help(	const char * function = NULL );
	static	int	Get_Function_Count(void);
	static	void	Next_Verbose_Help_Screen(void);
	static	void	Verbose_Help_File(void);
	static	void	Parse_Input( const char * input_string );

	static	bool	Get_Command_Suggestion(const char * input,const char * cur_suggestion,char * suggestion,char * help,int len);

	static	void	Print( const char *format, ... );

private:
	
	static	int									 Find_Function_Node(const char * name);
	static	ConsoleFunctionClass *			 Find_Command_Suggestion(const char * input, int index );
	static	void									 Sort_Function_List( void );

	static	DynamicVectorClass<ConsoleFunctionClass *>	FunctionList;	// list of all console functions
};
