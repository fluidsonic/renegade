#ifndef	DAMAGEZONE_H
#define	DAMAGEZONE_H

#ifndef	ALWAYS_H
	#include "always.h"
#endif

#ifndef OBBOX_H
	#include "obbox.h"
#endif

#ifndef	BASEGAMEOBJ_H
	#include "basegameobj.h"
#endif

/*
** Damage Zone Definition
*/
class DamageZoneGameObjDef : public BaseGameObjDef
{

public:
	DamageZoneGameObjDef( void );

	virtual uint32_t								Get_Class_ID( void ) const;
	virtual PersistClass *					Create( void ) const ;
	virtual bool								Save( ChunkSaveClass &csave );
	virtual bool								Load( ChunkLoadClass &cload );
	virtual const PersistFactoryClass &	Get_Factory( void ) const;

	DECLARE_EDITABLE( DamageZoneGameObjDef, BaseGameObjDef );

	const Vector3 &							Get_Color (void) const { return Color; }

protected:
	float			DamageRate;
	int			DamageWarhead;

	Vector3		Color;
	
	friend	class								DamageZoneGameObj;
};

/*
** Damage Zone 
*/
class DamageZoneGameObj : public BaseGameObj 
{

public:
									DamageZoneGameObj( void );
	virtual						~DamageZoneGameObj( void );

	virtual	void				Init( void );
	void							Init( const DamageZoneGameObjDef & definition );
	const DamageZoneGameObjDef &	Get_Definition( void ) const;

	virtual bool				Save( ChunkSaveClass & csave );
	virtual bool				Load( ChunkLoadClass & cload );
	virtual const PersistFactoryClass &	Get_Factory( void ) const;

	// Bounding Box
	void	Set_Bounding_Box( OBBoxClass & box )			{ BoundingBox = box; }
	const OBBoxClass & Get_Bounding_Box(void)				{ return BoundingBox; }

	virtual void				Think( void );

	// Network support
	//virtual bool				Exists_On_Client( void ) const	{ return false; }

protected:
	OBBoxClass				BoundingBox;
	float						DamageTimer;

};

#endif