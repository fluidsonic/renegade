
#ifndef __INPUT_CONFIG_H
#define __INPUT_CONFIG_H

#include "widestring.h"
#include "wwstring.h"

////////////////////////////////////////////////////////////////
//	Forward declarations
////////////////////////////////////////////////////////////////
class ChunkSaveClass;
class ChunkLoadClass;

////////////////////////////////////////////////////////////////
//
//	InputConfigClass
//
////////////////////////////////////////////////////////////////
class InputConfigClass
{
public:

	////////////////////////////////////////////////////////////////
	//	Public constructors/destructors
	////////////////////////////////////////////////////////////////	
	InputConfigClass (void)	:
		IsDefault (false),
		IsCustom (false)			{}

	InputConfigClass (const InputConfigClass &src)	:
		IsDefault (false),
		IsCustom (false)			{ *this = src; }

	~InputConfigClass (void)	{}

	////////////////////////////////////////////////////////////////
	//	Public operators
	////////////////////////////////////////////////////////////////
	bool operator== (const InputConfigClass &src)	{ return false; }
	bool operator!= (const InputConfigClass &src)	{ return true; }

	const InputConfigClass &operator= (const InputConfigClass &src);

	////////////////////////////////////////////////////////////////
	//	Public methods
	////////////////////////////////////////////////////////////////

	//
	//	Accessors
	//
	const WCHAR *	Get_Display_Name (void) const				{ return DisplayName; }
	const char *	Get_Filename (void) const					{ return Filename; }

	void				Set_Display_Name (const WCHAR *name)	{ DisplayName = name; }
	void				Set_Filename (const char *name)			{ Filename = name; }

	//
	//	Flags
	//
	bool				Is_Default (void) const			{ return IsDefault; }
	bool				Is_Custom (void) const			{ return IsCustom; }

	void				Set_Is_Default (bool onoff)	{ IsDefault = onoff; }
	void				Set_Is_Custom (bool onoff)		{ IsCustom = onoff; }

	//
	//	Save/load support
	//
	void				Save (ChunkSaveClass &csave);
	void				Load (ChunkLoadClass &cload);

private:

	////////////////////////////////////////////////////////////////
	//	Private methods
	////////////////////////////////////////////////////////////////
	void				Load_Variables (ChunkLoadClass &cload);

	////////////////////////////////////////////////////////////////
	//	Private member data
	////////////////////////////////////////////////////////////////	
	WideStringClass	DisplayName;
	StringClass			Filename;
	bool					IsDefault;
	bool					IsCustom;
};

#endif //__INPUT_CONFIG_H
