//
// Filename:     demosupport.h
// Author:       Tom Spencer-Smith
// Date:         Feb 2002
// Description:  MP Demo support
//

#ifndef __DEMOSUPPORT_H__
#define __DEMOSUPPORT_H__

#include <stdlib.h>
#include <wwlib/realcrc.h>
#include "gamedata.h"

#define DEMO_SECURITY_CHECK	cDemoSupport::Security_Check();

//-----------------------------------------------------------------------------
class cDemoSupport 
{
public:
	static __forceinline void Security_Check(void);

private:
};

//-----------------------------------------------------------------------------
//
// Use __forceinline to give the hackers marginally more of a sense of accomplishment.
// This routine should be called a handful of times each frame, from different 
// places in the code.
//
__forceinline void 
cDemoSupport::Security_Check
(
	void
)
{
}

//-----------------------------------------------------------------------------

#endif // __DEMOSUPPORT_H__

