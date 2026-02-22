#pragma once

#include "global.h"

#include "scriptablegameobj.h"
#include "damage.h"
#include "encyclopediamgr.h"

/*
** DamageableGameObjDef - Defintion class for a DamageableGameObj
*/
class DamageableGameObjDef : public ScriptableGameObjDef
{
public:
	DamageableGameObjDef( void );

	virtual bool								Save( ChunkSaveClass &csave );
	virtual bool								Load( ChunkLoadClass &cload );

	DECLARE_EDITABLE( DamageableGameObjDef, ScriptableGameObjDef );

	int											Get_Name_ID (void) const	{ return TranslatedNameID; }

	//
	//	Encyclopedia information
	//
	EncyclopediaMgrClass::TYPE				Get_Encyclopedia_Type (void) const	{ return EncyclopediaType; }
	int											Get_Encyclopedia_ID (void) const		{ return EncyclopediaID; }

	//
	//	Icon information
	//
	const StringClass &						Get_Icon_Filename (void) const		{ return InfoIconTextureFilename; }
	int											Get_Translated_Name_ID (void) const	{ return TranslatedNameID; }

	const DefenseObjectDefClass &			Get_DefenseObjectDef( void ) const	{ return DefenseObjectDef; }

	//
	// Accessors (added as needed)
	//
	int											Get_Default_Player_Type(void) const { return DefaultPlayerType; }

protected:
	DefenseObjectDefClass						DefenseObjectDef;
	StringClass									InfoIconTextureFilename;
	int											TranslatedNameID;	
	EncyclopediaMgrClass::TYPE					EncyclopediaType;
	int											EncyclopediaID;
	bool										NotTargetable;
	int											DefaultPlayerType;

	friend	class								DamageableGameObj;
};

/*
**
*/
class DamageableGameObj : public ScriptableGameObj
{

public:
	//	Constructor and Destructor
	DamageableGameObj( void );
	virtual	~DamageableGameObj( void );

	// Definitions
	void	Init( const DamageableGameObjDef & definition );
	void	Copy_Settings( const DamageableGameObjDef & definition );
	void	Re_Init( const DamageableGameObjDef & definition );
	const DamageableGameObjDef & Get_Definition( void ) const ;

	// Save / Load
	virtual	bool	Save( ChunkSaveClass & csave );
	virtual	bool	Load( ChunkLoadClass & cload );

	DefenseObjectClass *	Get_Defense_Object( void )						{ return &DefenseObject; }
	virtual	void	Apply_Damage( const OffenseObjectClass & damager, float scale = 1.0f, int alternate_skin = -1 );
	virtual	void	Completely_Damaged( const OffenseObjectClass & damager ) {};

	// Information settings
	const StringClass & 	Get_Info_Icon_Texture_Filename( void )		{ return Get_Definition().InfoIconTextureFilename; }
	int						Get_Translated_Name_ID( void ) const		{ return Get_Definition().TranslatedNameID; }

	virtual	bool			Is_Targetable( void ) const					{ return !Get_Definition().NotTargetable; }

	virtual	bool			Is_Health_Bar_Displayed( void ) const		{ return	IsHealthBarDisplayed; }
	virtual	void			Set_Is_Health_Bar_Displayed( bool state ) { IsHealthBarDisplayed = state; }

	// Type identification
	virtual	DamageableGameObj	*As_DamageableGameObj( void )	{ return this; }

	// Teams / Playertypes
	virtual int Get_Player_Type(void) const {return PlayerType;}
	virtual void Set_Player_Type(int type);
	bool		Is_Team_Player(void);
	Vector3		Get_Team_Color(void);
	bool		Is_Teammate(DamageableGameObj * p_obj);
	bool		Is_Enemy(DamageableGameObj * p_obj);
	

	//
	// Network support
	//
	virtual	void								Import_Occasional( BitStreamClass &packet );
	virtual	void								Export_Occasional( BitStreamClass &packet );

protected:

	DefenseObjectClass	DefenseObject;

	int					PlayerType;
	bool					IsHealthBarDisplayed;
};
