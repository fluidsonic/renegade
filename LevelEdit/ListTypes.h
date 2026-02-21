#if defined(_MSC_VER)
#pragma once
#endif

#ifndef __LISTTYPES_H
#define __LISTTYPES_H

#include "UniqueList.H"

//////////////////////////////////////////////////////////////////////////
//	Typedefs and structs
//////////////////////////////////////////////////////////////////////////
typedef UniqueListClass<class GroupMgrClass *>				GROUP_LIST;
typedef UniqueListClass<class NodeClass *>					NODE_LIST;

typedef DynamicVectorClass<CString>								STRING_LIST;
typedef DynamicVectorClass<class EditScriptClass *>		SCRIPT_LIST;

typedef DynamicVectorClass<class DefinitionClass *>		DEFINITION_LIST;
typedef DynamicVectorClass<class PresetClass *>				PRESET_LIST;
typedef DynamicVectorClass<class PresetListNode *>			PRESET_NODE_LIST;

typedef DynamicVectorClass<class ZoneParameterClass *>	ZONE_PARAM_LIST;

#endif //__LISTTYPES_H
