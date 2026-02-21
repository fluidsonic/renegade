//
// Filename:     nicenum.cpp
// Author:       Tom Spencer-Smith
// Date:         Dec 1999
// Description:  NIC enumeration
//

#ifndef __NICENUM_H__
#define __NICENUM_H__

#include "bittype.h"

//-----------------------------------------------------------------------------
class cNicEnum 
{
public:
	static void			Init(void);
	static USHORT		Get_Num_Nics(void)			{return NumNics;}
	static ULONG *		Get_Nics(void)					{return NicList;}

	enum					{MAX_NICS = 10};

private:
	static ULONG		Enumerate_Nics(ULONG * addresses, ULONG max_addresses);

	static ULONG		NicList[MAX_NICS];
	static USHORT		NumNics;
};

//-----------------------------------------------------------------------------

#endif // __NICENUM_H__




