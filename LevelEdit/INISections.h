#if defined(_MSC_VER)
#pragma once
#endif

#ifndef __INISECTIONS_H
#define __INISECTIONS_H


/////////////////////////////////////////////////////////
//
//	Constants
//

//
//	Level export sections
//
const TCHAR * const SECTION_DEBUG				= TEXT ("Level_Debug");
const TCHAR * const SECTION_BACKGROUND			= TEXT ("Level_Background");
const TCHAR * const SECTION_TERRAIN				= TEXT ("Level_Terrain");
const TCHAR * const SECTION_TILEMAP				= TEXT ("Level_Tilemap");
const TCHAR * const SECTION_OBJECTS				= TEXT ("Level_Objects");
const TCHAR * const SECTION_WAYPATHS			= TEXT ("Level_Waypaths");
const TCHAR * const SECTION_ZONES				= TEXT ("Level_Zones");
const TCHAR * const SECTION_SOUNDS				= TEXT ("Level_Sounds");
const TCHAR * const SECTION_PRELOADS			= TEXT ("Level_Preload_Assets");
const TCHAR * const SECTION_TRANSITIONS		= TEXT ("Level_Transitions");
const TCHAR * const SECTION_MUSIC				= TEXT ("Level_Music");

//
//	Object library sections and key names
//
const TCHAR * const OBJLIBRARY_OBJ_SECTION	= TEXT ("Objects");
const TCHAR * const OBJLIBRARY_ZONE_SECTION	= TEXT ("Zones");
const TCHAR * const OBJLIBRARY_TRANS_SECTION	= TEXT ("Transitions");
const TCHAR * const OBJLIBRARY_PATH_SECTION	= TEXT ("Waypaths");
const TCHAR * const OBJLIBRARY_LIGHT_SECTION	= TEXT ("Lights");
const TCHAR * const OBJLIBRARY_SOUND_SECTION	= TEXT ("Sounds");

const TCHAR * const OBJLIBRARY_ID				= TEXT ("TypeID");
const TCHAR * const OBJLIBRARY_NAME				= TEXT ("TypeName");
const TCHAR * const OBJLIBRARY_MODEL			= TEXT ("TypeModel");
const TCHAR * const OBJLIBRARY_AGGREGATES		= TEXT ("TypeAggregates");
const TCHAR * const OBJLIBRARY_CLASS			= TEXT ("TypeClass");
const TCHAR * const OBJLIBRARY_REL_TRANS		= TEXT ("ObjTransform");
const TCHAR * const OBJLIBRARY_PARENT_ID		= TEXT ("AggParentID");

const TCHAR * const OBJLIBRARY_CLASS_OBJ		= TEXT ("Object");
const TCHAR * const OBJLIBRARY_CLASS_ZONE		= TEXT ("Zone");
const TCHAR * const OBJLIBRARY_CLASS_TRANS	= TEXT ("Transition");
const TCHAR * const OBJLIBRARY_CLASS_PATH		= TEXT ("Waypath");
const TCHAR * const OBJLIBRARY_CLASS_LIGHT	= TEXT ("Light");
const TCHAR * const OBJLIBRARY_CLASS_SOUND	= TEXT ("Sound");


#endif //__INISECTION_H

