//
// Filename:     encoderlist.cpp
// Project:      wwbitpack.lib
// Author:       Tom Spencer-Smith
// Date:         June 1998
// Description:  
//

#include "encoderlist.h"

#include "wwdebug.h"

//
// Class statics
//
bool cEncoderList::IsCompressionEnabled = true;
cEncoderTypeEntry cEncoderList::EncoderTypes[];

//-----------------------------------------------------------------------------
void cEncoderList::Clear_Entries()
{
	WWDEBUG_SAY(("cEncoderList::Clear_Entries\n"));

	for (int i = 0; i < MAX_ENCODERTYPES; i++) {
		EncoderTypes[i].Invalidate();
	}
}

//-----------------------------------------------------------------------------
cEncoderTypeEntry & cEncoderList::Get_Encoder_Type_Entry(int index)
{
	WWASSERT(index >= 0 && index < MAX_ENCODERTYPES);
	return EncoderTypes[index];
}