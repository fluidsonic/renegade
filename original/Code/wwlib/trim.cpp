#include	"always.h"
#include	"trim.h"
#include	<string.h>


/*********************************************************************************************** 
 * strtrim -- Trim leading and trailing white space off of string.                             * 
 *                                                                                             * 
 *    This routine will remove the leading and trailing whitespace from the string specifed.   * 
 *    The string is modified in place.                                                         * 
 *                                                                                             * 
 * INPUT:   buffer   -- Pointer to the string to be trimmed.                                   * 
 *                                                                                             * 
 * OUTPUT:  none                                                                               * 
 *                                                                                             * 
 * WARNINGS:   none                                                                            * 
 *                                                                                             * 
 * HISTORY:                                                                                    * 
 *   02/06/1997 JLB : Created.                                                                 * 
 *=============================================================================================*/
char* strtrim(char* buffer)
{
	if (buffer) {
		/* Strip leading white space from the string. */
		char* source = buffer;

		while ((*source != 0) && ((unsigned char)*source <= 32)) {
			++source;
		}

		if (source != buffer) {
			memmove(buffer, source, strlen(source) + 1);
		}

		/* Clip trailing white space from the string. */
		for (int index = strlen(buffer) - 1; index >= 0; --index) {
			if ((*source != 0) && ((unsigned char)buffer[index] <= 32)) {
				buffer[index] = '\0';
			} else {
				break;
			}
		}
	}

	return buffer;
}


char16_t* wcstrim(char16_t* buffer)
{
	if (buffer) {
		/* Strip leading white space from the string. */
		char16_t* source = buffer;

		while ((*source != 0) && ((unsigned int)*source <= 32)) {
			++source;
		}

		if (source != buffer) {
			int src_len = 0; while (source[src_len]) src_len++;
			memmove(buffer, source, (src_len + 1) * sizeof(char16_t));
		}

		/* Clip trailing white space from the string. */
		int buf_len = 0; while (buffer[buf_len]) buf_len++;
		for (int index = buf_len - 1; index >= 0; --index) {
			if ((*source != 0) && ((unsigned int)buffer[index] <= 32)) {
				buffer[index] = u'\0';
			} else {
				break;
			}
		}
	}

	return buffer;
}
