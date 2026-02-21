#ifndef LIGHTSOLVECONTEXT_H
#define LIGHTSOLVECONTEXT_H

#include "always.h"
#include "lightsolveprogress.h"

class LightSolveObserverClass;

/**
** LightSolveContextClass
** This class is a container for all of the options and current state of
** a light solve.
** Any temporary data which needs to be passed around during the solve can
** be added into this class.
*/
class LightSolveContextClass
{
public:
	LightSolveContextClass(void);
	~LightSolveContextClass(void);

	void								Enable_Filtering(bool onoff) { FilteringEnabled = onoff; }
	bool								Is_Filtering_Enabled(void) { return FilteringEnabled; }
	void								Enable_Occlusion(bool onoff) { OcclusionEnabled = onoff; }
	bool								Is_Occlusion_Enabled(void) { return OcclusionEnabled; }

	LightSolveProgressClass &	Get_Progress(void) { return Progress; }
	
	void								Set_Observer(LightSolveObserverClass * observer) { Observer = observer; }
	LightSolveObserverClass *	Get_Observer(void) { return Observer; }

	void								Update_Observer(void); 

protected:

	bool								OcclusionEnabled;
	bool								FilteringEnabled;

	LightSolveProgressClass		Progress;
	LightSolveObserverClass	*	Observer;
};

/**
** LightSolveObserverClass 
** Derive from this class and install your object in the light solve context to get
** progress callbacks periodically...
*/
class LightSolveObserverClass
{	
public:
	virtual ~LightSolveObserverClass(void) { };
	virtual void Progress_Callback(LightSolveContextClass & context) { };
};

#endif //LIGHTSOLVECONTEXT_H

