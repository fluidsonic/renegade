#include "always.h"
#include "buildnum.h"
#include <stdio.h>
#include "win.h"

/*
**
** This is just a placeholder for the build number.
** A post build step will stamp the build number into here.
**
*/
char BuildInfoClass::BuildNumber [64] = {"Insert1Build2Number3Here4   xxxx                               "};
char BuildInfoClass::BuildDate   [64] = {"Insert1Build2Date3Here4     xxxx                               "};

/*
** Unreachable code warning.
*/
#pragma warning(disable : 4702)

/***********************************************************************************************
 * BuildInfoClass::Get_Build_Number -- Gets the 32 bit build number.                           *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    Nothing                                                                           *
 *                                                                                             *
 * OUTPUT:   Build number                                                                      *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   10/29/2001 4:54PM ST : Created                                                            *
 *=============================================================================================*/
unsigned long BuildInfoClass::Get_Build_Number(void)
{
	return (*(unsigned long*)(&BuildNumber[28]));
}

/***********************************************************************************************
 * BuildInfoClass::Get_Build_Number_String -- Gets the build number as a human readable string.*
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    Nothing                                                                           *
 *                                                                                             *
 * OUTPUT:   Ptr to build number string                                                        *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   10/29/2001 4:55PM ST : Created                                                            *
 *=============================================================================================*/
char *BuildInfoClass::Get_Build_Number_String(void)
{
	static char _buffer[16];
	sprintf (_buffer, "%d", *(unsigned long*)(&BuildNumber[28]));
	return (_buffer);
}

/***********************************************************************************************
 * BuildInfoClass::Get_Builder_Name -- Gets the name of the person who built this executable.  *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    Nothing                                                                           *
 *                                                                                             *
 * OUTPUT:   Ptr to builder name string                                                        *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   10/29/2001 5:08PM ST : Created                                                            *
 *=============================================================================================*/
char *BuildInfoClass::Get_Builder_Name(void)
{
	return(&BuildNumber[32]);
}

/***********************************************************************************************
 * BuildInfoClass::Get_Build_Date_String -- Gets the date this executable was built on.        *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    Nothing                                                                           *
 *                                                                                             *
 * OUTPUT:   Ptr to build date string                                                          *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   10/29/2001 5:10PM ST : Created                                                            *
 *=============================================================================================*/
char *BuildInfoClass::Get_Build_Date_String(void)
{
	static char _buffer[64];
	SYSTEMTIME systime;

	if (FileTimeToSystemTime ((LPFILETIME)(&BuildDate[28]), &systime) ) {
		sprintf(_buffer, "%02d/%02d/%04d - %02d:%02d:%02d", systime.wMonth, systime.wDay, systime.wYear, systime.wHour, systime.wMinute, systime.wSecond);
	} else {
		_buffer[0] = 0;
	}

	return(_buffer);
}

/***********************************************************************************************
 * BuildInfoClass::Get_Builder_Initials -- Gets the initials of the builder                    *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    Nothing                                                                           *
 *                                                                                             *
 * OUTPUT:   Ptr to string containing initials                                                 *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   10/29/2001 5:12PM ST : Created                                                            *
 *=============================================================================================*/
char *BuildInfoClass::Get_Builder_Initials(void)
{
	static char _buffer[4];

	_buffer[0] = BuildNumber[32];
	_buffer[1] = 0;
	_buffer[2] = 0;

	char *lastname = strchr(&BuildNumber[32], '_');
	if (lastname) {
		_buffer[1] = lastname[1];
	}
	return (_buffer);
}

/***********************************************************************************************
 * BuildInfoClass::Get_Build_Version_String -- Get composite version as string                 *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    Nothing                                                                           *
 *                                                                                             *
 * OUTPUT:   Version string                                                                    *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   10/29/2001 7:30PM ST : Created                                                            *
 *=============================================================================================*/
char *BuildInfoClass::Get_Build_Version_String(void)
{
	static char _buffer[128];
	sprintf(_buffer, "%s-%s", Get_Builder_Initials(), Get_Build_Number_String());
	return(_buffer);
}

/***********************************************************************************************
 * BuildInfoClass::Get_Build_Type -- Get the type of build this is                             *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    Nothing                                                                           *
 *                                                                                             *
 * OUTPUT:   Build type                                                                        *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   10/29/2001 7:40PM ST : Created                                                            *
 *=============================================================================================*/
BuildInfoClass::BuildType BuildInfoClass::Get_Build_Type(void)
{
	#ifndef _DEBUG
			return(BUILD_RELEASE);
	#else //_DEBUG
		return(BUILD_DEBUG);
	#endif //_DEBUG
}

/***********************************************************************************************
 * BuildInfoClass::Get_Build_Type_String -- Get the build type as a string.                    *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    Nothing                                                                           *
 *                                                                                             *
 * OUTPUT:   Nothing                                                                           *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   10/29/2001 7:42PM ST : Created                                                            *
 *=============================================================================================*/
char *BuildInfoClass::Get_Build_Type_String(void)
{
	switch (Get_Build_Type()) {
		case BUILD_DEBUG:
			return("DEBUG");

		case BUILD_RELEASE:
			return("RELEASE");

		case BUILD_PROFILE:
			return("PROFILE");

		default:
			return("UNKNOWN");
	}
}

/***********************************************************************************************
 * BuildInfoClass::Composite_Build_Info -- Get lots of build info                              *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    Nothing                                                                           *
 *                                                                                             *
 * OUTPUT:   Ptr to info                                                                       *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   10/29/2001 7:49PM ST : Created                                                            *
 *=============================================================================================*/
char *BuildInfoClass::Composite_Build_Info(void)
{
	static char _buffer[256];
	sprintf(_buffer, "%s Build %d by %s - Build time %s", Get_Build_Type_String(), Get_Build_Number(), Get_Builder_Name(), Get_Build_Date_String());
	return(_buffer);
}

/***********************************************************************************************
 * BuildInfoClass::Log_Build_Info -- Dump build info to the logfile.                           *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    Nothing                                                                           *
 *                                                                                             *
 * OUTPUT:   Nothing                                                                           *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   10/29/2001 7:49PM ST : Created                                                            *
 *=============================================================================================*/
void BuildInfoClass::Log_Build_Info(void)
{
}

