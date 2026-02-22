#pragma once

#include "global.h"


#include "wheelvehicle.h"

class MotorcycleDefClass;

/**
** MotorcycleClass
** This is a derivation of WheeledVehicleClass which adds self-balancing and
** leaning into turns.
*/
class MotorcycleClass : public WheeledVehicleClass
{
public:

	MotorcycleClass(void);
	virtual ~MotorcycleClass(void);
	virtual MotorcycleClass *	As_MotorcycleClass(void)	{ return this; }

	void								Init(const MotorcycleDefClass & def);

	// State variables
	float								Get_Lean_Value(void)			{ return LeanValue; }
	
	// Save-Load system
	virtual const PersistFactoryClass &	Get_Factory (void) const;
	virtual bool								Save (ChunkSaveClass &csave);
	virtual bool								Load (ChunkLoadClass &cload);

protected:

	float								LeanK0;
	float								LeanK1;
	float								LeanValue;	// how much the character should be leaning to balance the bike

	virtual void					Compute_Force_And_Torque(Vector3 * force,Vector3 * torque);
	virtual void					Compute_Inertia(void);

private:

	MotorcycleClass(const MotorcycleClass &);
	MotorcycleClass & operator = (const MotorcycleClass &);

};

/*
** MotorcycleDefClass - Initialization Structure/Factory/Editor Integration for a MotorcycleClass
*/
class MotorcycleDefClass : public WheeledVehicleDefClass
{
public:
	
	MotorcycleDefClass (void);
	
	// From DefinitionClass
	virtual uint32_t								Get_Class_ID (void) const;
	virtual PersistClass *					Create(void) const;

	// From PhysDefClass
	virtual const char *						Get_Type_Name(void)			{ return "MotorcycleDef"; }
	virtual bool								Is_Type(const char *);

	// From PersistClass
	virtual const PersistFactoryClass &	Get_Factory (void) const;
	virtual bool								Save(ChunkSaveClass &csave);
	virtual bool								Load(ChunkLoadClass &cload);

	//	Editable interface requirements
	DECLARE_EDITABLE(MotorcycleDefClass,WheeledVehicleDefClass);

protected:

	float				LeanK0;
	float				LeanK1;

	friend class MotorcycleClass;
};
