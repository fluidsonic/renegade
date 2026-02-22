#pragma once

#include "global.h"

#include "networkobject.h"
#include "vector.h"

////////////////////////////////////////////////////////////////
//	Forward delcarations
////////////////////////////////////////////////////////////////
class StaticAnimPhysClass;
class DoorNetworkObjectClass;
class ElevatorNetworkObjectClass;
class DSAPONetworkObjectClass;

////////////////////////////////////////////////////////////////
//
//	StaticNetworkObjectClass
//
////////////////////////////////////////////////////////////////
class StaticNetworkObjectClass : public NetworkObjectClass
{
public:

	////////////////////////////////////////////////////////////////
	//	Public constructors/destructors
	////////////////////////////////////////////////////////////////
	StaticNetworkObjectClass (void);
	~StaticNetworkObjectClass (void);

	////////////////////////////////////////////////////////////////
	//	Public methods
	////////////////////////////////////////////////////////////////

	//
	//	Initialization
	//
	virtual void		Initialize (StaticAnimPhysClass *phys_obj);
	virtual void		Delete (void)	{}

	//
	//	Timestep support
	//
	virtual void		Network_Think (void);

	//
	//	Server-to-client state importing/exporting
	//
	virtual void		Import_Rare (BitStreamClass &packet);
   virtual void		Export_Rare (BitStreamClass &packet);

	//
	//	Filtering support
	//
	virtual bool		Get_World_Position (Vector3 &pos) const;
	virtual int			Get_Vis_ID (void);
	//virtual float		Compute_Object_Priority (int client_id, const Vector3 &client_pos);

	//
	//	Static methods
	//
	static void			Generate_Static_Network_Objects (void);
	static void			Free_Static_Network_Objects (void);

	// 
	// Type identification
	//
	virtual DoorNetworkObjectClass *			As_Door_Network_Object_Class( void ) 	{ return NULL; }
	virtual ElevatorNetworkObjectClass *	As_Elevator_Network_Object_Class( void ) { return NULL; }
	virtual DSAPONetworkObjectClass *		As_DSAPO_Network_Object_Class( void ) 		{ return NULL; }

	//
	//	Diagnostics 
	//
	//virtual bool		Is_Tagged(void)										{ return true; }
	static int			Get_Static_Network_Object_Count (void)			{ return StaticNetworkObjectList.Count (); }
	static const StaticNetworkObjectClass *	Get_Static_Network_Object (int index)			{ return StaticNetworkObjectList[index]; }

protected:

	////////////////////////////////////////////////////////////////
	//	Protected member data
	////////////////////////////////////////////////////////////////
	StaticAnimPhysClass *	PhysObj;

	static DynamicVectorClass<StaticNetworkObjectClass *> StaticNetworkObjectList;

private:

	////////////////////////////////////////////////////////////////
	//	Private member data
	////////////////////////////////////////////////////////////////
	int			AnimationMode;
	float			LoopStart;
	float			LoopEnd;
	float			CurrFrame;
	float			TargetFrame;
};

////////////////////////////////////////////////////////////////
//
//	DoorNetworkObjectClass
//
////////////////////////////////////////////////////////////////
class DoorNetworkObjectClass : public StaticNetworkObjectClass
{
public:

	////////////////////////////////////////////////////////////////
	//	Public constructor
	////////////////////////////////////////////////////////////////
	/*
	DoorNetworkObjectClass (void)	:
		DoorState (0)	{}
	*/
	DoorNetworkObjectClass (void);

	////////////////////////////////////////////////////////////////
	//	Public methods
	////////////////////////////////////////////////////////////////

	//
	//	Intialization
	//
	virtual void		Initialize (StaticAnimPhysClass *phys_obj);

	//
	//	Timestep support
	//
	virtual void		Network_Think (void);

	//
	//	Server-to-client state importing/exporting
	//
	virtual void		Import_Rare (BitStreamClass &packet);
   virtual void		Export_Rare (BitStreamClass &packet);

	// 
	// Type identification
	//
	virtual  DoorNetworkObjectClass *			As_Door_Network_Object_Class( void )		{ return this; }

	//
	// Diagnostics
	// 
	//virtual bool		Is_Tagged(void)										{ return false; }
	virtual void		Get_Description(StringClass & description);

private:

	////////////////////////////////////////////////////////////////
	//	Private member data
	////////////////////////////////////////////////////////////////
	int	DoorState;
};

////////////////////////////////////////////////////////////////
//
//	ElevatorNetworkObjectClass
//
////////////////////////////////////////////////////////////////
class ElevatorNetworkObjectClass : public StaticNetworkObjectClass
{
public:

	////////////////////////////////////////////////////////////////
	//	Public constructor
	////////////////////////////////////////////////////////////////
	/*
	ElevatorNetworkObjectClass (void)	:
		State (0),
		DoorStateTop (0),
		DoorStateBottom (0)	{}
	*/
	ElevatorNetworkObjectClass (void);

	//
	//	Intialization
	//
	virtual void		Initialize (StaticAnimPhysClass *phys_obj);

	//
	//	Timestep support
	//
	virtual void		Network_Think (void);

	//
	//	Server-to-client state importing/exporting
	//
	virtual void		Import_Rare (BitStreamClass &packet);
   virtual void		Export_Rare (BitStreamClass &packet);

	// 
	// Type identification
	//
	virtual  ElevatorNetworkObjectClass *	As_Elevator_Network_Object_Class( void )	{ return this; }

	//
	// Diagnostics
	// 
	//virtual bool		Is_Tagged(void)										{ return true; }
	virtual void		Get_Description(StringClass & description);

private:

	////////////////////////////////////////////////////////////////
	//	Private member data
	////////////////////////////////////////////////////////////////
	int	State;
	int	DoorStateTop;
	int	DoorStateBottom;
};

////////////////////////////////////////////////////////////////
//
//	DSAPONetworkObjectClass
//
////////////////////////////////////////////////////////////////
class DSAPONetworkObjectClass : public StaticNetworkObjectClass
{
public:

	////////////////////////////////////////////////////////////////
	//	Public constructor
	////////////////////////////////////////////////////////////////
	/*
	DSAPONetworkObjectClass (void)	:
		Health (0) {
	*/
	DSAPONetworkObjectClass (void);

	////////////////////////////////////////////////////////////////
	//	Public methods
	////////////////////////////////////////////////////////////////

	//
	//	Initialization
	//
	virtual void		Initialize (StaticAnimPhysClass *phys_obj);

	//
	//	Timestep support
	//
	virtual void		Network_Think (void);

	//
	//	Server-to-client state importing/exporting
	//
	virtual void		Import_Rare (BitStreamClass &packet);
   virtual void		Export_Rare (BitStreamClass &packet);

	// 
	// Type identification
	//
	virtual				DSAPONetworkObjectClass *		As_DSAPO_Network_Object_Class( void )		{ return this; }

private:

	////////////////////////////////////////////////////////////////
	//	Private member data
	////////////////////////////////////////////////////////////////
	float		Health;
};
