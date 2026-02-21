#if defined(_MSC_VER)
#pragma once
#endif

#ifndef __DEFINITION_UTILS_H
#define __DEFINITION_UTILS_H

///////////////////////////////////////////////////////////////////////
// Forward declarations
///////////////////////////////////////////////////////////////////////
class DefinitionClass;
class PhysClass;


///////////////////////////////////////////////////////////////////////
//
//	Prototypes
//
///////////////////////////////////////////////////////////////////////
DefinitionClass *	Create_Physics_Definition (LPCTSTR base_class_name, bool is_temp = false);
DefinitionClass *	Create_Definition (int class_id);
void					Copy_Definition (DefinitionClass *src, DefinitionClass *dest, bool is_temp);
DefinitionClass *	Find_Physics_Definition (DefinitionClass *parent);

PhysClass *			Get_Phys_Obj_From_Definition (DefinitionClass *definition);

void					Build_Embedded_Definition_List (DEFINITION_LIST &list, DefinitionClass *parent);
void					Fix_Embedded_Definition_IDs (DefinitionClass *parent);


#endif //__DEFINITION_UTILS_H
