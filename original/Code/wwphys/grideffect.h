#if defined(_MSC_VER)
#pragma once
#endif

#ifndef GRIDEFFECT_H
#define GRIDEFFECT_H


#include "always.h"
#include "materialeffect.h"
#include "matrix3d.h"

class MatrixMapperClass;
class TextureClass;


/**
** GridFadeEffectClass
** This material effect super-imposes a 2d projected grid over the object that is rendered.
** The grid fades up, the object disappears, and the grid fades down.
*/
class GridEffectClass : public MaterialEffectClass
{
public:
	GridEffectClass(void);	
	~GridEffectClass(void);
	
	virtual void		Render_Push(RenderInfoClass & rinfo,PhysClass * obj);
	virtual void		Render_Pop(RenderInfoClass & rinfo);

	/*
	** The "parameter" changes from 0 to 1 at the specified rate, it controls
	** all other effects.
	*/
	void					Set_Current_Parameter(float param)	{ CurrentParameter = param; }
	void					Set_Target_Parameter(float param)	{ TargetParameter = param; }
	void					Set_Parameter_Rate(float rate)		{ ParameterRate = rate; }

	float					Get_Current_Parameter(void)			{ return CurrentParameter; }
	float					Get_Target_Parameter(void)				{ return TargetParameter; }
	float					Get_Parameter_Rate(void)				{ return ParameterRate; }
	
	/*
	** Control over the grid transform, this enables you to set the coordinate
	** axes that will generate the grid.
	*/
	void					Set_Grid_Transform(const Matrix3D & tm);
	const Matrix3D &	Get_Grid_Transform(void);

	/*
	** Texture, the texture is used one row at a time.  Animation of the grid
	** lines is achieved by scrolling through the rows of the texture as the
	** "parameter" changes.  GridEffectClass will automatically use a default
	** texture if the user doesn't want to specify a custom one.
	*/
	void					Set_Texture(TextureClass * tex);
	TextureClass *		Peek_Texture(void);

protected:
	
	float					CurrentParameter;
	float					TargetParameter;
	float					ParameterRate;
	unsigned int		LastRenderTime;

	Matrix3D				GridTransform;

	bool					RenderBaseMaterial;
	bool					RenderGridMaterial;
	
	MatrixMapperClass *		Stage0Mapper;
	MatrixMapperClass *		Stage1Mapper;
	MaterialPassClass *		MaterialPass;
};


#endif //GRIDEFFECT_H
