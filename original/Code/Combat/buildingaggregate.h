#pragma once

#include "global.h"

#include "staticanimphys.h"
#include "buildingstate.h"

class BuildingAggregateDefClass;

/**
** BuildingAggregateClass (BAG)
** This class implements the behavior of a "building aggregate".  Building aggregates are animated 
** static objects which display segments of their animation depending on the state of the building.  
*/
class	BuildingAggregateClass : public StaticAnimPhysClass 
{
public:

	//	Constructor and Destructor
	BuildingAggregateClass( void );
	virtual ~BuildingAggregateClass( void );

	// Definitions
	void	Init( const BuildingAggregateDefClass & definition );
	const BuildingAggregateDefClass * Get_BuildingAggregateDef( void ) const { assert( Definition ); return (BuildingAggregateDefClass *)Definition; }

	// State changing
	int				Get_Current_State(void);
	void				Set_Current_State(int new_state,bool force_update = false);
	bool				Is_MCT(void);

	// Save / Load
	virtual	bool	Save( ChunkSaveClass & csave );
	virtual	bool	Load( ChunkLoadClass & cload );

	virtual	void	Save_State( ChunkSaveClass & csave );
	virtual	void	Load_State( ChunkLoadClass & cload );

	virtual	void	On_Post_Load( void );
	virtual	const	PersistFactoryClass & Get_Factory( void ) const;

protected:
	
	bool				Is_Power_On(int state);

	int				CurrentState;

};

/**
** BuildingAggregateDefClass (BAGDef!)
** The data contained in this class defines the behavior of a "building aggregate".  Building
** aggregates are animated static objects which display segments of their animation depending
** on the state of the building.  
** Obviously, this is a very special case object; designed specifically to provide a set of
** features needed by the building logic in Renegade.
*/
class BuildingAggregateDefClass : public StaticAnimPhysDefClass 
{
public:
	BuildingAggregateDefClass(void);
	
	virtual uint32_t								Get_Class_ID( void ) const;
	virtual const char *						Get_Type_Name(void)				{ return "BuildingAggregateDef"; }
	virtual bool								Is_Type(const char *);
	virtual PersistClass *					Create( void ) const ;
	virtual bool								Save( ChunkSaveClass &csave );
	virtual bool								Load( ChunkLoadClass &cload );
	virtual const PersistFactoryClass &	Get_Factory( void ) const;

	DECLARE_EDITABLE( BuildingAggregateDefClass, StaticAnimPhysDefClass );

protected:

	bool											Save_State_Animation_Data(ChunkSaveClass & csave,int state_index);
	bool											Load_State_Animation_Data(ChunkLoadClass & cload,int state_index);

	/*
	** Animation Logic Setting
	** ANIM_LOGIC_LINEAR - Frame0 is used for each state, if the damage state changes, the animation
	**		is played to the new state's Frame0.  If the power state changes, we pop to the same relative
	**    position in the alternate power state.
	** ANIM_LOGIC_LOOP - In this mode, each state has a pair of frames which it loops between.  Whenever
	**		the state changes, we pop to the same relative position in the new loop.  In addition, certain
	**		states can have animation disabled; this will cause the object to freeze on the appropriate frame
	**		right after the state change.  
	*/
	enum 
	{
		ANIM_LOGIC_LINEAR						= 0,
		ANIM_LOGIC_LOOP,
		ANIM_LOGIC_SEQUENCE,
	};

	int											AnimLogicMode;
	bool											IsMCT;
	
	/*
	** Animation controls for each state.  In LOOP mode, we use Frame0 and Frame1 to define a loop.  In
	** Linear mode, only Frame0 is used.
	*/
	int											Frame0[BuildingStateClass::STATE_COUNT];
	int											Frame1[BuildingStateClass::STATE_COUNT];
	bool											AnimationEnabled[BuildingStateClass::STATE_COUNT];

	friend class BuildingAggregateClass;
};
