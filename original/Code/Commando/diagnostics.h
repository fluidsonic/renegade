#pragma once

#include "global.h"

//
// Filename:     diagnostics.h
// Project:      Commando
// Author:       Tom Spencer-Smith
// Date:         Dec 1998
// Description:  
//
//-----------------------------------------------------------------------------



//
// This class is for displaying the state of variables on the screen.
// Add anything you need to monitor to the Render method.
//


class Render2DTextClass;
class Font3DInstanceClass;

//-----------------------------------------------------------------------------
class cDiagnostics
{
   public:
      static void		Init(void);
      static void		Close(void);
      static void		Render(void);

   private:
		static void		Show_Object_Tally(void);
		static void		Add_Diagnostic(LPCSTR format, ...);

		static Render2DTextClass	*	PRenderer;
		static Font3DInstanceClass *	PFont;
		static float						DiagnosticX;
		static float						DiagnosticY;
};

//-----------------------------------------------------------------------------
