#include "colors.h"

#include "playertype.h"

Vector3 Get_Color_For_Team(int team)
{

	if (team == PLAYERTYPE_NOD) {
		return COLOR_TEAM_0;
	} else {
		return COLOR_TEAM_1;
	}	
}
