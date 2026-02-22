#pragma once

#include "global.h"

#include "persist.h"
#include "wwstring.h"
#include "widestring.h"
#include "vector.h"

//////////////////////////////////////////////////////////////////////////
//	Forward declarations
//////////////////////////////////////////////////////////////////////////
class ChunkSaveClass;
class ChunkLoadClass;
class StringTwiddlerClass;

//////////////////////////////////////////////////////////////////////////
//
//	TDBObjClass
//
//////////////////////////////////////////////////////////////////////////
class TDBObjClass : public PersistClass
{
public:

	//////////////////////////////////////////////////////////////
	//	Public constructors/destructors
	//////////////////////////////////////////////////////////////
	TDBObjClass (void);
	TDBObjClass (const TDBObjClass &src);
	virtual ~TDBObjClass (void);

	//////////////////////////////////////////////////////////////
	//	Public operators
	//////////////////////////////////////////////////////////////
	const TDBObjClass &	operator= (const TDBObjClass &src);

	//////////////////////////////////////////////////////////////
	//	Public methods
	//////////////////////////////////////////////////////////////

	//
	//	RTTI
	//
	virtual StringTwiddlerClass *	As_StringTwiddlerClass (void)		{ return NULL; }

	//
	// From PersistClass
	//
	const PersistFactoryClass &	Get_Factory (void) const;
	bool									Save (ChunkSaveClass &csave);
	bool									Load (ChunkLoadClass &cload);

	//
	// Copy methods
	//
	virtual TDBObjClass *				Clone (void) const					{ return new TDBObjClass (*this); }

	//
	// TranslateObj specific
	//
	virtual const WideStringClass &	Get_String (uint32_t lang_id);
	virtual const WideStringClass &	Get_String (void);
	virtual const StringClass &		Get_English_String (void)			{ return EnglishString; }
	virtual uint32_t							Get_ID (void)							{ return ID; }
	virtual const StringClass &		Get_ID_Desc (void)					{ return IDDesc; }
	virtual uint32_t							Get_Sound_ID (void)					{ return SoundID; }
	virtual const StringClass &		Get_Animation_Name (void)			{ return AnimationName; }
	virtual uint32_t							Get_Category_ID (void)				{ return CategoryID; }

	virtual void							Set_String (uint32_t lang_id, const char16_t *string);
	virtual void							Set_English_String (const char *string);
	virtual void							Set_ID (uint32_t id);
	virtual void							Set_ID_Desc (const char *desc);
	virtual void							Set_Animation_Name (const char *name)	{ AnimationName = name; }
	virtual void							Set_Sound_ID (uint32_t id)					{ SoundID = id; }
	virtual void							Set_Category_ID (uint32_t id)				{ CategoryID = id; }

	//
	//	Informational
	//
	virtual bool							Contains_Translation (uint32_t lang_id);

protected:

	//////////////////////////////////////////////////////////////
	//	Protected methods
	//////////////////////////////////////////////////////////////
	void								Save_Variables (ChunkSaveClass &csave);
	void								Load_Variables (ChunkLoadClass &cload);

	//////////////////////////////////////////////////////////////
	//	Protected member data
	//////////////////////////////////////////////////////////////
	StringClass									EnglishString;
	StringClass									IDDesc;
	uint32_t										ID;
	uint32_t										SoundID;
	uint32_t										CategoryID;
	StringClass									AnimationName;
	DynamicVectorClass<WideStringClass>	TranslatedStrings;
};
