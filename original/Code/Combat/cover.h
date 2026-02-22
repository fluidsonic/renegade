#pragma once

#include "global.h"


#ifndef	MATRIX3D_H
	#include "matrix3d.h"
#endif

#ifndef	VECTOR_H
	#include "vector.h"
#endif

#ifndef	REFCOUNT_H
	#include "refcount.h"
#endif

class	ChunkSaveClass;
class	ChunkLoadClass;

/*
**
*/
typedef	DynamicVectorClass<Vector3>	AttackPositionListType;

class	CoverEntryClass : public RefCountClass {

public:
	CoverEntryClass( void ) : Transform(1), Crouch( false ), InUse( false )	{}
	~CoverEntryClass( void ) {}

	bool	Save( ChunkSaveClass & csave );
	bool	Load( ChunkLoadClass & cload );

	void	Set_Transform( const Matrix3D	& tm )	{ Transform = tm; }
	const Matrix3D	& Get_Transform( void )			{ return Transform; }

	AttackPositionListType * Get_Attack_Position_List( void )	{ return &AttackPositionList; }
	Vector3 Get_Attack_Position( Vector3 & enemy_pos );

	void	Set_Crouch( bool crouch )					{ Crouch = crouch; }
	bool	Get_Crouch( void )							{ return Crouch; }

	void	Set_In_Use( bool in_use )					{ InUse = in_use; }
	bool	Get_In_Use( void )							{ return InUse; }

private:
	Matrix3D						Transform;
	AttackPositionListType	AttackPositionList;
	bool							Crouch;
	bool							InUse;
};

/*
**
*/
class	CoverManager {

public:
	static	void	Init( void );
	static	void	Shutdown( void );

	static	bool	Save( ChunkSaveClass & csave );
	static	bool	Load( ChunkLoadClass & cload );

	static	void	Add_Entry( CoverEntryClass * entry );
	static	void	Remove_Entry( CoverEntryClass * entry );
	static	void	Remove_All( void );

	static	CoverEntryClass * Request_Cover( const Vector3 & cur_pos, const Vector3 & danger, float max_dist );
	static	void	Release_Cover( CoverEntryClass * entry );

	static	bool	Is_Cover_Safe( CoverEntryClass * cover, const Vector3 & danger );

private:
	static	DynamicVectorClass<CoverEntryClass *>		CoverPositions;

};
