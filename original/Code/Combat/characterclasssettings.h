#pragma once

#include "global.h"

#include "definition.h"

///////////////////////////////////////////////////////////////////////////////////////////
//
//	CharacterClassSettingsDefClass
//
///////////////////////////////////////////////////////////////////////////////////////////
class CharacterClassSettingsDefClass : public DefinitionClass
{
public:

	//////////////////////////////////////////////////////////////////////////
	//	Public constants
	//////////////////////////////////////////////////////////////////////////
	typedef enum
	{
		CLASS_MINIGUNNER			= 0,
		CLASS_ROCKET_SOLDIER,
		CLASS_GRENADIER,
		CLASS_ENGINEER,
		CLASS_FLAME_THROWER,
		CLASS_MUTANT,
		CLASS_COUNT
	} CLASS;

	typedef enum
	{
		RANK_ENLISTED				= 0,
		RANK_OFFICER,
		RANK_SPECIAL_FORCES,
		RANK_BOSS,
		RANK_COUNT
	} RANK;

	typedef enum
	{
		TEAM_GDI				= 0,
		TEAM_NOD,
		TEAM_COUNT
	} TEAM;

	
	//////////////////////////////////////////////////////////////////////////
	//	Public constructors/destructors
	//////////////////////////////////////////////////////////////////////////
	CharacterClassSettingsDefClass (void);
	~CharacterClassSettingsDefClass (void);

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

	static CharacterClassSettingsDefClass *			Get_Instance (void)	{ return GlobalInstance; }	

	//
	//	Accessors
	//
	int								Get_Cost (CLASS char_class, RANK char_rank, TEAM char_team);
	int								Get_Definition (CLASS char_class, RANK char_rank, TEAM char_team);

	
	//
	//	Editable support
	//
	DECLARE_EDITABLE (CharacterClassSettingsDefClass, DefinitionClass);

protected:
	
	//////////////////////////////////////////////////////////////////////////
	//	Protected member data
	//////////////////////////////////////////////////////////////////////////

	int					CostTable[CLASS_COUNT][RANK_COUNT][TEAM_COUNT];
	int					DefinitionTable[CLASS_COUNT][RANK_COUNT][TEAM_COUNT];

	static CharacterClassSettingsDefClass *			GlobalInstance;
};
