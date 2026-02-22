//
// Filename:     nicenum.cpp
// Author:       Tom Spencer-Smith
// Date:         Dec 1999
// Description:  NIC enumeration
//

#ifndef __NICENUM_H__
#define __NICENUM_H__

#include "global.h"

//-----------------------------------------------------------------------------
class cNicEnum
{
public:
	static void			Init(void);
	static USHORT		Get_Num_Nics(void)			{return NumNics;}
	static uint32_t *		Get_Nics(void)					{return NicList;}

	enum					{MAX_NICS = 10};

private:
	static uint32_t		Enumerate_Nics(uint32_t * addresses, uint32_t max_addresses);

	static uint32_t		NicList[MAX_NICS];
	static USHORT		NumNics;
};

//-----------------------------------------------------------------------------

#endif // __NICENUM_H__

