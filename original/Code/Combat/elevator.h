#ifndef	ELEVATOR_H
#define	ELEVATOR_H

#ifndef	ALWAYS_H
	#include "always.h"
#endif

#ifndef	__ACCESSIBLE_PHYS_H
	#include "accessiblephys.h"
#endif

#include "gameobjref.h"

class AudibleSoundClass;
class SmartGameObj;

/*
** Call zone IDs
*/
typedef enum
{
	ZONE_LOWER_CALL		= 0,
	ZONE_LOWER_INSIDE,
	ZONE_UPPER_CALL,
	ZONE_UPPER_INSIDE,
	ZONE_MAX
} ELEVATOR_ZONE;

/*
** ElevatorPhysDefClass
*/
class ElevatorPhysDefClass : public AccessiblePhysDefClass 
{
public:
	ElevatorPhysDefClass(void);
	
	virtual uint32_t								Get_Class_ID( void ) const;
	virtual const char *						Get_Type_Name(void)				{ return "ElevatorPhysDef"; }
	virtual bool								Is_Type(const char *);
	virtual PersistClass *					Create( void ) const ;
	virtual bool								Save( ChunkSaveClass &csave );
	virtual bool								Load( ChunkLoadClass &cload );
	virtual const PersistFactoryClass &	Get_Factory( void ) const;

	const OBBoxClass &						Get_Zone (ELEVATOR_ZONE id) const	{ return CallZones[id]; }

	DECLARE_EDITABLE( ElevatorPhysDefClass, AccessiblePhysDefClass );

protected:

	OBBoxClass									CallZones[4];
	float											CloseDelay;

	float											DoorClosedTop_FrameNum;
	float											DoorOpeningBottom_FrameNum;
	float											ElevatorStartTop_FrameNum;
	float											ElevatorStoppedBottom_FrameNum;

	int											DoorOpenSoundDefID;
	int											DoorCloseSoundDefID;
	int											DoorUnlockSoundDefID;
	int											DoorAccessDeniedSoundDefID;
	int											ElevatorMovingSoundDefID;

	friend	class								ElevatorPhysClass;
};

/*
** ElevatorPhysClass
*/
class	ElevatorPhysClass : public AccessiblePhysClass 
{
public:
	//	Constructor and Destructor
	ElevatorPhysClass( void );
	virtual ~ElevatorPhysClass( void );

	// RTTI
	virtual ElevatorPhysClass *	As_ElevatorPhysClass(void)	{ return this; }

	// Definitions
	void	Init( const ElevatorPhysDefClass & definition );
	const ElevatorPhysDefClass * Get_ElevatorPhysDef( void ) const { assert( Definition ); return (ElevatorPhysDefClass *)Definition; }

	// Save / Load
	virtual	bool	Save( ChunkSaveClass & csave );
	virtual	bool	Load( ChunkLoadClass & cload );
	virtual	const	PersistFactoryClass & Get_Factory( void ) const;

	virtual	void	Save_State( ChunkSaveClass & csave );
	virtual	void	Load_State( ChunkLoadClass & cload );

	// Timestep
	virtual void	Timestep( float dt ); 

	// State import/export
	static void		Set_Precision(void);

	// AI support
	bool				Can_Object_Enter (SmartGameObj *game_obj);
	bool				Can_Object_Exit (SmartGameObj *game_obj);
	void				Request_Elevator (SmartGameObj *game_obj);
	int				Get_Floor (void);
	bool				Is_Moving (void) const	{ return (State == STATE_MOVING_UP || State == STATE_MOVING_DOWN); }
	void						Set_Current_Rider (ScriptableGameObj *rider)	{ CurrentAIRider = rider; }
	ScriptableGameObj *	Get_Current_Rider (void)							{ return CurrentAIRider; }

	enum {
		STATE_DOWN						= 0,
		STATE_MOVING_UP,
		STATE_UP,
		STATE_MOVING_DOWN,
		STATE_MAX
	};

	enum {
		DOOR_STATE_NORMAL				= 0,
		DOOR_STATE_UNLOCKED,
		DOOR_STATE_ACCESS_DENIED,
		DOOR_STATE_MAX
	};

protected:

	void	Get_Door_Transform( bool is_top, Matrix3D &tm );
	void	Update_Sound_Effects( void );
	void	Play_Effect( int effect_id, bool is_top );

	void	Update_State(void);
	void	Set_State( int new_state );
	void	Set_Door_State( int new_state, int door_id );

	enum {
		TOP		= 0,
		BOTTOM	= 1
	};

	int						State;
	int						DoorStates[2];
	float						CheckTimer;
	AudibleSoundClass *	MovingSoundObj;
	float						PrevFrame;
	bool						IsCallTimerSet;
	float						CallTimer;
	int						TriggerRequest;
	GameObjReference		CurrentAIRider;

	bool		Triggered( int zone_id );

	friend class ElevatorNetworkObjectClass;
};

#endif	// ELEVATOR_H

