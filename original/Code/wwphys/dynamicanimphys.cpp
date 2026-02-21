#include "dynamicanimphys.h"
#include "wwphysids.h"
#include "persistfactory.h"
#include "simpledefinitionfactory.h"
#include "matinfo.h"

/***********************************************************************************************

	DynamicAnimPhysClass Implementation

***********************************************************************************************/

/*
** Declare a PersistFactory for DynamicAnimPhysClass.  This enables the class to save and load.
*/
SimplePersistFactoryClass<DynamicAnimPhysClass,PHYSICS_CHUNKID_DYNAMICANIMPHYS>	_DynamicAnimPhysFactory;

/*
** Chunk ID's used by DynamicAnimPhysClass
*/
enum
{
	DYNAMICANIMPHYS_CHUNK_DECOPHYS							= 526000339,
	DYNAMICANIMPHYS_CHUNK_VARIABLES,
	DYNAMICANIMPHYS_CHUNK_ANIMMANAGER,
};										

DynamicAnimPhysClass::DynamicAnimPhysClass(void) :
	AnimManager(*this),
	ShadowManager(*this)
{
}

DynamicAnimPhysClass::~DynamicAnimPhysClass(void)
{
}

void DynamicAnimPhysClass::Init(const DynamicAnimPhysDefClass & def)
{
	DecorationPhysClass::Init(def);
	AnimManager.Init(def.AnimManagerDef);
}

void DynamicAnimPhysClass::Set_Model(RenderObjClass * model)
{
	DecorationPhysClass::Set_Model(model);
	Update_Cached_Model_Parameters();

	/*
	** Nuke strike uses a linear offset mapper to fade out, have to reset it so I'm
	** just resetting any mappers in any model used by a DynamicAnimPhys... hacky, oh well.
	*/
	if (Model != NULL) {
		Reset_Mappers(Model);
	}
}

void DynamicAnimPhysClass::Update_Cached_Model_Parameters(void)
{
	/*
	** Set up our animation manager
	*/
	AnimManager.Update_Cached_Model_Parameters();

	/*
	** Set up our shadow manager
	*/
	const DynamicAnimPhysDefClass * def = Get_DynamicAnimPhysDef();
	Enable_Shadow_Generation(def->CastsShadows);
	ShadowManager.Set_Shadow_Planes(def->ShadowNearZ,def->ShadowFarZ);
}

void DynamicAnimPhysClass::Reset_Mappers(RenderObjClass * model)
{
	if (model != NULL) {
		
		MaterialInfoClass * matinfo = model->Get_Material_Info();
		if (matinfo != NULL) {
			matinfo->Reset_Texture_Mappers();
		}
		REF_PTR_RELEASE(matinfo);
		
		for (int i=0; i<model->Get_Num_Sub_Objects(); i++) {
			RenderObjClass * sub_obj = model->Get_Sub_Object(i);
			Reset_Mappers(sub_obj);
			REF_PTR_RELEASE(sub_obj);
		}
	}
}

void DynamicAnimPhysClass::Timestep(float dt)
{

	/*
	** Let the animation manager handle progressing the animation, checking
	** for collisions, and managing any "riders"
	*/
	bool anim_changed = AnimManager.Timestep(dt);
	if (anim_changed) {
		Update_Visibility_Status();
		Update_Cull_Box();
	}
}

void DynamicAnimPhysClass::Post_Timestep_Process(void)
{
	ShadowManager.Update_Shadow();
}

const PersistFactoryClass & DynamicAnimPhysClass::Get_Factory(void) const
{
	return _DynamicAnimPhysFactory;
}

bool DynamicAnimPhysClass::Save(ChunkSaveClass &csave)
{
	csave.Begin_Chunk(DYNAMICANIMPHYS_CHUNK_DECOPHYS);
	DecorationPhysClass::Save(csave);
	csave.End_Chunk();

	csave.Begin_Chunk(DYNAMICANIMPHYS_CHUNK_ANIMMANAGER);
	AnimManager.Save(csave);
	csave.End_Chunk();
	
	return true;
}

bool DynamicAnimPhysClass::Load(ChunkLoadClass &cload)
{
	/*
	** Read in the chunks from the file
	*/
	while (cload.Open_Chunk()) {
		
		switch(cload.Cur_Chunk_ID()) 
		{
			case DYNAMICANIMPHYS_CHUNK_DECOPHYS:
				DecorationPhysClass::Load(cload);
				break;

			case DYNAMICANIMPHYS_CHUNK_ANIMMANAGER:
				AnimManager.Load(cload);
				break;

			default:
				break;
		}
		
		cload.Close_Chunk();
	}

	SaveLoadSystemClass::Register_Post_Load_Callback(this);
	return true;
}

void DynamicAnimPhysClass::On_Post_Load(void)
{
	DecorationPhysClass::On_Post_Load();

	/*
	** Always re-initialize any variables which depend on the model
	*/
	Update_Cached_Model_Parameters();

}

/***********************************************************************************************

	DynamicAnimPhysDefClass Implementation

***********************************************************************************************/

/*
** Persist factory for StaticAnimPhysDefClass's.  This makes them able to save and load.
*/
SimplePersistFactoryClass<DynamicAnimPhysDefClass,PHYSICS_CHUNKID_DYNAMICANIMPHYSDEF>	_DynamicAnimPhysDefFactory;

