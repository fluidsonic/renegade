#ifndef	CONTROL_H
#define	CONTROL_H

#ifndef	ALWAYS_H
	#include "always.h"
#endif

#ifndef WWMATH_H
#include "wwmath.h"
#endif

#ifndef WWDEBUG_H
#endif

/*
**
*/
class BitStreamClass;
class	ChunkSaveClass;
class	ChunkLoadClass;

/*
** ControlClass
*/
class ControlClass {

public:

	typedef enum {
		BOOLEAN_ONE_TIME_FIRST,
		BOOLEAN_JUMP					=	BOOLEAN_ONE_TIME_FIRST,
//		BOOLEAN_CROUCH,
		BOOLEAN_WEAPON_NEXT,
		BOOLEAN_WEAPON_PREV,
		BOOLEAN_WEAPON_RELOAD,
		BOOLEAN_WEAPON_USE,
		BOOLEAN_DIVE_FORWARD,
		BOOLEAN_DIVE_BACKWARD,
		BOOLEAN_DIVE_LEFT,
		BOOLEAN_DIVE_RIGHT,
		BOOLEAN_ACTION,						// was LADDER
		BOOLEAN_SELECT_NO_WEAPON,
		BOOLEAN_SELECT_WEAPON_0,
		BOOLEAN_SELECT_WEAPON_1,
		BOOLEAN_SELECT_WEAPON_2,
		BOOLEAN_SELECT_WEAPON_3,
		BOOLEAN_SELECT_WEAPON_4,
		BOOLEAN_SELECT_WEAPON_5,
		BOOLEAN_SELECT_WEAPON_6,
		BOOLEAN_SELECT_WEAPON_7,
		BOOLEAN_SELECT_WEAPON_8,
		BOOLEAN_SELECT_WEAPON_9,
		BOOLEAN_DROP_FLAG,
		BOOLEAN_VEHICLE_TOGGLE_GUNNER,

		BOOLEAN_CONTINUOUS_FIRST,
		BOOLEAN_WEAPON_FIRE_PRIMARY		=	BOOLEAN_CONTINUOUS_FIRST,
		BOOLEAN_WEAPON_FIRE_SECONDARY,
		BOOLEAN_WALK,
		BOOLEAN_CROUCH,
		BOOLEAN_TOTAL,				// Number of boolean channels

		NUM_BOOLEAN_ONE_TIME		= 	BOOLEAN_CONTINUOUS_FIRST - BOOLEAN_ONE_TIME_FIRST,
		NUM_BOOLEAN_CONTINUOUS	= 	BOOLEAN_TOTAL - BOOLEAN_CONTINUOUS_FIRST,
	} BooleanControl;

	typedef enum {
		ANALOG_MOVE_FORWARD,
		ANALOG_MOVE_LEFT,
		ANALOG_MOVE_UP,
		ANALOG_TURN_LEFT,
		ANALOG_CONTROL_COUNT				// Number of analog channels
	} AnalogControl;

	ControlClass( void )	: PendingOneTimeBooleanBits( 0 )	{ Clear_Control(); }
	~ControlClass( void )	{}

	bool				Save( ChunkSaveClass & csave );
	bool				Load( ChunkLoadClass & cload );

	void	Clear_Control( void );
	void	Clear_Boolean( void );
	ControlClass & operator = (const ControlClass & src);

	// Boolean Controls
	void	Set_Boolean( BooleanControl control, bool state = true );
	bool	Get_Boolean( BooleanControl control );

	void	Clear_One_Time_Boolean( void )				{OneTimeBooleanBits = 0;}
	unsigned	long Get_One_Time_Boolean_Bits(void)	{return OneTimeBooleanBits;}
	unsigned	char Get_Continuous_Boolean_Bits(void) {return ContinuousBooleanBits;}

	// Analog Controls
	void	Set_Analog( AnalogControl control, float value );
	float	Get_Analog( AnalogControl control );
	//float	Get_Clamp( AnalogControl control );
   //unsigned char  Scale_Analog(float clamp, float unscaled);
   //float Unscale_Analog(float clamp, unsigned char scaled);

	// Import & Export
   void	Import_Cs( BitStreamClass & packet );
   void	Export_Cs( BitStreamClass & packet );
   void	Import_Sc( BitStreamClass & packet );
   void	Export_Sc( BitStreamClass & packet );

	static void Set_Precision(void);

private:
	unsigned		int	OneTimeBooleanBits;
	unsigned		int	PendingOneTimeBooleanBits;
	unsigned		char	ContinuousBooleanBits;
	unsigned		char	PendingContinuousBooleanBits;
	float			AnalogValues[ ANALOG_CONTROL_COUNT ];
};

inline void	ControlClass::Set_Analog( AnalogControl control, float value )	
{
	AnalogValues[ control ] = value; 
}

inline float ControlClass::Get_Analog( AnalogControl control )
{ 
	return AnalogValues[ control ]; 
}

#endif	//	CONTROL_H

