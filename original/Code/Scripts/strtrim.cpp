#include "strtrim.h"
#include <ctype.h>
#include <stddef.h>
#include <string.h>


/******************************************************************************
*
* NAME
*     strtrim
*
* DESCRIPTION
*     Trim leading and trailing white space off of a string.
*
* INPUTS
*     char* buffer
*
* RESULTS
*     char*
*
******************************************************************************/

char* strtrim(char* buffer)
	{
	if (buffer != NULL)
		{
		// Strip leading white space from the string.
		char* source = buffer;
		
		while (isspace(*source))
			source++;

		if (source != buffer)
			strcpy(buffer, source);

		// Clip trailing white space from the string.
		for (int index = strlen(buffer) - 1; index >= 0; index--)
			{
			if (isspace(buffer[index]))
				buffer[index] = '\0';
			else
				break;
			}
		}

	return buffer;
	}
