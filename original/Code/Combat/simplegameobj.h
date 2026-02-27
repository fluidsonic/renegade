#pragma once

#include "global.h"


#include "physicalgameobj.h"

#include "playerterminal.h"

/*
** SimpleGameObjDef - Defintion class for a SimpleGameObj
*/
class SimpleGameObjDef : public PhysicalGameObjDef
{
public:
	SimpleGameObjDef( void );

	virtual uint32_t								Get_Class_ID( void ) const;
	virtual PersistClass *					Create( void ) const ;
	virtual bool								Save( ChunkSaveClass &csave );
	virtual bool								Load( ChunkLoadClass &cload );
	virtual const PersistFactoryClass &	Get_Factory( void ) const;


	// Accessors
	PlayerTerminalClass::TYPE				Get_Player_Terminal_Type( void ) const	{ return PlayerTerminalType; }
	bool											Get_Is_Editor_Object( void )	const		{ return IsEditorObject; }

protected:

	bool											IsEditorObject;
	bool											IsHiddenObject;	
	friend	class								SimpleGameObj;

	// See playerterminal.h
	PlayerTerminalClass::TYPE				PlayerTerminalType;
};

/*
**
*/
class SimpleGameObj : public PhysicalGameObj {

public:
	SimpleGameObj();
	virtual	~SimpleGameObj();

	// Definitions
	virtual	void	Init( void );
	void	Init( const SimpleGameObjDef & definition );
	const SimpleGameObjDef & Get_Definition( void ) const ;

	//	RTTI
	virtual	SimpleGameObj	*As_SimpleGameObj( void )		{ return this; }

	// SimpleGameObj interface
	bool Is_Hidden_Object(void)									{ return Get_Definition().IsHiddenObject; }

	// Save / Load / Construction Factory
	virtual	bool	Save( ChunkSaveClass & csave );
	virtual	bool	Load( ChunkLoadClass & cload );
	virtual	const	PersistFactoryClass & Get_Factory( void ) const;
	virtual	void	On_Post_Load( void );

	// Network support - TSS091901
	virtual	bool	Is_Always_Dirty( void )						{ return false; }
};
