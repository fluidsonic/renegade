#pragma once

#include "global.h"


#include "simplevec.h"

#include "ffactory.h"

/*
**
*/
class	FileFactoryListClass : public FileFactoryClass {

public:
	FileFactoryListClass( void );
	~FileFactoryListClass( void );

	void	Add_FileFactory( FileFactoryClass * factory, const char * name );
	void	Remove_FileFactory( FileFactoryClass * factory );
	FileFactoryClass *Remove_FileFactory(void);

	void	Set_Search_Start( const char *name );
	void	Reset_Search_Start( void )					{ SearchStartIndex = 0; }

	void	Add_Temp_FileFactory( FileFactoryClass * factory );
	FileFactoryClass * Remove_Temp_FileFactory( void );
	FileFactoryClass * Peek_Temp_FileFactory( void ) { return TempFactory; }

	virtual FileClass * Get_File( char const *filename );
	virtual void Return_File( FileClass *file );

	static FileFactoryListClass * Get_Instance( void )		{ return Instance; }

	int						Get_Factory_Count (void) const	{ return FactoryList.Count (); }
	FileFactoryClass *	Get_Factory (int index)				{ return FactoryList[index]; }

private:

	FileFactoryClass * TempFactory;
	SimpleDynVecClass<FileFactoryClass *>	FactoryList;
	DynamicVectorClass<StringClass>			FactoryNameList;
	int												SearchStartIndex;

	static FileFactoryListClass * Instance;
};
