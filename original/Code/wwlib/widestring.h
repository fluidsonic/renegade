#pragma once

#include "global.h"

#include "win.h"
#include "wwstring.h"
#include "trim.h"
#include "osdep.h"

//////////////////////////////////////////////////////////////////////
//
//	WideStringClass
//
//	This is a UNICODE (double-byte) version of StringClass.  All
//	operations are performed on wide character strings.
//
//////////////////////////////////////////////////////////////////////
class WideStringClass
{
public:

	////////////////////////////////////////////////////////////
	//	Public constructors/destructors
	////////////////////////////////////////////////////////////
	WideStringClass (int initial_len = 0,				bool hint_temporary = false);
	WideStringClass (const WideStringClass &string,	bool hint_temporary = false);
	WideStringClass (const char16_t *string,				bool hint_temporary = false);
	WideStringClass (char16_t ch,								bool hint_temporary = false);
	WideStringClass (const char *string,				bool hint_temporary = false);
	~WideStringClass (void);

	////////////////////////////////////////////////////////////
	//	Public operators
	////////////////////////////////////////////////////////////
	bool operator== (const char16_t *rvalue) const;
	bool operator!= (const char16_t *rvalue) const;

	inline const WideStringClass &operator= (const WideStringClass &string);
	inline const WideStringClass &operator= (const char16_t *string);
	inline const WideStringClass &operator= (char16_t ch);
	inline const WideStringClass &operator= (const char *string);

	const WideStringClass &operator+= (const WideStringClass &string);
	const WideStringClass &operator+= (const char16_t *string);
	const WideStringClass &operator+= (char16_t ch);

	friend WideStringClass operator+ (const WideStringClass &string1, const WideStringClass &string2);
	friend WideStringClass operator+ (const char16_t *string1, const WideStringClass &string2);
	friend WideStringClass operator+ (const WideStringClass &string1, const char16_t *string2);

	bool operator < (const char16_t *string) const;
	bool operator <= (const char16_t *string) const;
	bool operator > (const char16_t *string) const;
	bool operator >= (const char16_t *string) const;

	char16_t operator[] (int index) const;
	char16_t& operator[] (int index);
	operator const char16_t * (void) const;

	////////////////////////////////////////////////////////////
	//	Public methods
	////////////////////////////////////////////////////////////
	int			Compare (const char16_t *string) const;
	int			Compare_No_Case (const char16_t *string) const;

	inline int	Get_Length (void) const;
	bool			Is_Empty (void) const;

	void			Erase (int start_index, int char_count);
	int _cdecl  Format (const char16_t *format, ...);
	int _cdecl  Format_Args (const char16_t *format, const va_list & arg_list );
	bool			Convert_From (const char *text);
	bool			Convert_To (StringClass &string);
	bool			Convert_To (StringClass &string) const;

	// Trim leading and trailing whitespace (chars <= 32)
	void Trim(void);

	// Check if the string is composed of ANSI range characters. (0-255)
	bool Is_ANSI(void);

	char16_t *		Get_Buffer (int new_length);
	char16_t *		Peek_Buffer (void);

	////////////////////////////////////////////////////////////
	//	Static methods
	////////////////////////////////////////////////////////////
	static void	Release_Resources (void);

private:

	////////////////////////////////////////////////////////////
	//	Private structures
	////////////////////////////////////////////////////////////
	typedef struct _HEADER
	{
		int	allocated_length;
		int	length;
	} HEADER;

	////////////////////////////////////////////////////////////
	//	Private constants
	////////////////////////////////////////////////////////////
	enum
	{
		MAX_TEMP_STRING	= 4,
		MAX_TEMP_LEN		= 256,
		MAX_TEMP_BYTES		= (MAX_TEMP_LEN * sizeof (char16_t)) + sizeof (HEADER),
	};

	////////////////////////////////////////////////////////////
	//	Private methods
	////////////////////////////////////////////////////////////
	void			Get_String (int length, bool is_temp);
	char16_t *		Allocate_Buffer (int length);
	void			Resize (int size);
	void			Uninitialised_Grow (int length);
	void			Free_String (void);

