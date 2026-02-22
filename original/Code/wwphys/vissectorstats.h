#pragma once

#include "global.h"

#include "vector3.h"

class StaticPhysClass;
class VisTableClass;

/**
** VisSectorStatsClass
** This object is used to compute some statistics about the given vis sector.  For example,
** it stores how many polygons are in the vis-set from anywhere in the given sector.  
*/
class VisSectorStatsClass
{
public:

	VisSectorStatsClass(void);
	VisSectorStatsClass(const VisSectorStatsClass &src);
	~VisSectorStatsClass(void);

	const VisSectorStatsClass &	operator= (const VisSectorStatsClass &src);

	void					Compute_Stats(StaticPhysClass * obj,VisTableClass * vistbl);

	int					Get_Vis_Id(void);
	int					Get_Polygon_Count(void);
	int					Get_Texture_Count(void);
	int					Get_Texture_Bytes(void);
	Vector3				Get_Position(void);
	StaticPhysClass *	Peek_Phys_Obj(void);
	const char *		Get_Name(void);

	//
	// For use with DynamicVectorClass
	//
	bool operator== (const VisSectorStatsClass &src)	{ return false; }
	bool operator!= (const VisSectorStatsClass &src)	{ return true; }

protected:

	int						VisId;
	int						PolygonCount;
	int						TextureCount;
	int						TextureBytes;
	Vector3					CenterPoint;
	StaticPhysClass *		PhysObj;
};

/*
** inlines
*/

inline int VisSectorStatsClass::Get_Vis_Id(void)
{
	return VisId;
}

inline int VisSectorStatsClass::Get_Polygon_Count(void)
{
	return PolygonCount;
}

inline int VisSectorStatsClass::Get_Texture_Count(void)
{
	return TextureCount;
}

inline int VisSectorStatsClass::Get_Texture_Bytes(void)
{
	return TextureBytes;
}

inline Vector3 VisSectorStatsClass::Get_Position(void)
{
	return CenterPoint;
}

inline StaticPhysClass *VisSectorStatsClass::Peek_Phys_Obj(void)
{
	return PhysObj;
}
