//
// Filename:     fromaddress.h
// Project:      wwnet
// Author:       Tom Spencer-Smith
// Date:         June 1998
// Description:  
//
//-----------------------------------------------------------------------------
#if defined(_MSV_VER)
#pragma once
#endif

#ifndef FROMADDRESS_H
#define FROMADDRESS_H

#include "win.h"
#include <winsock.h>

//
// This trivial class exists solely to speed compile times.
//

class cFromAddress
{
	public:
      cFromAddress& operator=(const cFromAddress& rhs) {FromAddress = rhs.FromAddress; return * this;}

		SOCKADDR_IN FromAddress;
};

#endif // FROMADDRESS_H
