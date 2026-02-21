#ifndef	TRANSITIONGAMEOBJ_H
#define	TRANSITIONGAMEOBJ_H

#ifndef	ALWAYS_H
	#include "always.h"
#endif

#ifndef BASEGAMEOBJ_H
	#include "basegameobj.h"
#endif

#ifndef TRANSITION_H
	#include "transition.h"
#endif

/*
** TransitionGameObjDef - Defintion class for a TransitionGameObj 
*/
class TransitionGameObjDef : public BaseGameObjDef
{
public:
	TransitionGameObjDef( void ) {};
	virtual ~TransitionGameObjDef( void );

	virtual uint32								Get_Class_ID( void ) const;
	virtual PersistClass *					Create( void ) const ;
	virtual bool								Save( ChunkSaveClass &csave );
	virtual bool								Load( ChunkLoadClass &cload );
	virtual const PersistFactoryClass &	Get_Factory( void ) const;

	TRANSITION_DATA_LIST *					Get_Transition_List( void ) { return &Transitions; }
	const TRANSITION_DATA_LIST &			Get_Transition_List( void ) const { return Transitions; }
	void											Free_Transition_List( void );

protected:

	TRANSITION_DATA_LIST						Transitions;

	friend	class								TransitionGameObj;
};

/*
**
*/
class TransitionGameObj : public BaseGameObj {

public:
	TransitionGameObj();
	~TransitionGameObj();

	// Definitions
	virtual	void	Init( void );
	void	Init( const TransitionGameObjDef & definition );
	const TransitionGameObjDef & Get_Definition( void ) const ;

	// Save / Load / Construction Factory
	virtual	bool	Save( ChunkSaveClass & csave );
	virtual	bool	Load( ChunkLoadClass & cload );
	virtual	const	PersistFactoryClass & Get_Factory( void ) const;
	virtual	void	On_Post_Load( void );

	// Physics
	void					Set_Transform(const Matrix3D & tm);
	const Matrix3D &	Get_Transform(void) const;

	// Transition access
	void		Create_Transitions( void );
	void		Destroy_Transitions( void );
	void		Update_Transitions( void );

	int								Get_Transition_Count( void ) const	{ return TransitionInstances.Count(); }
	TransitionInstanceClass *	Get_Transition( int index )			{ return TransitionInstances[index]; }

	//
	//	Ladder support
	//
	int		Get_Ladder_Index( void )					{ return LadderIndex; }
	void		Set_Ladder_Index( int ladder_index );

protected:

	DynamicVectorClass<TransitionInstanceClass *> TransitionInstances;
	Matrix3D						TM;
	int							LadderIndex;
};

#endif	//	TRANSITIONGAMEOBJ_H

