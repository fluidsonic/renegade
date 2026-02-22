#pragma once

#include "global.h"


#ifndef	DEFINITION_H
	#include "definition.h"
#endif

class	ArmedGameObj;
class	BuildingGameObj;
class	DamageableGameObj;

/*
**
*/
class	ExplosionDefinitionClass : public DefinitionClass {

public:
	ExplosionDefinitionClass( void );		

	virtual uint32_t								Get_Class_ID( void ) const;
	virtual PersistClass *					Create( void ) const					{ assert( 0 ); return NULL; }
	virtual bool								Save( ChunkSaveClass &csave );
	virtual bool								Load( ChunkLoadClass &cload );
	virtual const PersistFactoryClass &	Get_Factory( void ) const;

	DECLARE_EDITABLE( ExplosionDefinitionClass, DefinitionClass );

	int											PhysDefID;
	int											SoundDefID;
	float											DamageRadius;
	float											DamageStrength;
	int											DamageWarhead;
	bool											DamageIsScaled;
	StringClass									DecalFilename;
	float											DecalSize;
	bool											AnimatedExplosion;

	float											CameraShakeIntensity;
	float											CameraShakeRadius;
	float											CameraShakeDuration;
};

/*
** Explosion Manager
*/
class	ExplosionManager {

public:
	static	void	Create_Explosion_At( int exlosion_id, const Vector3 & pos, ArmedGameObj * damager, const Vector3 & blast_direction = Vector3( 0,0,-1), DamageableGameObj * force_victim = NULL );
	static	void	Create_Explosion_At( int exlosion_id, const Matrix3D & tm, ArmedGameObj * damager, const Vector3 & blast_direction = Vector3( 0,0,-1), DamageableGameObj * force_victim = NULL  );

	static	void	Explosion_Damage_Building( int exlosion_id, BuildingGameObj * building, bool mct_damage, ArmedGameObj * damager );

	static	void	Server_Explode( int explosion_id, const Vector3 & pos, int owner_id, DamageableGameObj * force_victim = NULL );
	static	void	Explode( int explosion_id, const Vector3 & pos, int owner_id, int victim_id = 0 );
};