/*
** Definition factory for StaticAnimPhysDefClass.  This makes it show up in the editor
*/
DECLARE_DEFINITION_FACTORY(DynamicAnimPhysDefClass, CLASSID_DYNAMICANIMPHYSDEF, "DynamicAnimPhys") _DynamicAnimPhysDefDefFactory;

/*
** Chunk ID's used by StaticAnimPhysDefClass
*/
enum 
{
	DYNAMICANIMPHYSDEF_CHUNK_DECOPHYSDEF				= 052600316,			// (parent class)
	DYNAMICANIMPHYSDEF_CHUNK_ANIMMANAGERDEF,
	DYNAMICANIMPHYSDEF_CHUNK_VARIABLES,

	DYNAMICANIMPHYSDEF_VARIABLE_CASTSSHADOWS			= 0x01,
	DYNAMICANIMPHYSDEF_VARIABLE_SHADOWNEARZ,
	DYNAMICANIMPHYSDEF_VARIABLE_SHADOWFARZ,
};

DynamicAnimPhysDefClass::DynamicAnimPhysDefClass(void) :
	CastsShadows(false),
	ShadowNearZ(-1.0f),
	ShadowFarZ(-1.0f)
{
	// Make the animation manager variables editable
	ANIMCOLLISIONMANAGERDEF_EDITABLE_PARAMS( DynamicAnimPhysDefClass , AnimManagerDef );
  	PARAM_SEPARATOR(DynamicAnimPhysDefClass, "Shadow Settings");
	EDITABLE_PARAM(DynamicAnimPhysDefClass,ParameterClass::TYPE_BOOL, CastsShadows);
	FLOAT_UNITS_PARAM(DynamicAnimPhysDefClass,ShadowNearZ, -1.0f,1000.0f, "meters (-1 for default)") 
	FLOAT_UNITS_PARAM(DynamicAnimPhysDefClass,ShadowFarZ, -1.0f,1000.0f, "meters (-1 for default)") 
}

uint32 DynamicAnimPhysDefClass::Get_Class_ID (void) const	
{ 
	return CLASSID_DYNAMICANIMPHYSDEF; 
}

PersistClass * DynamicAnimPhysDefClass::Create(void) const
{
	DynamicAnimPhysClass * obj = NEW_REF(DynamicAnimPhysClass,());
	obj->Init(*this);
	return obj;
}

const PersistFactoryClass & DynamicAnimPhysDefClass::Get_Factory (void) const
{
	return _DynamicAnimPhysDefFactory;
}

const char * DynamicAnimPhysDefClass::Get_Type_Name(void)
{ 
	return "DynamicAnimPhysDef"; 
}

bool DynamicAnimPhysDefClass::Is_Type(const char * type_name)
{
	if (stricmp(type_name,DynamicAnimPhysDefClass::Get_Type_Name()) == 0) {
		return true;
	} else {
		return DecorationPhysDefClass::Is_Type(type_name);
	}
}

bool DynamicAnimPhysDefClass::Save(ChunkSaveClass &csave)
{
	csave.Begin_Chunk(DYNAMICANIMPHYSDEF_CHUNK_DECOPHYSDEF);
	DecorationPhysDefClass::Save(csave);
	csave.End_Chunk();

	csave.Begin_Chunk(DYNAMICANIMPHYSDEF_CHUNK_ANIMMANAGERDEF);
	AnimManagerDef.Save(csave);
	csave.End_Chunk();

	csave.Begin_Chunk(DYNAMICANIMPHYSDEF_CHUNK_VARIABLES);
	WRITE_MICRO_CHUNK(csave,DYNAMICANIMPHYSDEF_VARIABLE_CASTSSHADOWS,CastsShadows);
	WRITE_MICRO_CHUNK(csave,DYNAMICANIMPHYSDEF_VARIABLE_SHADOWNEARZ,ShadowNearZ);
	WRITE_MICRO_CHUNK(csave,DYNAMICANIMPHYSDEF_VARIABLE_SHADOWFARZ,ShadowFarZ);
	csave.End_Chunk();

	return true;
}

bool DynamicAnimPhysDefClass::Load(ChunkLoadClass &cload)
{
	while (cload.Open_Chunk()) {

		switch(cload.Cur_Chunk_ID()) {

			case DYNAMICANIMPHYSDEF_CHUNK_DECOPHYSDEF:
				DecorationPhysDefClass::Load(cload);
				break;

			case DYNAMICANIMPHYSDEF_CHUNK_ANIMMANAGERDEF:
				AnimManagerDef.Load(cload);
				break;

			case DYNAMICANIMPHYSDEF_CHUNK_VARIABLES:
				while (cload.Open_Micro_Chunk()) {
					switch(cload.Cur_Micro_Chunk_ID()) {
						READ_MICRO_CHUNK(cload,DYNAMICANIMPHYSDEF_VARIABLE_CASTSSHADOWS,CastsShadows);
						READ_MICRO_CHUNK(cload,DYNAMICANIMPHYSDEF_VARIABLE_SHADOWNEARZ,ShadowNearZ);
						READ_MICRO_CHUNK(cload,DYNAMICANIMPHYSDEF_VARIABLE_SHADOWFARZ,ShadowFarZ);
					}
					cload.Close_Micro_Chunk();	
				}
				break;

			default:
				break;
		}

		cload.Close_Chunk();
	}
	return true;
}

