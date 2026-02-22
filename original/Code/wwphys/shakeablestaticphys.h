#pragma once

#include "global.h"

#include "staticanimphys.h"

class ShakeableStaticPhysDefClass;

/**
** ShakeableStaticPhysClass
** This is a static animated physics class which plays its animation as a "shaking" effect
** when it is collided with
*/
class ShakeableStaticPhysClass : public StaticAnimPhysClass
{
public:
	//	Constructor and Destructor
	ShakeableStaticPhysClass( void );
	virtual ~ShakeableStaticPhysClass( void );

	// Definitions
	void	Init(const ShakeableStaticPhysDefClass & definition);
	const ShakeableStaticPhysDefClass * Get_ShakeableStaticPhysDef(void) const { assert( Definition ); return (ShakeableStaticPhysDefClass *)Definition; }

	// Play the animation once from start to end, typically going to be called from collision handling code
	void	Play_Animation(void);

	// Save / Load
	virtual	bool	Save(ChunkSaveClass & csave);
	virtual	bool	Load(ChunkLoadClass & cload);
	virtual	const	PersistFactoryClass & Get_Factory(void) const;

};

/*
** ShakeableStaticPhysClass
*/
class ShakeableStaticPhysDefClass : public StaticAnimPhysDefClass 
{
public:
	ShakeableStaticPhysDefClass(void);
	
	virtual uint32_t								Get_Class_ID(void) const;
	virtual const char *						Get_Type_Name(void)				{ return "ShakeableStaticPhysDef"; }
	virtual bool								Is_Type(const char *);
	virtual PersistClass *					Create(void) const ;
	virtual bool								Save(ChunkSaveClass & csave);
	virtual bool								Load(ChunkLoadClass & cload);
	virtual const PersistFactoryClass &	Get_Factory(void) const;

	DECLARE_EDITABLE( ShakeableStaticPhysDefClass, StaticAnimPhysDefClass );

protected:

	friend class								ShakeableStaticPhysClass;
};
