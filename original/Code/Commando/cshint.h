#ifndef __CSHINT_H__
#define __CSHINT_H__

#include "netevent.h"
#include "netclassids.h"

//-----------------------------------------------------------------------------
//
// A C->S message providing hints to the server that will enable it to better filter 
// data directed at me.
//
class	cCsHint : public cNetEvent
{
public:
   cCsHint(void);

	void						Init(int subject_id);

	virtual void			Export_Creation(BitStreamClass &packet);
	virtual void			Import_Creation(BitStreamClass &packet);

	virtual uint32			Get_Network_Class_ID(void) const				{return NETCLASSID_CSHINT;}

private:

	virtual void			Act(void);

	int						SenderId;
	int						SubjectId;
};

//-----------------------------------------------------------------------------

#endif	// __CSHINT_H__
