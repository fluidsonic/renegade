#pragma once

#include "global.h"

#include "physlist.h"

class PhysClass;
class StaticPhysClass;
class RenderObjClass;
class LightSolveContextClass;
class LightSolveProgressClass;

/**
** LightSolveClass
** This class is a collection of static functions which are used to generate static lighting
** solves for the scene or a subset of the scene.
*/

class LightSolveClass
{
public:

static void		Generate_Static_Light_Solve(LightSolveContextClass & context);
static void		Generate_Static_Light_Solve(LightSolveContextClass & context,RefPhysListClass & obj_list);

private:

static void		Compute_Solve(LightSolveContextClass & context,RefPhysListClass & obj_list);
static void		Compute_Solve(LightSolveContextClass & context,StaticPhysClass * phys_obj);
static void		Compute_Solve(LightSolveContextClass & context,RenderObjClass * model,NonRefPhysListClass & light_list);
static bool		Does_Obj_Get_Static_Light_Solve(StaticPhysClass * obj);
static bool		Does_Model_Get_Static_Light_Solve(RenderObjClass * model);

};
