#pragma once

#include "global.h"

#include "persist.h"
#include "parameter.h"
#include "simpleparameter.h"
#include "parameterlist.h"

//////////////////////////////////////////////////////////////////////////////////
//
//	EditableClass
//
//////////////////////////////////////////////////////////////////////////////////
class EditableClass : public PersistClass
{
public:

	/////////////////////////////////////////////////////////////////////
	//	Public methods
	//
	//	Note:  These methods can be implemented in derived classes
	// by the DECLARE_EDITABLE macro.
	//
	/////////////////////////////////////////////////////////////////////
	virtual int						Get_Parameter_Count (void) const;
	virtual ParameterClass *	Lock_Parameter (int i);
	virtual void					Unlock_Parameter (int i);
};

/////////////////////////////////////////////////////////////////////
//	Get_Parameter_Count
/////////////////////////////////////////////////////////////////////
inline int
EditableClass::Get_Parameter_Count (void) const
{
	return 0;
}

/////////////////////////////////////////////////////////////////////
//	Get_Parameter
/////////////////////////////////////////////////////////////////////
inline ParameterClass *
EditableClass::Lock_Parameter (int i)
{
	return NULL;
}

/////////////////////////////////////////////////////////////////////
//	Set_Parameter
/////////////////////////////////////////////////////////////////////
inline void
EditableClass::Unlock_Parameter (int i)
{
	return ;
}


	#define DECLARE_EDITABLE(_class, _parent)
	#define EDITABLE_PARAM(_class, type, data)
	#define NAMED_EDITABLE_PARAM(_class, type, data, name)
	#define INT_EDITABLE_PARAM(_class, data, min, max) 						
	#define INT_UNITS_PARAM(_class, data, min, max, unitsname) 	   	
	#define NAMED_INT_UNITS_PARAM(_class,data,min,max,unitsname,name) 	
	#define FLOAT_EDITABLE_PARAM(_class, data, min, max) 						
	#define FLOAT_UNITS_PARAM(_class, data, min, max, unitsname) 				
	#define NAMED_FLOAT_UNITS_PARAM(_class, data, min, max, unitsname,name) 	
	#define ANGLE_EDITABLE_PARAM(_class, data, min, max) 						
	#define NAMED_ANGLE_EDITABLE_PARAM(_class, data, min, max, name) 		
	#define GENERIC_EDITABLE_PARAM(_class, param)	
	#define MODEL_DEF_PARAM(_class, data, name) 									
	#define PHYS_DEF_PARAM(_class, data, name) 										
	#define SCRIPT_PARAM(_class, name, params)
	#define SCRIPTLIST_PARAM(_class, name, name_list, param_list)
	#define ENUM_PARAM(_class, data, params) 							
	#define FILENAME_PARAM(_class, data, desc, extension) 						
	#define DEFIDLIST_PARAM(_class, data, root_class_id) 							
	#define CLASSID_DEFIDLIST_PARAM(_class, data, root_class_id, class_id, name) 	
	#define ZONE_PARAM(_class, data, name) 											
	#define PARAM_SEPARATOR(_class, name) 											
	#define GENERIC_DEFID_PARAM(_class, data, root_class_id) 						
