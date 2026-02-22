#include "global.h"
#include "bitstream.h"


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
	uint32_t u_value;
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

	for (int i = 0; i < data_size; i++) {
		Add(data[i]);
	}
}

//-----------------------------------------------------------------------------
void BitStreamClass::Get_Raw_Data(char * buffer, USHORT buffer_size, USHORT data_size)
{

	for (int i = 0; i < data_size; i++) {
		Get(buffer[i]);
	}
}

//-----------------------------------------------------------------------------
void BitStreamClass::Add_Terminated_String(LPCSTR string, bool permit_empty)
{

	//
	// The terminating null is not transmitted.
	//
	USHORT len = (USHORT) strlen(string);
	if (!permit_empty) {
	}

	Add(len);
	for (int i = 0; i < len; i++) {
		Add(string[i]);
	}
}

//-----------------------------------------------------------------------------
void BitStreamClass::Get_Terminated_String(char * buffer, USHORT buffer_size, bool permit_empty)
{

	USHORT len;
	Get(len);
	if (!permit_empty) {
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

	//
	// The terminating null is not transmitted.
	//
	USHORT len = (USHORT)wcslen(string);
	if (!permit_empty) {
	}

	Add(len);
	for (int i = 0; i < len; i++) {
		Add((USHORT)string[i]);  // 16-bit wire format
	}
}

//-----------------------------------------------------------------------------
void BitStreamClass::Get_Wide_Terminated_String(WCHAR *buffer, USHORT buffer_len, bool permit_empty)
{

	USHORT len;
	Get(len);
	if (!permit_empty) {
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
	} else {
	}

	UINT compression_pc = (UINT) cMathUtil::Round(100 * c_size / (float) u_size);

	return compression_pc;
}
