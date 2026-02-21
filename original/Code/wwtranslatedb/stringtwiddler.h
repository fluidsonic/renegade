#if defined(_MSC_VER)
#pragma once
#endif

#ifndef __STRING_TWIDDLER_H
#define __STRING_TWIDDLER_H

#include "translateobj.h"
#include "translatedb.h"
#include "vector.h"


//////////////////////////////////////////////////////////////////////////
//	Forward declarations
//////////////////////////////////////////////////////////////////////////


//////////////////////////////////////////////////////////////////////////
//
//	StringTwiddlerClass
//
//////////////////////////////////////////////////////////////////////////
class StringTwiddlerClass : public TDBObjClass
{
public:
	
	//////////////////////////////////////////////////////////////
	//	Public constructors/destructors
	//////////////////////////////////////////////////////////////
	StringTwiddlerClass (void);
	StringTwiddlerClass (const StringTwiddlerClass &src);
	virtual ~StringTwiddlerClass (void);

	//////////////////////////////////////////////////////////////
	//	Public operators
	//////////////////////////////////////////////////////////////
	const StringTwiddlerClass &	operator= (const StringTwiddlerClass &src);

	//////////////////////////////////////////////////////////////
	//	Public methods
	//////////////////////////////////////////////////////////////

	//
	//	RTTI
	//
	StringTwiddlerClass *		As_StringTwiddlerClass (void)		{ return this; }
	
	//
	// From PersistClass
	//
	const PersistFactoryClass &Get_Factory (void) const;
	bool								Save (ChunkSaveClass &csave);
	bool								Load (ChunkLoadClass &cload);

	//
	// Copy methods
	//
	TDBObjClass *					Clone (void) const					{ return new StringTwiddlerClass (*this); }

	//
	// Inherited
	//
	const WideStringClass &		Get_String (uint32 lang_id);
	const StringClass &			Get_English_String (void)		{ Randomize (); return TDBObjClass::Get_English_String (); }
	const StringClass &			Get_ID_Desc (void)				{ Randomize (); return TDBObjClass::Get_ID_Desc (); }
	uint32							Get_Sound_ID (void)				{ Randomize (); return TDBObjClass::Get_Sound_ID (); }
	const StringClass &			Get_Animation_Name (void)		{ Randomize (); return TDBObjClass::Get_Animation_Name (); }
	uint32							Get_Category_ID (void)			{ Randomize (); return TDBObjClass::Get_Category_ID (); }

	//
	//	String list access
	//	
	void								Add_String (int string_id)			{ StringList.Add (string_id); }
	void								Reset_String_List (void)			{ StringList.Delete_All (); }
	int								Get_String_Count (void) const		{ return StringList.Count (); }
	int								Get_String (int index) const		{ return StringList[index]; }
	TDBObjClass *					Lookup_String (int index);
	
protected:

	//////////////////////////////////////////////////////////////
	//	Protected methods
	//////////////////////////////////////////////////////////////
	void								Save_Variables (ChunkSaveClass &csave);
	void								Load_Variables (ChunkLoadClass &cload);

	void								Randomize (int lang_id = TranslateDBClass::LANGID_ENGLISH);

private:

	//////////////////////////////////////////////////////////////
	//	Private member data
	//////////////////////////////////////////////////////////////
	DynamicVectorClass<int>		StringList;
};


#endif //__STRING_TWIDDLER_H

