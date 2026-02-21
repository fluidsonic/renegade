#include "ffactorylist.h"
#include "wwfile.h"


FileFactoryListClass * FileFactoryListClass::Instance = NULL;

/*
**
*/
FileFactoryListClass::FileFactoryListClass( void ) :
	SearchStartIndex( 0 ),
	TempFactory( NULL )
{
	Instance = this;
}

FileFactoryListClass::~FileFactoryListClass( void )
{
	Instance = NULL;
}

/*
**
*/
void	FileFactoryListClass::Add_FileFactory( FileFactoryClass * factory, const char *name )
{
	FactoryList.Add( factory );
	FactoryNameList.Add( name );
	Reset_Search_Start ();
}


/*
**
*/
void	FileFactoryListClass::Remove_FileFactory( FileFactoryClass * factory )
{
	for (int index = 0; index < FactoryList.Count (); index ++) {

		//
		//	If this is the factory we're looking for, then remove
		// it from the list.
		//
		if (FactoryList[index] == factory) {
			FactoryList.Delete (index);
			FactoryNameList.Delete (index);
			Reset_Search_Start ();
			break;
		}
	}

	return ;
}

/***********************************************************************************************
 * FileFactoryListClass::Remove_FileFactory -- Remove any old file factory                     *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    Nothing                                                                           *
 *                                                                                             *
 * OUTPUT:   Ptr to removed factory. NULL if none in the list                                  *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   9/7/2001 4:40PM ST : Created                                                              *
 *=============================================================================================*/
FileFactoryClass *FileFactoryListClass::Remove_FileFactory(void)
{
	FileFactoryClass *factory = NULL;

	if (FactoryList.Count()) {
		factory = FactoryList[0];
		FactoryList.Delete(0);
		FactoryNameList.Delete(0);
	}

	Reset_Search_Start ();
	return(factory);
}


void	FileFactoryListClass::Add_Temp_FileFactory( FileFactoryClass * factory )
{
	TempFactory = factory;
}

FileFactoryClass * FileFactoryListClass::Remove_Temp_FileFactory( void )
{
	FileFactoryClass * factory = TempFactory;
	TempFactory = NULL;
	return factory;
}


FileClass * FileFactoryListClass::Get_File( char const *filename )
{
	// Very kludgly...

	// Then the temp factory
	if ( TempFactory ) {
		FileClass * file = TempFactory->Get_File( filename );
		if ( file != NULL ) {
			if ( file->Is_Available() ) {
				return file;
			} else {
				TempFactory->Return_File( file );
			}
		}
	}

	// Try the first in the list...
	if ( SearchStartIndex < FactoryList.Count() ) {
		FileClass * file = FactoryList[SearchStartIndex]->Get_File( filename );
		if ( file != NULL ) {
			if ( file->Is_Available() ) {
				return file;
			} else {
				FactoryList[SearchStartIndex]->Return_File( file );
			}
		}
	}

	// Then try the rest
	for ( int i = 0; i < FactoryList.Count(); i++ ) {
		if (i != SearchStartIndex) {
			FileClass * file = FactoryList[i]->Get_File( filename );
			if ( file != NULL ) {
				if ( file->Is_Available() ) {
					return file;
				} else {
					FactoryList[i]->Return_File( file );
				}
			}
		}
	}

	// Failed!

	// Just use the first and don't check for available
	if ( FactoryList.Count() > 0 ) {
		FileClass * file = FactoryList[0]->Get_File( filename );
		if ( file != NULL ) {
			return file;
		}
	}

	return NULL;
}

void FileFactoryListClass::Return_File( FileClass *file )
{
	// This is kinda bad. Just return it to the first one.  (Since they all do the same thing)
	FactoryList[0]->Return_File( file );
}


void FileFactoryListClass::Set_Search_Start( const char *name )
{
	SearchStartIndex = 0;

	//
	//	Try to find which mix file we should start searching from...
	//
	for (int index = 0; index < FactoryNameList.Count (); index ++) {
		if (FactoryNameList[index].Compare_No_Case( name ) == 0) {
			SearchStartIndex = index;
			break;
		}
	}
	return ;
}
