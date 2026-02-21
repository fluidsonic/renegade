#ifndef RLE_H
#define RLE_H

/*
**	This class will RLE compress and decompress arbitrary blocks of data. This RLE compression
**	is geared to compressing only runs of 0 bytes. This makes it useful for sprite encoding.
*/
class RLEEngine 
{
	public:
	
		/*
		**	Codec for arbitrary blocks.
		*/
		int Compress(void const * source, void * dest, int length) const;
		int Decompress(void const * source, void * dest, int length) const;

		/*
		**	Codec for length encoded blocks. This is useful for sprite storage.
		*/
		int Line_Compress(void const * source, void * dest, int length) const;
		int Line_Decompress(void const * source, void * dest) const;
};

#endif