	inline void	Store_Length (int length);
	inline void	Store_Allocated_Length (int allocated_length);
	inline HEADER * Get_Header (void) const;
	int			Get_Allocated_Length (void) const;

	void			Set_Buffer_And_Allocated_Length (char16_t *buffer, int length);

	////////////////////////////////////////////////////////////
	//	Private member data
	////////////////////////////////////////////////////////////
	char16_t *		m_Buffer;

	////////////////////////////////////////////////////////////
	//	Static member data
	////////////////////////////////////////////////////////////
	static char		m_TempString1[MAX_TEMP_BYTES];
	static char		m_TempString2[MAX_TEMP_BYTES];
	static char		m_TempString3[MAX_TEMP_BYTES];
	static char		m_TempString4[MAX_TEMP_BYTES];
	static char16_t *	m_FreeTempPtr[MAX_TEMP_STRING];
	static char16_t *	m_ResTempPtr[MAX_TEMP_STRING];

	static int		m_UsedTempStringCount;
	static FastCriticalSectionClass m_TempMutex;

	static char16_t	m_NullChar;
	static char16_t *	m_EmptyString;
};

///////////////////////////////////////////////////////////////////
//	WideStringClass
///////////////////////////////////////////////////////////////////
inline
WideStringClass::WideStringClass (int initial_len, bool hint_temporary)
	:	m_Buffer (m_EmptyString)
{
	Get_String (initial_len, hint_temporary);
	m_Buffer[0]	= m_NullChar;

	return ;
}

///////////////////////////////////////////////////////////////////
//	WideStringClass
///////////////////////////////////////////////////////////////////
inline
WideStringClass::WideStringClass (char16_t ch, bool hint_temporary)
	:	m_Buffer (m_EmptyString)
{
	Get_String (2, hint_temporary);
	(*this) = ch;
	return ;
}

///////////////////////////////////////////////////////////////////
//	WideStringClass
///////////////////////////////////////////////////////////////////
inline
WideStringClass::WideStringClass (const WideStringClass &string, bool hint_temporary)
 	:	m_Buffer (m_EmptyString)
{
	if (hint_temporary || (string.Get_Length()>1)) {
		Get_String(string.Get_Length()+1, hint_temporary);
	}

	(*this) = string;
	return ;
}

///////////////////////////////////////////////////////////////////
//	WideStringClass
///////////////////////////////////////////////////////////////////
inline
WideStringClass::WideStringClass (const char16_t *string, bool hint_temporary)
	:	m_Buffer (m_EmptyString)
{
	int len=string ? static_cast<int>(wcslen(string)) : 0;
	if (hint_temporary || len>0) {
		Get_String (len+1, hint_temporary);
	}

	(*this) = string;
	return ;
}

///////////////////////////////////////////////////////////////////
//	WideStringClass
///////////////////////////////////////////////////////////////////
inline
WideStringClass::WideStringClass (const char *string, bool hint_temporary)
	:	m_Buffer (m_EmptyString)
{
	if (hint_temporary || (string && strlen(string)>0)) {
		Get_String (static_cast<int>(strlen(string)) + 1, hint_temporary);
	}

	(*this) = string;
	return ;
}

///////////////////////////////////////////////////////////////////
//	~WideStringClass
///////////////////////////////////////////////////////////////////
inline
WideStringClass::~WideStringClass (void)
{
	Free_String ();
	return ;
}

///////////////////////////////////////////////////////////////////
//	Is_Empty
///////////////////////////////////////////////////////////////////
inline bool
WideStringClass::Is_Empty (void) const
{
	return (m_Buffer[0] == m_NullChar);
}

///////////////////////////////////////////////////////////////////
//	Compare
///////////////////////////////////////////////////////////////////
inline int
WideStringClass::Compare (const char16_t *string) const
{
	if (string) {
		return wcscmp (m_Buffer, string);
	}

	return -1;
}

