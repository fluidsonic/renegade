#include "global.h"
#include "effectrecycler.h"
#include "timeddecophys.h"
#include "rendobj.h"
#include "combat.h"
#include "assetmgr.h"
#include "htree.h"
#include "hanim.h"
#include "part_emt.h"

/***********************************************************************************************
 * EffectRecyclerClass::EffectRecyclerClass -- constructor                                     *
 *                                                                                             *
 * INPUT:                                                                                      *
 *                                                                                             *
 * OUTPUT:                                                                                     *
 *                                                                                             *
 * WARNINGS:                                                                                   *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *=============================================================================================*/
EffectRecyclerClass::EffectRecyclerClass(void)
{
}

/***********************************************************************************************
 * EffectRecyclerClass::~EffectRecyclerClass -- destructor                                     *
 *                                                                                             *
 * INPUT:                                                                                      *
 *                                                                                             *
 * OUTPUT:                                                                                     *
 *                                                                                             *
 * WARNINGS:                                                                                   *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *=============================================================================================*/
EffectRecyclerClass::~EffectRecyclerClass(void)
{
	Reset();
}

/***********************************************************************************************
 * EffectRecyclerClass::Reset -- releases all resources                                        *
 *                                                                                             *
 * INPUT:                                                                                      *
 *                                                                                             *
 * OUTPUT:                                                                                     *
 *                                                                                             *
 * WARNINGS:                                                                                   *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   5/8/2001   gth : Created.                                                                 *
 *   7/11/2001  gth : Created.                                                                 *
 *=============================================================================================*/
void EffectRecyclerClass::Reset(void)
{
	ModelRecycler.Reset();
	InactiveTDecos.Reset_List();
}

/***********************************************************************************************
 * EffectRecyclerClass::Spawn_Effect -- Spawn a timed effect into the combat scene             *
 *                                                                                             *
 * INPUT:                                                                                      *
 *                                                                                             *
 * OUTPUT:                                                                                     *
 *                                                                                             *
 * WARNINGS:                                                                                   *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   5/8/2001   gth : Created.                                                                 *
 *=============================================================================================*/
void EffectRecyclerClass::Spawn_Effect(TimedDecorationPhysDefClass * def,const Matrix3D & tm)
{
	Spawn_Effect(def->Get_Model_Name(),tm,def->Get_Lifetime());
}

/***********************************************************************************************
 * EffectRecyclerClass::Spawn_Effect -- spawn an effect into the combat scene                  *
 *                                                                                             *
 * INPUT:                                                                                      *
 *                                                                                             *
 * OUTPUT:                                                                                     *
 *                                                                                             *
 * WARNINGS:                                                                                   *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   5/8/2001   gth : Created.                                                                 *
 *=============================================================================================*/
void EffectRecyclerClass::Spawn_Effect(const char * robj_name,const Matrix3D & tm,float time)
{
	// allocate/recycle the render object
	RenderObjClass * model = internal_get_model(robj_name,tm);

	if (model != NULL) {

		// allocate/recycle a TDeco
		TimedDecorationPhysClass * tdeco = internal_get_tdeco();

		if ((time == -1.0f) && (model->Class_ID() == RenderObjClass::CLASSID_PARTICLEEMITTER)) {
			ParticleEmitterClass * emitter = (ParticleEmitterClass *)model;
			time = emitter->Get_Lifetime();
		}

		// configure and add to the scene
		tdeco->Set_Model(model);
		tdeco->Set_Lifetime(time);
		tdeco->Enable_Dont_Save(true);
		tdeco->Enable_Is_Pre_Lit(true);
		tdeco->Set_Collision_Group(UNCOLLIDEABLE_GROUP);
		tdeco->Set_Observer(this);

		COMBAT_SCENE->Add_Dynamic_Object(tdeco);
		tdeco->Set_Transform(tm);

		REF_PTR_RELEASE(model);
		REF_PTR_RELEASE(tdeco);
	}
}

/***********************************************************************************************
 * EffectRecyclerClass::Object_Removed_From_Scene -- physobserver interface, reclaim our objec *
 *                                                                                             *
 *    Reclaims the model and tdeco when they expire.                                           *
 *                                                                                             *
 * INPUT:                                                                                      *
 *                                                                                             *
 * OUTPUT:                                                                                     *
 *                                                                                             *
 * WARNINGS:                                                                                   *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   5/8/2001   gth : Created.                                                                 *
 *=============================================================================================*/
void EffectRecyclerClass::Object_Removed_From_Scene(PhysClass * observed_obj)
{

	TimedDecorationPhysClass * tdeco = observed_obj->As_TimedDecorationPhysClass();
	RenderObjClass * model = tdeco->Peek_Model();
	ModelRecycler.Return_Render_Object(model);
	tdeco->Set_Model(NULL);

	InactiveTDecos.Add(tdeco);
}

/***********************************************************************************************
 * EffectRecyclerClass::internal_get_tdeco -- internally recycle or allocate a tdeco           *
 *                                                                                             *
 * INPUT:                                                                                      *
 *                                                                                             *
 * OUTPUT:                                                                                     *
 *                                                                                             *
 * WARNINGS:                                                                                   *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   5/8/2001   gth : Created.                                                                 *
 *=============================================================================================*/
TimedDecorationPhysClass * EffectRecyclerClass::internal_get_tdeco(void)
{
	if (InactiveTDecos.Is_Empty()) {
		return NEW_REF(TimedDecorationPhysClass,());
	} else {
		return InactiveTDecos.Remove_Head();
	}
}

/***********************************************************************************************
 * EffectRecyclerClass::internal_get_model -- recycle or allocate a render object              *
 *                                                                                             *
 * INPUT:                                                                                      *
 *                                                                                             *
 * OUTPUT:                                                                                     *
 *                                                                                             *
 * WARNINGS:                                                                                   *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   5/8/2001   gth : Created.                                                                 *
 *=============================================================================================*/
RenderObjClass * EffectRecyclerClass::internal_get_model(const char * robj_name,const Matrix3D & tm)
{
	RenderObjClass * model = ModelRecycler.Get_Render_Object(robj_name,tm);

	if ((model != NULL) && (model->Get_HTree() != NULL)) {
		/*
		** Auto play an anim if we find it
		*/
		StringClass	anim_name;
		anim_name.Format( "%s.%s", 
			model->Get_HTree()->Get_Name(), 
			model->Get_HTree()->Get_Name() );
		HAnimClass * anim = WW3DAssetManager::Get_Instance()->Get_HAnim( anim_name );
		if ( anim != NULL ) {
			model->Set_Animation( anim, 0, RenderObjClass::ANIM_MODE_ONCE );
			anim->Release_Ref();
		}
	}

	return model;
}
