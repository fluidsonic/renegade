#include "grideffect.h"
#include "ww3d.h"
#include "physresourcemgr.h"
#include "vertmaterial.h"
#include "matpass.h"
#include "matrixmapper.h"
#include "rinfo.h"
#include "camera.h"
#include "phys.h"

/***********************************************************************************************
**
** GridEffectClass Implementation
**
***********************************************************************************************/

const float DEFAULT_GRID_PARAMTER_RATE = 0.5f;

GridEffectClass::GridEffectClass(void) :
	CurrentParameter(0.0f),
	TargetParameter(1.0f),
	ParameterRate(DEFAULT_GRID_PARAMTER_RATE),
	LastRenderTime(0),
	RenderBaseMaterial(true),
	RenderGridMaterial(false),
	Stage0Mapper(NULL),
	Stage1Mapper(NULL),
	MaterialPass(NULL)
{
	LastRenderTime = WW3D::Get_Sync_Time();

	MaterialPass = NEW_REF(MaterialPassClass,());
	
	VertexMaterialClass * vmtl = PhysResourceMgrClass::Create_Emissive_Material();

	Stage0Mapper = NEW_REF(MatrixMapperClass,(0));
	Stage0Mapper->Set_Type(MatrixMapperClass::DEPTH_GRADIENT);
	vmtl->Set_Mapper(Stage0Mapper,0);
	
	Stage1Mapper = NEW_REF(MatrixMapperClass,(1));
	Stage1Mapper->Set_Type(MatrixMapperClass::DEPTH_GRADIENT);
	vmtl->Set_Mapper(Stage1Mapper,1);
	
	MaterialPass->Set_Material(vmtl);
	REF_PTR_RELEASE(vmtl);
	
	ShaderClass shader = ShaderClass::_PresetAdditiveShader;
	shader.Set_Post_Detail_Color_Func(ShaderClass::DETAILCOLOR_ADD);
	MaterialPass->Set_Shader(shader);
	
	MaterialPass->Set_Texture(PhysResourceMgrClass::Peek_Grid_Texture(),0);
	MaterialPass->Set_Texture(PhysResourceMgrClass::Peek_Grid_Texture(),1);
}

GridEffectClass::~GridEffectClass(void)
{
	REF_PTR_RELEASE(Stage0Mapper);
	REF_PTR_RELEASE(Stage1Mapper);
	REF_PTR_RELEASE(MaterialPass);
}
	
void GridEffectClass::Render_Push(RenderInfoClass & rinfo,PhysClass * obj)
{
	if (CurrentParameter == TargetParameter) {
		if (CurrentParameter < 0.5f) {
			TargetParameter = 1.0f;
		} else {
			TargetParameter = 0.0f;
		}
	}

	/*
	** Update the grid properties
	** - compute dt since last render
	** - update the parameter
	** - compute the intensity, if it is above 0.0 then:
	** - compute the texture transform given this camera, update the mappers
	** - compute the texel row, update the mappers
	*/
	int millisecs = WW3D::Get_Sync_Time() - LastRenderTime;
	LastRenderTime = WW3D::Get_Sync_Time();
	float dt = (float)millisecs / 1000.0f;

	float parameter_step = ParameterRate * dt;
	if (parameter_step > WWMath::Fabs(TargetParameter - CurrentParameter)) {
		CurrentParameter = TargetParameter;
	} else {
		if (TargetParameter < CurrentParameter) {
			CurrentParameter -= parameter_step;
		} else {
			CurrentParameter += parameter_step;
		}
	}

	static float _angle = 0.0f;
	static float _angle_delta = 0.001f;
	_angle += _angle_delta;

	GridTransform = obj->Get_Transform();

	Matrix3D camera_tm = rinfo.Camera.Get_Transform();

	Matrix3D xgridtm;
	GridTransform.Get_Inverse(xgridtm);
//	xgridtm.Rotate_Y(DEG_TO_RADF(90.0f));
	Matrix4 tm = Matrix4(xgridtm) * Matrix4(camera_tm);

	Stage0Mapper->Set_Texture_Transform(Matrix4(tm),64.0f);
	Stage0Mapper->Set_Gradient_U_Coord(CurrentParameter);

	Matrix3D ygridtm;
	GridTransform.Get_Inverse(ygridtm);
//	ygridtm.Rotate_X(DEG_TO_RADF(90.0f));
	tm = Matrix4(ygridtm) * Matrix4(camera_tm);

	Stage1Mapper->Set_Texture_Transform(tm,64.0f);
	Stage1Mapper->Set_Gradient_U_Coord(CurrentParameter);
		
	RenderBaseMaterial = (CurrentParameter < 0.5f);

	/*
	** Push this pass if it is visible, decide whether or not to render the base passes
	*/
	rinfo.Push_Material_Pass(MaterialPass);
	
	if (RenderBaseMaterial == false) {
		rinfo.Push_Override_Flags(RenderInfoClass::RINFO_OVERRIDE_ADDITIONAL_PASSES_ONLY);
	}
}

void GridEffectClass::Render_Pop(RenderInfoClass & rinfo)
{
	if (RenderBaseMaterial == false) {
		rinfo.Pop_Override_Flags();
	}
	rinfo.Pop_Material_Pass();
}

void GridEffectClass::Set_Grid_Transform(const Matrix3D & tm)
{
	GridTransform = tm;
}

const Matrix3D & GridEffectClass::Get_Grid_Transform(void)
{
	return GridTransform;
}

void GridEffectClass::Set_Texture(TextureClass * tex)
{
	MaterialPass->Set_Texture(tex);
}

TextureClass * GridEffectClass::Peek_Texture(void)
{
	return MaterialPass->Peek_Texture();
}

