#pragma once

#include "global.h"


#include "simplevec.h"

#include "damage.h"

class	SoldierGameObj;

/*
** InventoryClass is a black box used to remember the inventory of a soldier across levels
*/
class	InventoryClass {

public:
	InventoryClass( void );
	~InventoryClass( void );

	void	Reset( void );

	void	Store_Inventory( SoldierGameObj * soldier );
	void	Restore_Inventory( SoldierGameObj * soldier );

	void	Add_Weapon( int id, int rounds, bool has_weapon );

private:

	struct WeaponAmmo {
		int	WeaponID;
		int	AmmoCount;
		bool	HasWeapon;
	};

	SimpleDynVecClass<WeaponAmmo>	WeaponAmmoList;
	ArmorType	ShieldType;
	float			ShieldStrength;
	float			ShieldStrengthMax;
	float			Health;
	float			HealthMax;
};
