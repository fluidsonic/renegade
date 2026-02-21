#ifndef	INVENTORY_H
#define	INVENTORY_H

#ifndef	ALWAYS_H
	#include "always.h"
#endif

#ifndef	SIMPLEVEC_H
	#include "simplevec.h"
#endif

#ifndef	DAMAGE_H
	#include "damage.h"
#endif

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

#endif


