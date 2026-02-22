#pragma once

#include "global.h"

//
// Filename:     netinterface.h
// Project:      Network.lib, for Commando
// Author:       Tom Spencer-Smith
// Date:         Dec 1998
// Description:  
//
//-----------------------------------------------------------------------------


#include "widestring.h"

//GAMESPY - allow 30 chars + ( + 2 chars for collision (add 2..99) + ) + NULL
//const USHORT MAX_NICKNAME_LENGTH = 17; // Includes NULL.
const USHORT MAX_NICKNAME_LENGTH = 35;

//-----------------------------------------------------------------------------
class cNetInterface
{
   public:
      cNetInterface(void);
      ~cNetInterface(void);

      static void						Set_Random_Nickname(void);
      static void						Set_Nickname(WideStringClass & name);
      static WideStringClass		Get_Nickname(void);

		static void Set_Side_Preference(int side);
		static int Get_Side_Preference(void);

   private:
      cNetInterface(const cNetInterface& rhs);					// Disallow copy (compile/link time)
      cNetInterface& operator=(const cNetInterface& rhs);	// Disallow assignment (compile/link time)

		static WideStringClass		Nickname;
		static int mSidePreference;
};

//-----------------------------------------------------------------------------

      //static WideStringClass &	Get_Nickname(void);
