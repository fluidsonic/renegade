#include "bitstream.h"

#include <string.h>	// for strlen
#include <math.h>		// for ceil

#include "wwdebug.h"
#include "mathutil.h"
#include "widestring.h"


//-----------------------------------------------------------------------------
BitStreamClass::BitStreamClass() :
	cBitPacker(),
	UncompressedSizeBytes(0)
{
}

//-----------------------------------------------------------------------------
BitStreamClass& BitStreamClass::operator=(const BitStreamClass& rhs)
{
	//
	// Call operator for base class
	//
	cBitPacker::operator= (rhs);

	UncompressedSizeBytes = rhs.UncompressedSizeBytes;

   return * this;
}

//-----------------------------------------------------------------------------
void BitStreamClass::Add(bool value)
{
	if (cEncoderList::Is_Compression_Enabled()) {
		Add_Bits(value, 1);
	} else {
		Add_Bits(value, BIT_DEPTH(bool));
	}

	UncompressedSizeBytes += BYTE_DEPTH(bool);
}

//-----------------------------------------------------------------------------
bool BitStreamClass::Get(bool & value)
{
	ULONG u_value;
	if (cEncoderList::Is_Compression_Enabled()) {
		Get_Bits(u_value, 1);
	} else {
		Get_Bits(u_value, BIT_DEPTH(bool));
	}

	value = (u_value == 1);
	return value;
}

//-----------------------------------------------------------------------------
void BitStreamClass::Add_Raw_Data(LPCSTR data, USHORT data_size)
{
	WWASSERT(data != NULL);
	WWASSERT(data_size >= 0);

	for (int i = 0; i < data_size; i++) {
		Add(data[i]);
	}
}

//-----------------------------------------------------------------------------
void BitStreamClass::Get_Raw_Data(char * buffer, USHORT buffer_size, USHORT data_size)
{
	WWASSERT(buffer != NULL);
	WWASSERT(data_size >= 0);
   WWASSERT(buffer_size >= data_size);

	for (int i = 0; i < data_size; i++) {
		Get(buffer[i]);
	}
}

//-----------------------------------------------------------------------------
void BitStreamClass::Add_Terminated_String(LPCSTR string, bool permit_empty)
{
	WWASSERT(string != NULL);

	//
	// The terminating null is not transmitted.
	//
	USHORT len = (USHORT) strlen(string);
	if (!permit_empty) {
		WWASSERT(len > 0);
	}

	Add(len);
	for (int i = 0; i < len; i++) {
		Add(string[i]);
	}
}

//-----------------------------------------------------------------------------
void BitStreamClass::Get_Terminated_String(char * buffer, USHORT buffer_size, bool permit_empty)
{
	WWASSERT(buffer != NULL);
	WWASSERT(buffer_size > 0);

	USHORT len;
	Get(len);
	WWASSERT(len < buffer_size);
	if (!permit_empty) {
		WWASSERT(len > 0);
	}

	char temp = '?';
	int i = 0;
	for (i = 0; i < len; i++) {
		Get(temp);
		if (i < buffer_size - 1) {
			buffer[i] = temp;
		}
	}

	// Null-terminate it.
	if (i < buffer_size) {
		buffer[i] = 0;
	} else {
		buffer[buffer_size - 1] = 0;
	}
}


//-----------------------------------------------------------------------------
void BitStreamClass::Add_Wide_Terminated_String(const WCHAR *string, bool permit_empty)
{
	WWASSERT(string != NULL);

	//
	// The terminating null is not transmitted.
	//
	USHORT len = (USHORT)wcslen (string);
	if (!permit_empty) {
		WWASSERT(len > 0 && "Empty string not permitted");
	}

	Add(len);
	for (int i = 0; i < len; i++) {
		Add(string[i]);
	}
}

//-----------------------------------------------------------------------------
void BitStreamClass::Get_Wide_Terminated_String(WCHAR *buffer, USHORT buffer_len, bool permit_empty)
{
	WWASSERT(buffer != NULL);
	WWASSERT(buffer_len > 0);

	USHORT len;
	Get(len);
	WWASSERT(len < buffer_len && "String length exceeds provided buffer");
	if (!permit_empty) {
		WWASSERT(len > 0 && "Empty string not permitted");
	}

	USHORT temp_u = 0;
	int i = 0;
	for (i = 0; i < len; i++) {
		Get(temp_u);
		if (i < buffer_len - 1) {
			buffer[i] = (WCHAR)temp_u;
		}
	}

	if (i < buffer_len - 1) {
		buffer[i] = 0; // Null-terminate it.
	} else {
		buffer[buffer_len-1] = 0;
	}
}


//-----------------------------------------------------------------------------
UINT BitStreamClass::Get_Compressed_Size_Bytes() const
{
	return (UINT) ceil(Get_Bit_Write_Position() / 8.0f);
}

//-----------------------------------------------------------------------------
UINT BitStreamClass::Get_Compression_Pc() const
{
	UINT c_size = Get_Compressed_Size_Bytes();
	UINT u_size = Get_Uncompressed_Size_Bytes();

	if (cEncoderList::Is_Compression_Enabled()) {
		WWASSERT(c_size <= u_size);
	} else {
		WWASSERT(c_size == u_size);
	}

	WWASSERT(u_size > 0);

	UINT compression_pc = (UINT) cMathUtil::Round(100 * c_size / (float) u_size);
	WWASSERT(compression_pc >= 0 && compression_pc <= 100);

	return compression_pc;
}
