//
// Filename:     netinterface.cpp
// Project:      Network.lib, for Commando
// Author:       Tom Spencer-Smith
// Date:         Dec 1998
// Description:  
//

#include "netinterface.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "miscutil.h"
#include "win.h"
#include "mmsys.h"
#include "useroptions.h"

//
// class statics
//
WideStringClass	cNetInterface::Nickname;
int cNetInterface::mSidePreference = -1;

//-----------------------------------------------------------------------------
cNetInterface::cNetInterface(void)
{
}

//-----------------------------------------------------------------------------
cNetInterface::~cNetInterface(void)
{
}

//-----------------------------------------------------------------------------
WideStringClass cNetInterface::Get_Nickname(void)
{
	if (strcmp(cUserOptions::GameSpyNickname.Get(), "") != 0) {
		WideStringClass wide_name;
		wide_name.Convert_From(cUserOptions::GameSpyNickname.Get());
		return wide_name;
	}
	return Nickname;
}

//-----------------------------------------------------------------------------
void cNetInterface::Set_Nickname(WideStringClass & name)
{
	Nickname = name;

	//
	// Abbreviate to 9 chars
	//
	if (Nickname.Get_Length() > 9) {
		Nickname[9] = 0;
	}
}

//-----------------------------------------------------------------------------
void cNetInterface::Set_Random_Nickname(void)
{      
	char name[MAX_COMPUTERNAME_LENGTH + 1];
	DWORD size = sizeof(name);
	::GetComputerName(name, &size);

	int length_test = MAX_COMPUTERNAME_LENGTH + 1 - MAX_NICKNAME_LENGTH;
	if (length_test > 0) {
		name[MAX_NICKNAME_LENGTH - 1] = 0;
	}

	WideStringClass widename;
	widename.Convert_From(name);

   Set_Nickname(widename);
}

void cNetInterface::Set_Side_Preference(int side)
{
	mSidePreference = side;
}

int cNetInterface::Get_Side_Preference(void)
{
	return mSidePreference;
}

//WideStringClass & cNetInterface::Get_Nickname(void)
	/*
	Nickname = name;

  //
	// Abbreviate to 9 chars
	//
	if (Nickname.Get_Length() > 9) {
		Nickname[9] = 0;
	}
	*/

   //return Nickname;

