#if defined(_MSC_VER)
#pragma once
#endif

#ifndef __NODE_CATEGORIES_H
#define __NODE_CATEGORIES_H

#include "definitionclassids.h"
#include "icons.h"
#include "editorchunkids.h"
#include "combatchunkid.h"


////////////////////////////////////////////////////////////////////////////////////
//
//	Structures and tables
//
////////////////////////////////////////////////////////////////////////////////////
typedef struct
{
	const char *name;
	int			clsid;
	int			icon;
} CATEGORY_INFO;

const CATEGORY_INFO PRESET_CATEGORIES[] = 
{
	{ "Terrain",			CLASSID_TERRAIN,				TERRAIN_ICON },
	{ "Tile",				CLASSID_TILE,					TILE_ICON },
	{ "Object",				CLASSID_GAME_OBJECTS,		OBJECT_ICON },
	{ "Buildings",			CLASSID_BUILDINGS,			BUILDING_ICON },
	{ "Munitions",			CLASSID_MUNITIONS,			OBJECT_ICON },	
	{ "Dummy Object",		CLASSID_DUMMY_OBJECTS,		OBJECT_ICON },	
	{ "Cover Spots",		CLASSID_COVERSPOT,			OBJECT_ICON },	
	{ "Light",				CLASSID_LIGHT,					LIGHT_ICON },
	{ "Sound",				CLASSID_SOUND,					SOUND_ICON },	
	{ "Waypath",			CLASSID_WAYPATH,				WAYPATH_ICON },
	{ "Twiddlers",			CLASSID_TWIDDLERS,			RAND_ICON },
	{ "Editor Objects",	CLASSID_EDITOR_OBJECTS,		VIS_ICON },
	{ "Global Settings",	CLASSID_GLOBAL_SETTINGS,	OBJECT_ICON },	
};

const int PRESET_CATEGORY_COUNT = sizeof (PRESET_CATEGORIES) / sizeof (CATEGORY_INFO);


#endif //__NODE_CATEGORIES_H
