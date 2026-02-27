#pragma once

#include "global.h"

#include "definition.h"
#include "widestring.h"

///////////////////////////////////////////////////////////////////////////////////////////
//
//	PurchaseSettingsDefClass
//
///////////////////////////////////////////////////////////////////////////////////////////
class PurchaseSettingsDefClass : public DefinitionClass
{
public:

	//////////////////////////////////////////////////////////////////////////
	//	Public constants
	//////////////////////////////////////////////////////////////////////////
	typedef enum
	{
		TYPE_CLASSES		= 0,
		TYPE_VEHICLES,
		TYPE_EQUIPMENT,
		TYPE_SECRET_CLASSES,
		TYPE_SECRET_VEHICLES,
		TYPE_COUNT
	} TYPE;

	typedef enum
	{
		TEAM_GDI				= 0,
		TEAM_NOD,
		TEAM_MUTANT_GDI,
		TEAM_MUTANT_NOD,
		TEAM_COUNT
	} TEAM;
	
	//////////////////////////////////////////////////////////////////////////
	//	Public constructors/destructors
	//////////////////////////////////////////////////////////////////////////
	PurchaseSettingsDefClass (void);
	~PurchaseSettingsDefClass (void);

	//////////////////////////////////////////////////////////////////////////
	//	Public methods
	//////////////////////////////////////////////////////////////////////////

	//
	//	From DefinitionClass
	//
	virtual uint32_t								Get_Class_ID (void) const;
	virtual PersistClass *					Create (void) const ;
	virtual bool								Save (ChunkSaveClass &csave);
	virtual bool								Load (ChunkLoadClass &cload);
	virtual const PersistFactoryClass &	Get_Factory (void) const;	

	//
	//	Accessors
	//
	TYPE								Get_Type (void)							{ return Type; }
	TEAM								Get_Team (void)							{ return Team; }

	const WCHAR *					Get_Name (int index);
	int								Get_Cost (int index)						{ return CostList[index]; }
	int								Get_Definition (int index)				{ return DefinitionList[index]; }
	const StringClass &			Get_Texture (int index)					{ return TextureList[index]; }

	const WCHAR *					Get_Enlisted_Name (int index);
	int								Get_Enlisted_Definition (int index)	{ return DefinitionList[index]; }
	const StringClass &			Get_Enlisted_Texture (int index)		{ return TextureList[index]; }

	//
	//	Alternate support
	//
	int								Get_Alt_Definition (int index, int alt_index)	{ return AlternateDefinitionList[index][alt_index]; }
	const StringClass &			Get_Alt_Texture (int index, int alt_index)		{ return AlternateTextureList[index][alt_index]; }

	//
	//	Singleton access
	//
	static PurchaseSettingsDefClass *	Find_Definition (TYPE type, TEAM team);
	
	//
	//	Editable support
	//

protected:

	//////////////////////////////////////////////////////////////////////////
	//	Protected methods
	//////////////////////////////////////////////////////////////////////////
	void				Load_Variables (ChunkLoadClass &cload);

	//////////////////////////////////////////////////////////////////////////
	//	Protected constants
	//////////////////////////////////////////////////////////////////////////
	enum
	{
		ROWS	= 4,
		COLS	= 5,
		MAX_ALTERNATES = 3,
		MAX_ENTRIES = 10
	};
	
	//////////////////////////////////////////////////////////////////////////
	//	Protected member data
	//////////////////////////////////////////////////////////////////////////

	TEAM					Team;
	TYPE					Type;
	int					CostList[MAX_ENTRIES];
	int					DefinitionList[MAX_ENTRIES];
	int					NameList[MAX_ENTRIES];
	StringClass			TextureList[MAX_ENTRIES];

	int					AlternateDefinitionList[MAX_ENTRIES][MAX_ALTERNATES];
	StringClass			AlternateTextureList[MAX_ENTRIES][MAX_ALTERNATES];
	
	static PurchaseSettingsDefClass *	DefinitionArray[TYPE_COUNT][TEAM_COUNT];
};
