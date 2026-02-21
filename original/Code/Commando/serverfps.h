#ifndef __SERVERFPS_H__
#define __SERVERFPS_H__

#include "networkobject.h"

//-----------------------------------------------------------------------------
//
// A S->C mirrored object to inform clients of server fps.
// This singleton object may not be statically allocated.
//
class	cServerFps : public NetworkObjectClass
{
public:

	void						Set_Fps(int fps);
	int						Get_Fps(void)								{return Fps;}

	void						Delete (void)								{}
	virtual void			Export_Frequent(BitStreamClass &packet);
	virtual void			Import_Frequent(BitStreamClass &packet);
	virtual void			Set_Delete_Pending (void) {};		// Never needs deletion since it persists during a game session.

	//
	// Static methods for the global singleton instance
	//
	static void				Create_Instance(void);
	static void				Destroy_Instance(void);
	static cServerFps *	Get_Instance(void);


private:

	//
	// Constructor and destructor are private - static functions are used to
	// manage the global singleton instance.
	//
	cServerFps(void);
	~cServerFps(void) {}

	int						Fps;

	//
	// The global singleton instance
	//
	static cServerFps *	TheInstance;
};

//-----------------------------------------------------------------------------

#endif	// __SERVERFPS_H__

