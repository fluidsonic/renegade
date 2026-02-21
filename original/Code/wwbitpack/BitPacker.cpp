//
// Filename:     bitpacker.cpp
// Project:      wwbitpack.lib
// Author:       Tom Spencer-Smith
// Date:         June 1998
// Description:  Minimal bit encoding
//

#include "bitpacker.h"

#include <string.h>	// for memset


//-----------------------------------------------------------------------------
//cBitPacker::cBitPacker(UINT buffer_size) :
cBitPacker::cBitPacker() :
	//BufferSize(buffer_size),
	BitWritePosition(0),
	BitReadPosition(0)
{
	//assert(BufferSize > 0);

	//Buffer = new BYTE[BufferSize];
	//assert(Buffer != NULL);
	//memset(Buffer, 0, BufferSize);
	memset(Buffer, 0, MAX_BUFFER_SIZE);
}

//-----------------------------------------------------------------------------
cBitPacker::~cBitPacker() 
{
	//delete [] Buffer;
}

//-----------------------------------------------------------------------------
cBitPacker& cBitPacker::operator=(const cBitPacker& rhs)
{
	//assert(BufferSize == rhs.BufferSize);

	//memcpy(Buffer, rhs.Buffer, rhs.BufferSize);
	memcpy(Buffer, rhs.Buffer, MAX_BUFFER_SIZE);
	BitReadPosition		= rhs.BitReadPosition;
	BitWritePosition		= rhs.BitWritePosition;

   return * this;
}

//-----------------------------------------------------------------------------
//
// This method needs optimization
//
// 02-14-2002 Jani: Optimized the code somewhat. Note that the old code reverted
// the bit order and the new one doesn't, so the versions are not compatible.
// If you use optimized Add_Bits() you need to also use optimize Get_Bits().
//

void cBitPacker::Add_Bits(ULONG value, UINT num_bits)
{
	//
	// N.B. Presently you cannot use this class with an atomic type of more 
	// than 4 bytes, such as a double. Hopefully you would be using a float 
	// instead anyway.
	//
#if 0	// Old version

	ULONG mask = 1 << (num_bits - 1);
	while (mask > 0) {

		//assert(BitWritePosition < BufferSize * 8);

		UINT byte_num = BitWritePosition / 8;
		UINT bit_offset = BitWritePosition % 8;
		bool bit_value = (value & mask) != 0;
		Buffer[byte_num] |= bit_value << bit_offset;

		BitWritePosition++;

		mask >>= 1;
	}

#else	// New faster version

	// Verify that we're not writing over buffer

	// Fill the remaining bits of the write byte first
	UINT byte_num = BitWritePosition >> 3;
	UINT bit_offset = BitWritePosition & 0x7;
	BitWritePosition+=num_bits;		// Advance the write position

	// If write buffer is not byte aligned, write the remaining bits first
	value <<= 32-num_bits;
	if (bit_offset) {
		UINT bit_count = 8 - bit_offset;
		if (bit_count>num_bits) bit_count=num_bits;

		ULONG bit_value = value;
		value <<= bit_count;					// Remove the copied bits
		num_bits -= bit_count;
		bit_value >>= (24+bit_offset);
		Buffer[byte_num++] |= bit_value;
	}

	// Write the rest of the data as bytes
	if (num_bits>8) {
		for (unsigned a=0;a<num_bits;a+=8) {
			Buffer[byte_num++]=(unsigned char)(value>>24);
			value<<=8;
		}
	}
	else {
		Buffer[byte_num]=(unsigned char)(value>>24);
	}
#endif
}

//-----------------------------------------------------------------------------
//
// This method needs optimization
// 02-14-2002 Jani: Optimized. See Add_Bits() for notes.
//
void cBitPacker::Get_Bits(ULONG & value, UINT num_bits)
{
#if 0	// Old version

	value = 0;
	for (int bit = num_bits - 1; bit >= 0; bit--) {

		//assert(BitReadPosition < BufferSize * 8);
		UINT byte_num = BitReadPosition / 8;
		UINT bit_offset = BitReadPosition % 8;
		bool b = (Buffer[byte_num] & (1 << bit_offset)) != 0;

		value += (b << bit);	

		BitReadPosition++;
	}
#else // New faster version

	// Verify that we're not reading over buffer or write pointer

	UINT read_len=num_bits;
	UINT byte_num = BitReadPosition / 8;
	UINT bit_offset = BitReadPosition % 8;
	BitReadPosition += num_bits;

	UINT bit_count = 8 - bit_offset;
	if (bit_count>num_bits) bit_count=num_bits;
	value = (ULONG(Buffer[byte_num++]) << (bit_offset+24));
	num_bits-=bit_count;

	int shift;
	for (shift=24-bit_count;shift>0;shift-=8,num_bits-=8) value|=unsigned(Buffer[byte_num++]) << shift;
	if (num_bits>0) value|=Buffer[byte_num++]>>(-shift);

	value >>= 32-read_len;
#endif
}

//-----------------------------------------------------------------------------
//
// This method is only for use by a packet class when data is received.
//

void cBitPacker::Set_Bit_Write_Position(UINT position)
{
	//assert(position <= BufferSize * 8);
	BitWritePosition = position;
}










/*
//-----------------------------------------------------------------------------
void cBitPacker::Increment_Bit_Position(int num_bits)
{

	for (int i = 0; i < num_bits; i++) {
		Advance_Bit_Position();
		NumBits++;
	}
}

//-----------------------------------------------------------------------------
UINT cBitPacker::Get_Compressed_Size_Bytes() const 
{
	return (int) ceil(BitWritePosition / 8.0f);
}

//-----------------------------------------------------------------------------
inline void cBitPacker::Advance_Bit_Position()
{
	BitWritePosition++;
	
	//
	// If the following assert hits then our buffer is not large enough.
	// We can advance BitWritePosition one bit past the end of the buffer, but
	// we cannot write there.
	//
	//assert(BitWritePosition < BufferSize * 8);
}

*/


