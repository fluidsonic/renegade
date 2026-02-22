#include "global.h"
#include "inventory.h"
#include "soldier.h"
#include "weaponbag.h"
#include "weapons.h"

/*
**
*/
InventoryClass::InventoryClass( void )
{
	Reset();
}

/*
**
*/
InventoryClass::~InventoryClass( void )
{
}

/*
**
*/
void InventoryClass::Reset( void )
{
	while ( WeaponAmmoList.Count() != 0 ) {
		WeaponAmmoList.Delete( 0 );
	}
	ShieldType = 0;
	ShieldStrength = 0.0f;
	ShieldStrengthMax = 0.0f;
	Health = 0.0f;
	HealthMax = 0.0f;
}

/*
**
*/
void	InventoryClass::Store_Inventory( SoldierGameObj * soldier )
{
	Reset();

	ShieldType = soldier->Get_Defense_Object()->Get_Shield_Type();
	ShieldStrength = soldier->Get_Defense_Object()->Get_Shield_Strength();
	ShieldStrengthMax = soldier->Get_Defense_Object()->Get_Shield_Strength_Max();
	Health = soldier->Get_Defense_Object()->Get_Health();
	HealthMax = soldier->Get_Defense_Object()->Get_Health_Max();
	soldier->Get_Weapon_Bag()->Store_Inventory( this );
}

/*
**
*/
void	InventoryClass::Add_Weapon( int id, int rounds, bool has_weapon )
{
	WeaponAmmo entry;
	entry.WeaponID = id;
	entry.AmmoCount = rounds;
	entry.HasWeapon = has_weapon;
	WeaponAmmoList.Add( entry );
}

/*
**
*/
void	InventoryClass::Restore_Inventory( SoldierGameObj * soldier )
{
	DefenseObjectClass * def_obj = soldier->Get_Defense_Object();
	if ( ShieldType > def_obj->Get_Shield_Type() ) {
		def_obj->Set_Shield_Type( ShieldType );
	}
	if ( ShieldStrengthMax > def_obj->Get_Shield_Strength_Max() ) {
		def_obj->Set_Shield_Strength_Max( ShieldStrengthMax );
	}
	if ( ShieldStrength > def_obj->Get_Shield_Strength() ) {
		def_obj->Set_Shield_Strength( ShieldStrength );
	}
	if ( HealthMax > def_obj->Get_Health_Max() ) {
		def_obj->Set_Health_Max( HealthMax );
	}
	if ( Health > def_obj->Get_Health() ) {
		def_obj->Set_Health( Health );
	}

	WeaponBagClass * bag = soldier->Get_Weapon_Bag();
	for ( int i = 0; i < WeaponAmmoList.Count(); i++ ) {
		const WeaponAmmo & entry = WeaponAmmoList[i];
		WeaponClass * 	weapon = bag->Add_Weapon( entry.WeaponID, 0, entry.HasWeapon );
		if ( entry.AmmoCount == -1 ) {
			weapon->Add_Rounds( -1 );
		} else if ( weapon->Get_Total_Rounds() < entry.AmmoCount ) {
			weapon->Add_Rounds( entry.AmmoCount - weapon->Get_Total_Rounds() );
		}

	}
}
