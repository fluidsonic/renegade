#include "colors.h"

#include "playertype.h"
#include "wwdebug.h"


Vector3 Get_Color_For_Team(int team)
{
	WWASSERT(team == PLAYERTYPE_NOD || team == PLAYERTYPE_GDI);

	if (team == PLAYERTYPE_NOD) {
		return COLOR_TEAM_0;
	} else {
		return COLOR_TEAM_1;
	}	
}
