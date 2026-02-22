#ifndef	POWERUP_H
#define	POWERUP_H

#ifndef	ALWAYS_H
	#include "always.h"
#endif

#ifndef SIMPLEGAMEOBJ_H
	#include "simplegameobj.h"
#endif

class		PowerUpGameObj;
class		AudibleSoundClass;
class		WeaponBagClass;

/*
** PowerUpGameObjDef - Defintion class for a PowerUpGameObj
*/
class PowerUpGameObjDef : public SimpleGameObjDef
{
public:
	PowerUpGameObjDef( void );

	virtual uint32_t								Get_Class_ID( void ) const;
	virtual PersistClass *					Create( void ) const ;
	virtual bool								Save( ChunkSaveClass &csave );
	virtual bool								Load( ChunkLoadClass &cload );
	virtual const PersistFactoryClass &	Get_Factory( void ) const;

	DECLARE_EDITABLE( PowerUpGameObjDef, SimpleGameObjDef );

	// Grant returns true if anything was granted that the grantee didn't already have
	bool											Grant( SmartGameObj * obj, PowerUpGameObj * p_powerup = NULL, bool hud_display = true ) const ;

	int											Get_Grant_Weapon_ID (void) const	{ return GrantWeaponID; }

protected:
	int											GrantShieldType;
	float											GrantShieldStrength;
	float											GrantShieldStrengthMax;
	float											GrantHealth;
	float											GrantHealthMax;
	int											GrantWeaponID;
	bool											GrantWeapon;
	int											GrantWeaponRounds;
	bool											GrantWeaponClips;
	bool											Persistent;
	//bool											IsCaptureTheFlag;
	int											GrantKey;
	bool											AlwaysAllowGrant;

	int											GrantSoundID;
	StringClass									GrantAnimationName;
	int											IdleSoundID;
	StringClass									IdleAnimationName;

	friend	class								PowerUpGameObj;

};

/*
**
*/
class PowerUpGameObj : public SimpleGameObj {

public:
				PowerUpGameObj( void );
	virtual ~PowerUpGameObj( void );

	// Definitions
	virtual	void	Init( void );
	void	Init( const PowerUpGameObjDef & definition );
	const PowerUpGameObjDef & Get_Definition( void ) const ;

	// Save / Load / Construction Factory
	virtual	bool	Save( ChunkSaveClass & csave );
	virtual	bool	Load( ChunkLoadClass & cload );
	virtual	void	On_Post_Load( void );		// MOVED
	virtual	const	PersistFactoryClass & Get_Factory( void ) const;

	// Think
	virtual	void	Think( void );
	void	Grant( SmartGameObj * obj );

	// Type identification
	virtual PowerUpGameObj		*As_PowerUpGameObj()		{ return this; }

	static	PowerUpGameObj *	Create_Backpack( ArmedGameObj * provider );

	// Network support
	virtual	bool	Is_Always_Dirty( void )					{ return false; }
	//virtual	void	Get_Extended_Information( StringClass & description );
	virtual	void	Get_Description( StringClass & description );

	void	Expire( void );

protected:

	// State types
	enum
	{
		STATE_BECOMING_IDLE			= 0,
		STATE_IDLING,
		STATE_GRANTING,
		STATE_EXPIRING,
	};

	void						Set_State( int state );
	void						Update_State( void );

	AudibleSoundClass *	IdleSoundObj;
	int						State;
	float						StateEndTimer;

	// For backpacks, which can hold multiple weapons and ammo
	WeaponBagClass *		WeaponBag;				

	friend	class			PowerUpGameObjDef;
};

#endif	// POWERUP_H

