#if defined(_MSC_VER)
#pragma once
#endif

#ifndef __PRESET_EXPORT_H
#define __PRESET_EXPORT_H

#include "bittype.h"
#include "vector.h"
#include "wwstring.h"


//////////////////////////////////////////////////////////////////////////
// Forward declarations
//////////////////////////////////////////////////////////////////////////
class ParameterClass;
class TextFileClass;
class PresetClass;


//////////////////////////////////////////////////////////////////////////
//
//	PresetExportClass
//
//////////////////////////////////////////////////////////////////////////
class PresetExportClass
{
public:
	
	//////////////////////////////////////////////////////////////
	//	Public constructors/destructors
	//////////////////////////////////////////////////////////////
	PresetExportClass (void)		{}
	~PresetExportClass (void)		{}

	//////////////////////////////////////////////////////////////
	//	Public methods
	//////////////////////////////////////////////////////////////
	
	//
	//	Static methods
	//
	static void			Import (const char *filename);
	static void			Export (uint32 class_id, const char *filename);

private:

	/////////////////////////////////////////////////////////////////////
	//	Private methods
	/////////////////////////////////////////////////////////////////////
	static bool			Can_Export_Parameter (ParameterClass *parameter);
	static void			Write_Column_Headers (uint32 class_id, TextFileClass &file);
	static void			Export_Preset (TextFileClass &file, PresetClass *preset);
	static int			Find_Header (TextFileClass &file);

	static ParameterClass *Find_Parameter (PresetClass *preset, const char *parameter_name);

	static bool			Validate_Columns (int class_id, DynamicVectorClass<StringClass> &column_headers);
	static bool			Read_Column_Headers (TextFileClass &file, DynamicVectorClass<StringClass> &column_headers);

	static bool			Import_Presets (TextFileClass &file, int class_id, DynamicVectorClass<StringClass> &column_headers);
	static bool			Import_Setting (PresetClass *preset, ParameterClass *parameter, const char *value);
};


#endif //__PRESET_EXPORT_H
