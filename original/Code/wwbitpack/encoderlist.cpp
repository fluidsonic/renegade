//
// Filename:     encoderlist.cpp
// Project:      wwbitpack.lib
// Author:       Tom Spencer-Smith
// Date:         June 1998
// Description:  
//

#include "encoderlist.h"


//
// Class statics
//
bool cEncoderList::IsCompressionEnabled = true;
cEncoderTypeEntry cEncoderList::EncoderTypes[];

//-----------------------------------------------------------------------------
void cEncoderList::Clear_Entries()
{

	for (int i = 0; i < MAX_ENCODERTYPES; i++) {
		EncoderTypes[i].Invalidate();
	}
}

//-----------------------------------------------------------------------------
cEncoderTypeEntry & cEncoderList::Get_Encoder_Type_Entry(int index)
{
	return EncoderTypes[index];
}