///////////////////////////////////////////////////////////////////
//	Compare_No_Case
///////////////////////////////////////////////////////////////////
inline int
WideStringClass::Compare_No_Case (const char16_t *string) const
{
	if (string) {
		return _wcsicmp (m_Buffer, string);
	}

	return -1;
}

///////////////////////////////////////////////////////////////////
//	operator[]
///////////////////////////////////////////////////////////////////
inline char16_t
WideStringClass::operator[] (int index) const
{
	return m_Buffer[index];
}

inline char16_t&
WideStringClass::operator[] (int index)
{
	return m_Buffer[index];
}

///////////////////////////////////////////////////////////////////
//	operator const char16_t *
///////////////////////////////////////////////////////////////////
inline
WideStringClass::operator const char16_t * (void) const
{
	return m_Buffer;
}

///////////////////////////////////////////////////////////////////
//	operator==
///////////////////////////////////////////////////////////////////
inline bool
WideStringClass::operator== (const char16_t *rvalue) const
{
	return (Compare (rvalue) == 0);
}

///////////////////////////////////////////////////////////////////
//	operator!=
///////////////////////////////////////////////////////////////////
inline bool
WideStringClass::operator!= (const char16_t *rvalue) const
{
	return (Compare (rvalue) != 0);
}

///////////////////////////////////////////////////////////////////
//	operator=
///////////////////////////////////////////////////////////////////
inline const WideStringClass &
WideStringClass::operator= (const WideStringClass &string)
{
	return operator= ((const char16_t *)string);
}

///////////////////////////////////////////////////////////////////
//	operator <
///////////////////////////////////////////////////////////////////
inline bool
WideStringClass::operator < (const char16_t *string) const
{
	if (string) {
		return (wcscmp (m_Buffer, string) < 0);
	}

	return false;
}

///////////////////////////////////////////////////////////////////
//	operator <=
///////////////////////////////////////////////////////////////////
inline bool
WideStringClass::operator <= (const char16_t *string) const
{
	if (string) {
		return (wcscmp (m_Buffer, string) <= 0);
	}

	return false;
}

///////////////////////////////////////////////////////////////////
//	operator >
///////////////////////////////////////////////////////////////////
inline bool
WideStringClass::operator > (const char16_t *string) const
{
	if (string) {
		return (wcscmp (m_Buffer, string) > 0);
	}

	return true;
}

///////////////////////////////////////////////////////////////////
//	operator >=
///////////////////////////////////////////////////////////////////
inline bool
WideStringClass::operator >= (const char16_t *string) const
{
	if (string) {
		return (wcscmp (m_Buffer, string) >= 0);
	}

	return true;
}

///////////////////////////////////////////////////////////////////
//	Erase
///////////////////////////////////////////////////////////////////
inline void
WideStringClass::Erase (int start_index, int char_count)
{
	int len = Get_Length ();

	if (start_index < len) {

		if (start_index + char_count > len) {
			char_count = len - start_index;
		}

		::memmove (	&m_Buffer[start_index],
						&m_Buffer[start_index + char_count],
						static_cast<size_t>(len - (start_index + char_count) + 1) * sizeof (char16_t));

		Store_Length( static_cast<int>(wcslen(m_Buffer)) );
	}

	return ;
}

///////////////////////////////////////////////////////////////////
// Trim leading and trailing whitespace (chars <= 32)
///////////////////////////////////////////////////////////////////
inline void WideStringClass::Trim(void)
{
	wcstrim(m_Buffer);
	int len = static_cast<int>(wcslen(m_Buffer));
	Store_Length(len);
}

///////////////////////////////////////////////////////////////////
//	operator=
///////////////////////////////////////////////////////////////////
inline const WideStringClass &
WideStringClass::operator= (const char16_t *string)
{
	if (string) {
		int len = static_cast<int>(wcslen (string));
		Uninitialised_Grow (len + 1);
		Store_Length (len);

		::memcpy (m_Buffer, string, static_cast<size_t>(len + 1) * sizeof (char16_t));
	}

	return (*this);
}

