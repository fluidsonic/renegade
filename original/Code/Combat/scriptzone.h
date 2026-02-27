#pragma once

#include "global.h"


#include "scriptablegameobj.h"

#include "obbox.h"

#include "slist.h"

/*
** ZoneConstants
** Convienent namespace declaration for the constants used with zones
*/
namespace ZoneConstants
{
	typedef enum
	{
		TYPE_DEFAULT					= 0,
		TYPE_CTF,
		TYPE_VEHICLE_CONSTRUCTION,
		TYPE_VEHICLE_REPAIR,
		TYPE_TIBERIUM_FIELD,
		TYPE_BEACON,
		TYPE_GDI_TIB_FIELD,
		TYPE_NOD_TIB_FIELD,
		TYPE_COUNT
	} ZoneType;
}

/*
** ZoneGameObjDef - Defintion class for a ZoneGameObj
*/
class ScriptZoneGameObjDef : public ScriptableGameObjDef
{
public:
	ScriptZoneGameObjDef( void );

	virtual uint32_t								Get_Class_ID( void ) const;
	virtual PersistClass *					Create( void ) const ;
	virtual bool								Save( ChunkSaveClass &csave );
	virtual bool								Load( ChunkLoadClass &cload );
	virtual bool								Is_Valid_Config (StringClass &message) { return true; }
	virtual const PersistFactoryClass &	Get_Factory( void ) const;


	const Vector3 &							Get_Color (void) const { return Color; }

	ZoneConstants::ZoneType					Get_Type (void) const	{ return ZoneType; }

protected:

	ZoneConstants::ZoneType					ZoneType;
	Vector3										Color;
	bool											IsCTFZone;
	bool											CheckStarsOnly;
	bool											IsEnvironmentZone;
	friend	class								ScriptZoneGameObj;
};

/*
**
*/
class ScriptZoneGameObj : public ScriptableGameObj {

public:
	ScriptZoneGameObj();
	virtual	~ScriptZoneGameObj();

	// Definitions
	virtual	void	Init( void );
	void	Init( const ScriptZoneGameObjDef & definition );
	const ScriptZoneGameObjDef & Get_Definition( void ) const;
	
	// RTTI
	ScriptZoneGameObj	*As_ScriptZoneGameObj( void )	{ return this; };

	// Save / Load / Construction Factory
	virtual	bool	Save( ChunkSaveClass & csave );
	virtual	bool	Load( ChunkLoadClass & cload );
	virtual	const	PersistFactoryClass & Get_Factory( void ) const;

	//	Thinking
	virtual	void		Think();

	virtual	void		Get_Position(Vector3 * set_pos) const { *set_pos = BoundingBox.Center; }

	// Bounding Box
	void	Set_Bounding_Box( OBBoxClass & box )			{ BoundingBox = box; }
	const OBBoxClass & Get_Bounding_Box(void)				{ return BoundingBox; }

	// PlayerType (a simple copy of what's used in PhyiscalGameObj, needed for CTF
   int		Get_Player_Type( void) const					{ return PlayerType; }
   void		Set_Player_Type( int type )					{ PlayerType = type; }

   int		Count_Team_Members_Inside( int player_type );

	static ScriptZoneGameObj *	Find_Closest_Zone (const Vector3 &pos, ZoneConstants::ZoneType type);

	// Network support
	//virtual bool	Exists_On_Client( void ) const;

   bool	Is_Environment_Zone( void )						{ return Get_Definition().IsEnvironmentZone; }

protected:
	OBBoxClass			BoundingBox;
	int					PlayerType;

	// a list of all SMART objects currently inside me and a checking function
	SList<GameObjReference>	  	InsideList;
	void		Entered( SmartGameObj * obj );
	bool		In_List( SmartGameObj * obj );
	bool		Inside_Me( const SmartGameObj * obj );

};
