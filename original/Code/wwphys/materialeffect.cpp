#include "materialeffect.h"
#include "materialeffectlist.h"
#include "rinfo.h"
#include "matpass.h"

static NonRefMaterialEffectListClass _AllocatedEffects;

/*************************************************************************************************
**
** MaterialEffectClass Implementation
**
*************************************************************************************************/
MaterialEffectClass::MaterialEffectClass(void) :
	AutoRemoveEnabled(false),
	SuppressShadows(false)
{
	_AllocatedEffects.Add(this);
}

MaterialEffectClass::~MaterialEffectClass(void)
{
	_AllocatedEffects.Remove(this);
}

void MaterialEffectClass::Timestep_All_Effects(float dt)
{
	NonRefMaterialEffectListIterator it(&_AllocatedEffects);
	while (!it.Is_Done()) {
		it.Peek_Obj()->Timestep(dt);
		it.Next();
	}
}

/*************************************************************************************************
**
** SimpleEffectClass Implementation
**
*************************************************************************************************/

DEFINE_AUTO_POOL(SimpleEffectClass, SIMPLE_EFFECT_GROWTH_STEP);

SimpleEffectClass::SimpleEffectClass(MaterialPassClass * matpass) : 
	MatPass(NULL) 
{ 
	REF_PTR_SET(MatPass,matpass); 
}

SimpleEffectClass::~SimpleEffectClass(void)											
{ 
	REF_PTR_RELEASE(MatPass); 
}

void SimpleEffectClass::Render_Push(RenderInfoClass & rinfo,PhysClass *)	
{
	rinfo.Push_Material_Pass(MatPass); 
}

void SimpleEffectClass::Render_Pop(RenderInfoClass & rinfo)		
{ 
	rinfo.Pop_Material_Pass(); 
}

