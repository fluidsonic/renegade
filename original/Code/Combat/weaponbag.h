#pragma once

#include "global.h"


#include "vector.h"

/*
**
*/
class	WeaponClass;
class	ChunkSaveClass;
class	ChunkLoadClass;
class	ArmedGameObj;
class	WeaponDefinitionClass;
class	AmmoDefinitionClass;
class BitStreamClass;
class InventoryClass;

/*
** WeaponBags manage collections of weapons
*/
class	WeaponBagClass {

public:
	WeaponBagClass( ArmedGameObj * owner );
	~WeaponBagClass( void );

	bool				Save( ChunkSaveClass & csave );
	bool				Load( ChunkLoadClass & cload );

	bool				Is_Weapon_Owned( int weapon_id );
	bool				Is_Ammo_Full( int weapon_id );

	WeaponClass * 	Add_Weapon( const WeaponDefinitionClass * def, int rounds = 0, bool give_weapon = true );
	WeaponClass * 	Add_Weapon( int id, int rounds = 0, bool give_weapon = true );
	WeaponClass * 	Add_Weapon( const char *weapon_name, int rounds = 0, bool give_weapon = true );
	void				Remove_Weapon( int index );

	void				Clear_Weapons( void );

	int				Get_Count( void )					{ return WeaponList.Count(); }
	WeaponClass *	Peek_Weapon( int index )		{ return WeaponList[ index ]; }
	WeaponClass	*	Get_Weapon( void )				{ return	WeaponList[ WeaponIndex ]; }
	WeaponClass	*	Get_Next_Weapon( void );

	void				Import_Weapon_List(BitStreamClass & packet);
	void				Export_Weapon_List(BitStreamClass & packet);

	int				Get_Index( void )					{ return WeaponIndex; }
	void				Select_Index( int index );
	void				Select_Next( void );
	void				Select_Prev( void );
	void				Select_Key_Number( int key_number );
	void				Select_Weapon( WeaponClass * weapon );
	void				Select_Weapon_ID( int weapon_id );
	void				Select_Weapon_Name( const char * name );
	void				Deselect( void );

 	bool				Is_Changed( void )					{ return IsChanged; }
 	void				Force_Changed( void )				{ IsChanged = true; }
   void				Reset_Changed( void )				{ IsChanged = false; }

 	bool				HUD_Is_Changed( void )					{ return HUDIsChanged; }
   void				HUD_Reset_Changed( void )				{ HUDIsChanged = false; }

	// returns if anything was moved
	bool				Move_Contents( WeaponBagClass * source );

	void				Store_Inventory( InventoryClass * );
	void				Restore_Inventory( InventoryClass * );

private:
	ArmedGameObj						*	Owner;
	DynamicVectorClass<WeaponClass*>	WeaponList;
	int										WeaponIndex;
	bool										IsChanged;
	bool										HUDIsChanged;

	WeaponClass *	Find_Weapon( const WeaponDefinitionClass * def );

	void		Mark_Owner_Dirty( void );

};
