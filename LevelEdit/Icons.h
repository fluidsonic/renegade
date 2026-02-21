#if defined(_MSC_VER)
#pragma once
#endif


#ifndef __ICONS_H
#define __ICONS_H

#include "Resource.H"

///////////////////////////////////////////////////////////////
//
//	Constants
//
typedef enum
{
	FOLDER_ICON			= 0,
	OPEN_FOLDER_ICON,
	OBJECT_ICON,
	TILE_ICON,
	TERRAIN_ICON,
	LIGHT_ICON,
	ZONE_ICON,
	TRANSITION_ICON,
	WAYPATH_ICON,
	SOUND_ICON,
	TEMP_ICON,
	FILE_ICON,
	FILES_ICON,	
	VIS_ICON,
	PATHFIND_ICON,
	NAVIGATOR_ICON,
	PLUS_OVERLAY_ICON,
	BUILDING_ICON,
	NULL_ICON,
	RAND_ICON,
	DIALOGUE_ICON,
	ICON_COUNT

} ICON_INDEX;

extern const UINT ICON_RESOURCE_IDS[ICON_COUNT];

#endif //__ICONS_H

