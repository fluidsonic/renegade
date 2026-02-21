#include "diaglog.h"
#include "ffactory.h"
#include "wwfile.h"
#include "timemgr.h"

#include <WTYPES.H>	// for SYSTEMTIME

FileClass * _DiagLogFile = NULL;

/*
**
*/
void	DiagLogClass::Init( void )
{
	FileClass * file = NULL;
	for ( int i = 0; i < 100; i++ ) {
		StringClass	filename;
		filename.Format( "DIAGLOG%03d.TXT", i );
		file = _TheWritingFileFactory->Get_File( filename );
		if ( file == NULL || !file->Is_Available() ) {
			break;
		}
		_TheWritingFileFactory->Return_File( file );
		file = NULL;
	}

	if ( file != NULL ) {
		file->Close();
		file->Open(FileClass::WRITE);
		_DiagLogFile = file;
	}

	SYSTEMTIME dt;
	::GetSystemTime( &dt );
	StringClass dt_string;
	dt_string.Format( "%02d/%02d/%02d %02d:%02d:%02d", dt.wMonth, dt.wDay, dt.wYear, dt.wHour, dt.wMinute, dt.wSecond );
	DIAG_LOG(( "OPEN", "%s", (const char *)dt_string ));
}

void	DiagLogClass::Shutdown( void )
{
	if ( _DiagLogFile != NULL ) {
		SYSTEMTIME dt;
		::GetSystemTime( &dt );
		StringClass dt_string;
		dt_string.Format( "%02d/%02d/%02d %02d:%02d:%02d", dt.wMonth, dt.wDay, dt.wYear, dt.wHour, dt.wMinute, dt.wSecond );
		DIAG_LOG(( "CLOS", "%s", (const char *)dt_string ));

		_DiagLogFile->Close();
		_TheWritingFileFactory->Return_File( _DiagLogFile );
		_DiagLogFile = NULL;
	}
}

void	DiagLogClass::Log_Timed( const char * type, const char * format, ... )
{
	if ( _DiagLogFile != NULL ) {

		va_list arg_list;
		va_start (arg_list, format);
		StringClass data;
		data.Format_Args( format, arg_list );
		va_end (arg_list);

		StringClass line;
		float time = TimeManager::Get_Total_Seconds();
		line.Format( "%s; %1.2f; %s%c%c", type, time, data, 0x0D, 0x0A );
		_DiagLogFile->Write( line, ::strlen( line ) );
	}
}