///////////////////////////////////////////////////////////////////
//	operator=
///////////////////////////////////////////////////////////////////
inline const WideStringClass &
WideStringClass::operator= (const char *string)
{
	Convert_From(string);
	return (*this);
}

///////////////////////////////////////////////////////////////////
//	operator=
///////////////////////////////////////////////////////////////////
inline const WideStringClass &
WideStringClass::operator= (char16_t ch)
{
	Uninitialised_Grow (2);

	m_Buffer[0] = ch;
	m_Buffer[1] = m_NullChar;
	Store_Length (1);

	return (*this);
}

///////////////////////////////////////////////////////////////////
//	operator+=
///////////////////////////////////////////////////////////////////
inline const WideStringClass &
WideStringClass::operator+= (const char16_t *string)
{
	if (string) {
		int cur_len = Get_Length ();
		int src_len = static_cast<int>(wcslen (string));
		int new_len = cur_len + src_len;

		//
		//	Make sure our buffer is large enough to hold the new string
		//
		Resize (new_len + 1);
		Store_Length (new_len);

		//
		//	Copy the new string onto our the end of our existing buffer
		//
		::memcpy (&m_Buffer[cur_len], string, static_cast<size_t>(src_len + 1) * sizeof (char16_t));
	}

	return (*this);
}

///////////////////////////////////////////////////////////////////
//	operator+=
///////////////////////////////////////////////////////////////////
inline const WideStringClass &
WideStringClass::operator+= (char16_t ch)
{
	int cur_len = Get_Length ();
	Resize (cur_len + 2);

	m_Buffer[cur_len]			= ch;
	m_Buffer[cur_len + 1]	= m_NullChar;

	if (ch != m_NullChar) {
		Store_Length (cur_len + 1);
	}

	return (*this);
}

///////////////////////////////////////////////////////////////////
//	Get_Buffer
///////////////////////////////////////////////////////////////////
inline char16_t *
WideStringClass::Get_Buffer (int new_length)
{
	Uninitialised_Grow (new_length);

	return m_Buffer;
}

///////////////////////////////////////////////////////////////////
//	Peek_Buffer
///////////////////////////////////////////////////////////////////
inline char16_t *
WideStringClass::Peek_Buffer (void)
{
	return m_Buffer;
}

///////////////////////////////////////////////////////////////////
//	operator+=
///////////////////////////////////////////////////////////////////
inline const WideStringClass &
WideStringClass::operator+= (const WideStringClass &string)
{
	int src_len = string.Get_Length();
	if (src_len > 0) {
		int cur_len = Get_Length ();
		int new_len = cur_len + src_len;

		//
		//	Make sure our buffer is large enough to hold the new string
		//
		Resize (new_len + 1);
		Store_Length (new_len);

		//
		//	Copy the new string onto our the end of our existing buffer
		//
		::memcpy (&m_Buffer[cur_len], (const char16_t *)string, static_cast<size_t>(src_len + 1) * sizeof (char16_t));
	}

	return (*this);
}

///////////////////////////////////////////////////////////////////
//	operator+=
///////////////////////////////////////////////////////////////////
inline WideStringClass
operator+ (const WideStringClass &string1, const WideStringClass &string2)
{
	WideStringClass new_string(string1, true);
	new_string += string2;
	return new_string;
}

///////////////////////////////////////////////////////////////////
//	operator+=
///////////////////////////////////////////////////////////////////
inline WideStringClass
operator+ (const char16_t *string1, const WideStringClass &string2)
{
	WideStringClass new_string(string1, true);
	new_string += string2;
	return new_string;
}

///////////////////////////////////////////////////////////////////
//	operator+=
///////////////////////////////////////////////////////////////////
inline WideStringClass
operator+ (const WideStringClass &string1, const char16_t *string2)
{
	WideStringClass new_string(string1, true);
	new_string += string2;
	return new_string;
}

