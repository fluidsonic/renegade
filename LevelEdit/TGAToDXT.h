#ifndef TGATODXT_H
#define TGATODXT_H


// Includes.
#include <winbase.h>


// Class declerations.
class TGAToDXTClass
{
	public:
		 TGAToDXTClass();
		~TGAToDXTClass();

		bool Convert (const char *inputfilename, const char *outputfilename, FILETIME *writetimeptr, bool &redundantalpha);

	protected:
		void Write (const char *outputfilename);

		FILETIME		  *WriteTimePtr;	// Time stamp of write time of DXT file.	
		unsigned char *Buffer;			// Staging buffer.
		unsigned			BufferSize;		// Size of buffer in bytes.
		unsigned			BufferCount;	// No. of bytes written to buffer.

	friend void ReadDTXnFile (DWORD count, void *buffer);
	friend void WriteDTXnFile (DWORD datacount, void *data);
};


// Externals.
extern TGAToDXTClass _TGAToDXTConverter;


#endif