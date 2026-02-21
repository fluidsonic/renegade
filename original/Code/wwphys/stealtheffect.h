
#ifndef STEALTHEFFECT_H
#define STEALTHEFFECT_H

#include "always.h"
#include "materialeffect.h"
#include "vector2.h"
#include "matrixmapper.h"

class ChunkSaveClass;
class ChunkLoadClass;

/**
** StealthEffectClass
** This material effect can manage and display "stealth" mode for an object.
** It has a requirement that the user "timestep" it each frame so that its internal
** state gets updated.  
*/
class StealthEffectClass : public MaterialEffectClass
{
public:
	StealthEffectClass(void);
	~StealthEffectClass(void);
	
	virtual void		Timestep(float dt);
	virtual void		Render_Push(RenderInfoClass & rinfo,PhysClass * obj);
	virtual void		Render_Pop(RenderInfoClass & rinfo);

	void					Enable_Stealth(bool onoff);
	void					Set_Friendly(bool onoff);
	void					Set_Broken(bool onoff);

	bool					Is_Stealth_Enabled(void)					{ return IsStealthEnabled; }
	bool					Is_Friendly(void)								{ return IsFriendly; }
	bool					Is_Broken(void)								{ return IsBroken; }
	bool					Is_Stealthed(void) const					{ return CurrentFraction > 0.5f; }
	
	void					Set_Fade_Distance(float d)					{ FadeDistance = d; }
	float					Get_Fade_Distance(void) const				{ return FadeDistance; }

	void					Set_Current_State( float percent )		{ CurrentFraction = percent; }
	void					Set_Target_State( float percent )		{ TargetFraction = percent; }

	void					Damage_Occured(void);

	/*
	** Save-Load support
	*/
	bool					Save(ChunkSaveClass & csave);
	bool					Load(ChunkLoadClass & cload);

protected:
	
	void					Update_Target_Fraction(void);

	bool					IsStealthEnabled;		// is stealth supposed to be turned on?
	bool					IsFriendly;				// is this object a friend of the player?
	bool					IsBroken;				// is this object's cloaking unit broken?
	float					FadeDistance;			// distance at which the effect fades completely out

	float					CurrentFraction;
	float					TargetFraction;
	
	Vector2				UVRate;

	// Render State
	bool					RenderBaseMaterial;
	bool					RenderStealthMaterial;
	float					IntensityScale;
	Vector2				UVOffset;

	MatrixMapperClass *		Mapper;
	MaterialPassClass *		MaterialPass;
};

#endif //STEALTHEFFECT_H