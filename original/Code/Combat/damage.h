#ifndef	DAMAGE_H
#define	DAMAGE_H

#ifndef	ALWAYS_H
	#include "always.h"
#endif

#ifndef	GAMEOBJREF_H
	#include "gameobjref.h"
#endif

#ifndef _DATASAFE_H
	#include	"datasafe.h"
#endif //_DATASAFE_H

#include "editable.h"

/*
**
*/
typedef	unsigned int	ArmorType;
typedef	unsigned int	WarheadType;
typedef	safe_unsigned_int	SafeArmorType;
typedef	safe_unsigned_int	SafeWarheadType;
class		ArmedGameObj;
class		DamageableGameObj;
class		BitStreamClass;
class		ChunkSaveClass;
class		ChunkLoadClass;

/*
**
*/
class	ArmorWarheadManager {

public:

	// Build
	static void	Init( void );
	static void Shutdown( void );

	static bool				Is_Armor_Soft( ArmorType armor );

	// Type additions/access
	static int			 	Get_Num_Armor_Types( void );
	static ArmorType		Get_Armor_Type( const char *name );
	static const char * 	Get_Armor_Name( ArmorType type );
	static int			 	Get_Num_Warhead_Types( void );
	static WarheadType	Get_Warhead_Type( const char *name );
	static const char *	Get_Warhead_Name( WarheadType type );

	// Save IDs
	static int			 	Get_Armor_Save_ID( ArmorType type );
	static ArmorType	 	Find_Armor_Save_ID( int id );
	static int			 	Get_Warhead_Save_ID( WarheadType type );
	static ArmorType	 	Find_Warhead_Save_ID( int id );

	// Damage Multiplier Settings
//	static void		Set_Damage_Multiplier( ArmorType armor, WarheadType warhead, float multiplier );
	static float	Get_Damage_Multiplier( ArmorType armor, WarheadType warhead );

	// Shield Absorbsion Settings
//	static void		Set_Shield_Absorbsion( ArmorType armor, WarheadType warhead, float percent );
	static float	Get_Shield_Absorbsion( ArmorType armor, WarheadType warhead );

	typedef enum {
		SPECIAL_DAMAGE_TYPE_NONE,
		SPECIAL_DAMAGE_TYPE_FIRE,
		SPECIAL_DAMAGE_TYPE_CHEM,
		SPECIAL_DAMAGE_TYPE_ELECTRIC,
		SPECIAL_DAMAGE_TYPE_CNC_FIRE,
		SPECIAL_DAMAGE_TYPE_CNC_CHEM,
		SPECIAL_DAMAGE_TYPE_SUPER_FIRE,
		NUM_SPECIAL_DAMAGE_TYPES,			// Keep synced with ouch_types
	} SpecialDamageType;

	static SpecialDamageType	Get_Special_Damage_Type( WarheadType type );
	static float					Get_Special_Damage_Probability( WarheadType type );

	static WarheadType 			Get_Special_Damage_Warhead( SpecialDamageType type );
	static float					Get_Special_Damage_Duration( SpecialDamageType type );
	static float					Get_Special_Damage_Scale( SpecialDamageType type );
	static const char *			Get_Special_Damage_Explosion( SpecialDamageType type );

	static float					Get_Visceroid_Probability( WarheadType type );

	static bool						Is_Skin_Impervious( SpecialDamageType type, ArmorType skin );

private:
	static	safe_float			*Multipliers;
	static	safe_float			*Absorbsion;
};

/*
**
*/
class OffenseObjectClass {

#define	DEFAULT_DAMAGE		1.0f

public:

	// Constructors & Destructor
	OffenseObjectClass( float damage = DEFAULT_DAMAGE, WarheadType warhead = 0, ArmedGameObj *owner = NULL ) : \
							Damage( damage ), Warhead( warhead ), ForceServerDamage( false ), EnableClientDamage( false ) { Set_Owner( owner ); }
	OffenseObjectClass( const OffenseObjectClass & base );
	~OffenseObjectClass() { Set_Owner( NULL ); };

	bool				Save( ChunkSaveClass & csave );
	bool				Load( ChunkLoadClass & cload );

	// Offensive Damage Rating (ODR)
	void	Set_Damage( float damage )	{ Damage = damage;	}
	float	Get_Damage( void ) const 			{ return Damage;	}

	// Warhead
	void	Set_Warhead( WarheadType warhead )	{ Warhead = warhead;	}
	WarheadType	Get_Warhead( void ) const 				{ return Warhead;	}

	// Owner
	void	Set_Owner( ArmedGameObj *owner )	{ Owner = (ScriptableGameObj *)owner; }
	ArmedGameObj *Get_Owner( void ) const	{ return (ArmedGameObj *)Owner.Get_Ptr();	}

	// Special flag to force servers to apply damage.  Only needed for client authoratative damage
	bool							ForceServerDamage;

	// Special flag to disable client damage.  So it only works for bullets.  Only needed for client authoratative damage
	bool							EnableClientDamage;

private:
//	safe_float						Damage;
	float								Damage;
	WarheadType						Warhead;
	GameObjReference				Owner;

};

