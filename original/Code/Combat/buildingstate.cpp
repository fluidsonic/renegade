#include "global.h"
#include "buildingstate.h"

/*
** Static data used by BuildingStateClass
*/
static int _EquivalentPowerOnState[] = 
{
	BuildingStateClass::HEALTH100_POWERON,
	BuildingStateClass::HEALTH75_POWERON,
	BuildingStateClass::HEALTH50_POWERON,
	BuildingStateClass::HEALTH25_POWERON,
	BuildingStateClass::DESTROYED_POWERON,

	BuildingStateClass::HEALTH100_POWERON,
	BuildingStateClass::HEALTH75_POWERON,
	BuildingStateClass::HEALTH50_POWERON,
	BuildingStateClass::HEALTH25_POWERON,
	BuildingStateClass::DESTROYED_POWERON,

};

static int _EquivalentPowerOffState[] = 
{
	BuildingStateClass::HEALTH100_POWEROFF,
	BuildingStateClass::HEALTH75_POWEROFF,
	BuildingStateClass::HEALTH50_POWEROFF,
	BuildingStateClass::HEALTH25_POWEROFF,
	BuildingStateClass::DESTROYED_POWEROFF,

	BuildingStateClass::HEALTH100_POWEROFF,
	BuildingStateClass::HEALTH75_POWEROFF,
	BuildingStateClass::HEALTH50_POWEROFF,
	BuildingStateClass::HEALTH25_POWEROFF,
	BuildingStateClass::DESTROYED_POWEROFF,
};

static const char * _StateNames[] = 
{
	"Building State: Health 100%, Power ON",
	"Building State: Health 75%, Power ON",
	"Building State: Health 50%, Power ON",
	"Building State: Health 25%, Power ON",
	"Building State: Destroyed, Power ON",
	"Building State: Health 100%, Power OFF",
	"Building State: Health 75%, Power OFF",
	"Building State: Health 50%, Power OFF",
	"Building State: Health 25%, Power OFF",
	"Building State: Destroyed, Power OFF",
};

int BuildingStateClass::Get_Health_State(int building_state)
{
	int state = building_state;
	if (state >= HEALTH100_POWEROFF) {
		state -= HEALTH100_POWEROFF;
	}
	return state;
}

int BuildingStateClass::Percentage_To_Health_State(float health)
{
	if (health <= 0.0f) {
		return HEALTH_0;
	} 
	if (health <= 25.0f) {
		return HEALTH_25;
	}
	if (health <= 50.0f) {
		return HEALTH_50;
	}
	if (health <= 75.0f) {
		return HEALTH_75;
	}
	return HEALTH_100;
}

bool BuildingStateClass::Is_Power_On(int building_state)
{
	return (building_state < HEALTH100_POWEROFF);
}

int BuildingStateClass::Enable_Power(int input_state,bool onoff)
{
	if (onoff) {
		return _EquivalentPowerOnState[input_state];
	} else {
		return _EquivalentPowerOffState[input_state];
	}
}

int BuildingStateClass::Compose_State(int health_state,bool power_onoff)
{
	int state = health_state;
	if (power_onoff == false) {
		state += HEALTH100_POWEROFF;
	}
	return state;
}

const char * BuildingStateClass::Get_State_Name(int state)
{

	return _StateNames[state];
}

 
