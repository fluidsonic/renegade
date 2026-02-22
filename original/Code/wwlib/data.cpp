#include	"always.h"
#include <new.h>
#include	"data.h"

/***********************************************************************************************
 * Load_Alloc_Data -- Allocates a buffer and loads the file into it.                           *
 *                                                                                             *
 *    This is the C++ replacement for the Load_Alloc_Data function. It will allocate the       *
 *    memory big enough to hold the file and then read the file into it.                       *
 *                                                                                             *
 * INPUT:   file  -- The file to read.                                                         *
 *                                                                                             *
 *          mem   -- The memory system to use for allocation.                                  *
 *                                                                                             *
 * OUTPUT:  Returns with a pointer to the allocated and filled memory block.                   *
 *                                                                                             *
 * WARNINGS:   none                                                                            *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   10/17/1994 JLB : Created.                                                                 *
 *=============================================================================================*/
void * Load_Alloc_Data(FileClass & file)
{
	void * ptr = NULL;
	if (file.Is_Available()) {
		long size = file.Size();

		ptr = new char[size];
		if (ptr != NULL) {
			file.Read(ptr, size);
		}
	}
	return(ptr);
}

/***********************************************************************************************
 * Load_Uncompress -- Loads and uncompresses data to a buffer.                                 *
 *                                                                                             *
 *    This is the C++ counterpart to the Load_Uncompress function. It will load the file       *
 *    specified into the graphic buffer indicated and uncompress it.                           *
 *                                                                                             *
 * INPUT:   file     -- The file to load and uncompress.                                       *
 *                                                                                             *
 *          uncomp_buff -- The graphic buffer that initial loading will use.                   *
 *                                                                                             *
 *          dest_buff   -- The buffer that will hold the uncompressed data.                    *
 *                                                                                             *
 *          reserved_data  -- This is an optional pointer to a buffer that will hold any       *
 *                            reserved data the compressed file may contain. This is           *
 *                            typically a palette.                                             *
 *                                                                                             *
 * OUTPUT:  Returns with the size of the uncompressed data in the destination buffer.          *
 *                                                                                             *
 * WARNINGS:   none                                                                            *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   10/17/1994 JLB : Created.                                                                 *
 *=============================================================================================*/
long Load_Uncompress(FileClass & file, Buffer & uncomp_buff, Buffer & dest_buff, void * reserved_data)
{
	unsigned short	size;
	void	* sptr = uncomp_buff.Get_Buffer();
	void	* dptr = dest_buff.Get_Buffer();
	int	opened = false;
	CompHeaderType	header;

	/*
	**	The file must be opened in order to be read from. If the file
	**	isn't opened, then open it. Record this fact so that it can be
	**	restored to its closed state at the end.
	*/
	if (!file.Is_Open()) {
		if (!file.Open()) {
			return(0);
		}
		opened = true;
	}

	/*
	**	Read in the size of the file (supposedly).
	*/
	file.Read(&size, sizeof(size));

	/*
	**	Read in the header block. This block contains the compression type
	**	and skip data (among other things).
	*/
	file.Read(&header, sizeof(header));
	size -= (unsigned short)sizeof(header);

	/*
	**	If there are skip bytes then they must be processed. Either read
	**	them into the buffer provided or skip past them. No check is made
	**	to ensure that the reserved data buffer is big enough (watch out!).
	*/
	if (header.Skip) {
		size -= header.Skip;
		if (reserved_data) {
			file.Read(reserved_data, header.Skip);
		} else {
			file.Seek(header.Skip, SEEK_CUR);
		}
		header.Skip = 0;
	}

	/*
	**	Determine where is the proper place to load the data. If both buffers
	**	specified are identical, then the data should be loaded at the end of
	**	the buffer and decompressed at the beginning.
	*/
	if (uncomp_buff.Get_Buffer() == dest_buff.Get_Buffer()) {
		sptr = (char *)sptr + uncomp_buff.Get_Size()-(size+sizeof(header));
	}

	/*
	**	Read in the bulk of the data.
	*/
	memmove(sptr, &header, sizeof(header));
//	Mem_Copy(&header, sptr, sizeof(header));
	file.Read((char *)sptr + sizeof(header), size);

	/*
	**	Decompress the data.
	*/
	size = (unsigned short) Uncompress_Data(sptr, dptr);

	/*
	**	Close the file if necessary.
	*/
	if (opened) {
		file.Close();
	}
	return((long)size);
}

typedef struct SRecord {
	int ID;						// ID number of the string resource.
	int TimeStamp;				// 'Time' that this string was last requested.
	char String[2048];			// Copy of string resource.

	SRecord(void) : ID(-1), TimeStamp(-1) {}
} SRecord;

/***********************************************************************************************
 * Fetch_String -- Fetches a string resource.                                                  *
 *                                                                                             *
 *    Fetches a string resource and returns a pointer to its text.                             *
 *                                                                                             *
 * INPUT:   id -- The ID number of the string resource to fetch.                               *
 *                                                                                             *
 * OUTPUT:  Returns with a pointer to the actual text of the string resource.                  *
 *                                                                                             *
 * WARNINGS:   none                                                                            *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   12/25/1996 JLB : Created.                                                                 *
 *=============================================================================================*/
char const * Fetch_String(int id)
{
	return("");
}

void const * Fetch_Resource(LPCSTR resname, LPCSTR restype)
{
	return(NULL);
}

int Load_Picture(FileClass & file, Buffer & scratchbuf, Buffer & destbuf, unsigned char * palette, PicturePlaneType )
{
	return(Load_Uncompress(file, scratchbuf, destbuf,  palette ) / 8000);
}

/***********************************************************************************************
 * Hires_Load -- Allocates memory for, and loads, a resolution dependant file.                 *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    Name of file to load                                                              *
 *                                                                                             *
 * OUTPUT:   Ptr to loaded file                                                                *
 *                                                                                             *
 * WARNINGS: Caller is responsible for releasing the memory allocated                          *
 *                                                                                             *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *    5/13/96 3:20PM ST : Created                                                              *
 *=============================================================================================*/
void * Hires_Load(FileClass & file)
{
	int 	length;
	void * return_ptr;

	if (file.Is_Available()) {

		length = file.Size();
		return_ptr = new char[length];
		file.Read(return_ptr, length);
		return (return_ptr);

	} else {
		return (NULL);
	}
}

