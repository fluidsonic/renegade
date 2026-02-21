

#ifndef BUILDINGSTATES_H
#define BUILDINGSTATES_H

#include "always.h"

/**
** BuildingStateClass
** This 'class' encapsulates some functions which work with the enumerated states that buildings can 
** have.  These enumerations are used by BuildingGameObj and BuildingAggregateClass...
**
** WARNING: Don't change these enumerations without checking the BuildingGameObj and BuildingAggregateClass
** to see what problems you might cause with save-load and their general operation...
*/
class BuildingStateClass
{
public:

	enum 
	{
		HEALTH100_POWERON				= 0,
		HEALTH75_POWERON,
		HEALTH50_POWERON,
		HEALTH25_POWERON,
		DESTROYED_POWERON,

		HEALTH100_POWEROFF,
		HEALTH75_POWEROFF,
		HEALTH50_POWEROFF,
		HEALTH25_POWEROFF,
		DESTROYED_POWEROFF,

		STATE_COUNT
	};

	enum 
	{
		HEALTH_100						= 0,
		HEALTH_75,
		HEALTH_50,
		HEALTH_25,
		HEALTH_0,
	};

	static int				Get_Health_State(int building_state);
	static int				Percentage_To_Health_State(float health);

	static bool				Is_Power_On(int building_state);
	static int				Enable_Power(int input_state,bool onoff);

	static int				Compose_State(int health_state,bool power_onoff);
	
	static const char *	Get_State_Name(int state);
};

#endif // BUILDINGSTATES_H