///////////////////////////////////////////////////////////////////
//	Get_Allocated_Length
//
//	Return allocated size of the string buffer
///////////////////////////////////////////////////////////////////
inline int
WideStringClass::Get_Allocated_Length (void) const
{
	int allocated_length = 0;

	//
	//	Read the allocated length from the header
	//
	if (m_Buffer != m_EmptyString) {
		HEADER *header		= Get_Header ();
		allocated_length	= header->allocated_length;
	}

	return allocated_length;
}

///////////////////////////////////////////////////////////////////
//	Get_Length
//
//	Return text legth. If length is not known calculate it, otherwise
// just return the previously stored value (strlen tends to take
// quite a lot cpu time if a lot of string combining operations are
// performed.
///////////////////////////////////////////////////////////////////
inline int
WideStringClass::Get_Length (void) const
{
	int length = 0;

	if (m_Buffer != m_EmptyString) {

		//
		//	Read the length from the header
		//
		HEADER *header	= Get_Header ();
		length			= header->length;

		//
		//	Hmmm, a zero length was stored in the header,
		// we better manually get the string length.
		//
		if (length == 0) {
			length = static_cast<int>(wcslen (m_Buffer));
			((WideStringClass *)this)->Store_Length (length);
		}
	}

	return length;
}

///////////////////////////////////////////////////////////////////
//	Set_Buffer_And_Allocated_Length
//
// Set buffer pointer and init size variable. Length is set to 0
// as the contents of the new buffer are not necessarily defined.
///////////////////////////////////////////////////////////////////
inline void
WideStringClass::Set_Buffer_And_Allocated_Length (char16_t *buffer, int length)
{
	Free_String ();
	m_Buffer = buffer;

	//
	//	Update the header (if necessary)
	//
	if (m_Buffer != m_EmptyString) {
		Store_Allocated_Length (length);
		Store_Length (0);
	} else {
	}

	return ;
}

///////////////////////////////////////////////////////////////////
// Allocate_Buffer
///////////////////////////////////////////////////////////////////
inline char16_t *
WideStringClass::Allocate_Buffer (int length)
{
	//
	//	Allocate a buffer that is 'length' characters long, plus the
	// bytes required to hold the header.
	//
	char *buffer = new char[(sizeof (char16_t) * static_cast<size_t>(length)) + sizeof (WideStringClass::_HEADER)];

	//
	//	Fill in the fields of the header
	//
	HEADER *header					= reinterpret_cast<HEADER *>(buffer);
	header->length					= 0;
	header->allocated_length	= length;

	//
	//	Return the buffer as if it was a char16_t pointer
	//
	return reinterpret_cast<char16_t *>(buffer + sizeof (WideStringClass::_HEADER));
}

///////////////////////////////////////////////////////////////////
// Get_Header
///////////////////////////////////////////////////////////////////
inline WideStringClass::HEADER *
WideStringClass::Get_Header (void) const
{
	return reinterpret_cast<HEADER *>(((char *)m_Buffer) - sizeof (WideStringClass::_HEADER));
}

///////////////////////////////////////////////////////////////////
// Store_Allocated_Length
///////////////////////////////////////////////////////////////////
inline void
WideStringClass::Store_Allocated_Length (int allocated_length)
{
	if (m_Buffer != m_EmptyString) {
		HEADER *header					= Get_Header ();
		header->allocated_length	= allocated_length;
	} else {
	}

	return ;
}

///////////////////////////////////////////////////////////////////
// Store_Length
//
// Set length... The caller of this (private) function better
// be sure that the len is correct.
///////////////////////////////////////////////////////////////////
inline void
WideStringClass::Store_Length (int length)
{
	if (m_Buffer != m_EmptyString) {
		HEADER *header		= Get_Header ();
		header->length		= length;
	} else {
	}

	return ;
}

///////////////////////////////////////////////////////////////////
// Convert_To
///////////////////////////////////////////////////////////////////
inline bool
WideStringClass::Convert_To (StringClass &string)
{
	return (string.Copy_Wide (m_Buffer));
}

inline bool
WideStringClass::Convert_To (StringClass &string) const
{
	return (string.Copy_Wide (m_Buffer));
}
