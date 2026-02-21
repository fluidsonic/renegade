

#ifndef HUMANPHYS_H
#define HUMANPHYS_H

#include "vector3.h"
#include "phys3.h"

class HumanPhysDefClass;

/**
** HumanPhysClass
** This class performs additional human physics logic.  It basically adds
** user control to the normal Phys3Class behavior.
*/

class HumanPhysClass : public Phys3Class
{
public:

	HumanPhysClass(void);
	virtual ~HumanPhysClass(void);
	virtual HumanPhysClass *	As_HumanPhysClass(void) { return this; }

	void								Init(const HumanPhysDefClass & def);

	virtual void					Timestep(float dt); 
	virtual void					Render(RenderInfoClass & rinfo);

	/*
	** Simulation and rendering toggles for all static physics objects
	*/
	virtual bool					Is_Simulation_Disabled(void)				{ return _DisableHumanSimulation || Phys3Class::Is_Simulation_Disabled(); }
	virtual bool					Is_Rendering_Disabled(void)				{ return _DisableHumanRendering || Phys3Class::Is_Simulation_Disabled(); }
	static void						Disable_All_Simulation(bool onoff)		{ _DisableHumanSimulation = onoff; }
	static void						Disable_All_Rendering(bool onoff)		{ _DisableHumanRendering = onoff; }
	static bool						Is_All_Simulation_Disabled(void)			{ return _DisableHumanSimulation; }
	static bool						Is_All_Rendering_Disabled(void)			{ return _DisableHumanRendering; }

	/*
	** Save-Load system
	*/
	virtual const PersistFactoryClass &	Get_Factory (void) const;
	virtual bool								Save (ChunkSaveClass &csave);
	virtual bool								Load (ChunkLoadClass &cload);

	bool								Has_Just_Jumped (void) const { return JustJumped; }
	void								Jump_To_Point (const Vector3 &point);
	
protected:

	virtual void					Check_Ground(const AABoxClass & box,GroundStateStruct * gs,float check_dist);
	virtual bool					Ballistic_Move(float dt);
	virtual bool					Normal_Move(const GroundStateStruct & gs,float dt);
	virtual bool					Slide_Move(const GroundStateStruct & gs,float dt);
	void								Compute_Desired_Move_Vector(const GroundStateStruct & gs,float dt,Vector3 * set_move);

	bool								JustJumped;			// character jumped and has not started coming down yet, dont snap him!
	bool								IsAIControlledJump;

private:
	
	static bool						_DisableHumanSimulation;
	static bool						_DisableHumanRendering;

	// not implemented
	HumanPhysClass(const HumanPhysClass &);
	HumanPhysClass & operator = (const HumanPhysClass &);

};


/**
** HumanPhysDefClass - Initialization Structure/Factory/Editor Integration for a HumanPhysClass
*/
class HumanPhysDefClass : public Phys3DefClass
{
public:
	
	HumanPhysDefClass(void);
	
	// From DefinitionClass
	virtual uint32								Get_Class_ID (void) const;
	virtual PersistClass *					Create(void) const;

	// From PhysDefClass
	virtual const char *						Get_Type_Name(void)			{ return "HumanPhysDef"; }
	virtual bool								Is_Type(const char *);

	// From PersistClass
	virtual const PersistFactoryClass &	Get_Factory (void) const;
	virtual bool								Save(ChunkSaveClass &csave);
	virtual bool								Load(ChunkLoadClass &cload);

	//	Editable interface requirements
	DECLARE_EDITABLE(HumanPhysDefClass,Phys3DefClass);

protected:
	
	// No variables for now...
};

#endif

