#pragma once

#include "global.h"

#include "decophys.h"

class TimedDecorationPhysDefClass;

/**
** TimedDecorationPhysClass
** DecorationPhysClass with a Lifetime
*/
class TimedDecorationPhysClass : public DecorationPhysClass
{
public:

	TimedDecorationPhysClass(void);
	
	void									Init(const TimedDecorationPhysDefClass & def);
	virtual bool						Needs_Timestep(void)										{ return true; }
	virtual void						Timestep(float dt);

	virtual void						Set_Lifetime( float time );
	virtual float						Get_Lifetime( void );

	virtual TimedDecorationPhysClass *	As_TimedDecorationPhysClass(void)			{ return this; }

	// save-load system
	virtual const PersistFactoryClass &	Get_Factory (void) const;
	virtual bool								Save (ChunkSaveClass &csave);
	virtual bool								Load (ChunkLoadClass &cload);		

private:

	float											Lifetime;

	// Not implemented...
	TimedDecorationPhysClass(const TimedDecorationPhysClass &);
	TimedDecorationPhysClass & operator = (const TimedDecorationPhysClass &);

};

/**
** TimedDecorationPhysDefClass
** Definition data structure for TimedDecorationPhysClass
*/
class TimedDecorationPhysDefClass : public DecorationPhysDefClass
{
public:
	
	TimedDecorationPhysDefClass(void);
	
	// From DefinitionClass
	virtual uint32_t								Get_Class_ID (void) const;
	virtual PersistClass *					Create(void) const;

	// From PhysDefClass
	virtual const char *						Get_Type_Name(void);
	virtual bool								Is_Type(const char *);

	// From PersistClass
	virtual const PersistFactoryClass &	Get_Factory (void) const;
	virtual bool								Save(ChunkSaveClass &csave);
	virtual bool								Load(ChunkLoadClass &cload);

	// accessors
	float											Get_Lifetime(void) { return Lifetime; }

	//	Editable interface requirements
	DECLARE_EDITABLE(TimedDecorationPhysDefClass,DecorationPhysDefClass);

protected:

	float											Lifetime;

	friend class TimedDecorationPhysClass;
};