/**
** DefenseObjectClass
** This is a class that is meant to be embedded inside a game object and handles all health/damage
** tracking.  Also, the definition for the game object which embeds this object can embed a
** DefenseObjectDefClass to automatically get all of the settings.
**
*/
class DefenseObjectDefClass;

class DefenseObjectClass {

#define	DEFAULT_HEALTH		100.0f

public:

	// Constructors & Destructor
	DefenseObjectClass( float health = DEFAULT_HEALTH, ArmorType skin = 0 ) :
      Health( health ), HealthMax( health ), Skin( skin ), ShieldStrength( 0 ), ShieldStrengthMax( 0 ), ShieldType( 0 ), CanObjectDie( true ) {};//,
      //LastSentHealth( -1 ), LastSentSkin( -1 ), LastSentShieldStrength( -1 ), LastSentShieldType( -1 )  {};
	~DefenseObjectClass() {};

	// DefenseObjects now have a pointer to their cooresponing GameObj to report damage and scoring to the PlayerData
	void				Init( const DefenseObjectDefClass & def, DamageableGameObj * owner);
	bool				Save( ChunkSaveClass & csave );
	bool				Load( ChunkLoadClass & cload );

	// Owner
	void	Set_Owner( DamageableGameObj *owner )	{ Owner = (ScriptableGameObj *)owner; }
	DamageableGameObj *Get_Owner( void ) const	{ return (DamageableGameObj *)Owner.Get_Ptr();	}

	// Skin
	void	Set_Skin( ArmorType skin )		{ Skin = skin; }
	ArmorType	Get_Skin( void ) const 	{ return Skin;	}

	enum {MAX_MAX_HEALTH					= 2000};//500};
	enum {MAX_MAX_SHIELD_STRENGTH		= 2000};//500};

	// Health
	void				Set_Health(float health);
	void				Add_Health(float add_health);
	float				Get_Health(void) const ;
	void				Set_Health_Max(float health);
	float				Get_Health_Max(void) const ;

	// Shield
	void				Set_Shield_Strength(float str);
	void				Add_Shield_Strength(float str);
	float				Get_Shield_Strength(void) const ;
	void				Set_Shield_Strength_Max(float str);
	float				Get_Shield_Strength_Max(void) const ;

	void				Set_Shield_Type( ArmorType type );
	ArmorType	Get_Shield_Type( void ) const 		{ return ShieldType; }

	// Apply Damage
	float			Apply_Damage( const OffenseObjectClass	& offense, float scale = 1.0f, int alternate_skin = -1 );
	float			Do_Damage( const OffenseObjectClass	& offense, float scale = 1.0f, int alternate_skin = -1 );

	// Request_Damage
	void			Request_Damage( const OffenseObjectClass & offense, float scale = 1.0f );

	// Will an apply damage call actually repair?
	bool			Is_Repair( const OffenseObjectClass	& offense, float scale = 1.0f );

	// Would an apply damage call actually damage?
	bool			Would_Damage( const OffenseObjectClass	& offense, float scale = 1.0f );

	bool			Is_Soft( void );
	void			Set_Can_Object_Die( bool onoff )	{ CanObjectDie = onoff; }

	// State import/export
   virtual	void	Import( BitStreamClass & packet );
   virtual	void	Export( BitStreamClass & packet );

	static void Set_Precision(void);

	float			Get_Damage_Points(void) const			{ return DamagePoints; }
	float			Get_Death_Points(void) const			{ return DeathPoints; }

private:
	safe_float		Health;
	safe_float		HealthMax;
	SafeArmorType	Skin;
	safe_float		ShieldStrength;
	safe_float		ShieldStrengthMax;
	SafeArmorType	ShieldType;
	safe_float		DamagePoints;
	safe_float		DeathPoints;
	bool				CanObjectDie;

	GameObjReference	Owner;

	void				Mark_Owner_Dirty( void );

};

/**
** DefenseObjectDefClass
** This class is meant to be a component of a definition class for a game or physics
** object which contains a DefenseObject.  Use the associated macro to make all of
** the member variables editable in your class.
*/
class DefenseObjectDefClass
{
public:
	DefenseObjectDefClass(void);
	~DefenseObjectDefClass(void);

	bool			Save(ChunkSaveClass &csave);
	bool			Load(ChunkLoadClass &cload);

public:
	safe_float		Health;
	safe_float		HealthMax;
	SafeArmorType	Skin;
	safe_float		ShieldStrength;
	safe_float		ShieldStrengthMax;
	SafeArmorType	ShieldType;
	safe_float		DamagePoints;
	safe_float		DeathPoints;
};

/*
** Use this macro to make all of the member variables in a DefenseObjectDefClass editable.
** The first parameter to the macro is the type-name of your class (e.g. PhysicalGameObjectDef) and
** the second parameter is the name of the member variable which is the defense object def.
*/

#define	DEFENSEOBJECTDEF_EDITABLE_PARAMS( class_name, member_name )

#endif

