#pragma once

#include "global.h"

#include "robjlist.h"
#include "multilist.h"
#include "combatphysobserver.h"
#include "renderobjectrecycler.h"

class Matrix3D;
class TimedDecorationPhysDefClass;
class TimedDecorationPhysClass;
class RenderObjClass;

/**
** EffectRecyclerClass
** This class can recycle any "fire-and-forget" eye-candy type rendering objects.  It
** will recycle both the render objects and timed-deco-phys wrappers internally.  This
** system will be used for things in the game like surface-effect particle emitters, 
** explosions, etc.
**
** This object will create/recycle the desired render object, insert it into the combat
** scene, and reclaim the objects when the animation/emitter finishes.
*/
class EffectRecyclerClass : public CombatPhysObserverClass
{
public:

	EffectRecyclerClass(void);
	~EffectRecyclerClass(void);

	void					Reset(void);
	void					Spawn_Effect(TimedDecorationPhysDefClass * definition,const Matrix3D & tm);
	void					Spawn_Effect(const char * render_obj_name,const Matrix3D & tm,float time = -1.0f);
	
	/*
	** PhysObserver Interface - whenever an effect expires, we can re-use the model and
	** physics object.  
	*/
	virtual void		Object_Removed_From_Scene(PhysClass * observed_obj);

private:

	TimedDecorationPhysClass *							internal_get_tdeco(void);
	RenderObjClass *										internal_get_model(const char * robj_name,const Matrix3D & tm);

	RenderObjectRecyclerClass							ModelRecycler;
	RefMultiListClass<TimedDecorationPhysClass>	InactiveTDecos;

};
