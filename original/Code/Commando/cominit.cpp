//*****************************************************************************
//
//	Copyright (c) 2000 Westwood Studios. All Rights Reserved.
//
//	cominit.cpp
//
// Created on 30 Mar 2001 by Tom Spencer-Smith (Westwood/Vegas)
//
//	Description:
//
//	See h file.
//
//*****************************************************************************

#include "cominit.h"

#include <objbase.h>

//#include "utility.h"

// 
// Creating this instance will setup all COM stuff & do cleanup on program exit
//
static cComInit	global_com_initializer;

//---------------------------------------------------------------------------
cComInit::cComInit
(
	void
)
{
	HRESULT hres = ::CoInitialize(NULL);
	if (!SUCCEEDED(hres))
	{
		::MessageBox(NULL, "Unable to initialize COM.", "Error:", MB_OK | MB_ICONERROR);
		::exit(0);
	}
}

//---------------------------------------------------------------------------
cComInit::~cComInit
(
	void
)
{
	::CoUninitialize();
}

//---------------------------------------------------------------------------

