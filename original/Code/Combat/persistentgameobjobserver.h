#pragma once

#include "global.h"


#include "gameobjobserver.h"

#include "persist.h"

#include "vector.h"

/*
** PersistentGameObjObserverClass
*/
class	PersistentGameObjObserverClass : public PersistClass, public GameObjObserverClass {
public:
				 PersistentGameObjObserverClass( void );
	virtual	~PersistentGameObjObserverClass( void );

	virtual	bool	Save( ChunkSaveClass & csave );
	virtual	bool	Load( ChunkLoadClass & cload );

};

/*
** class	PersistentGameObjObserverManager
*/
class	PersistentGameObjObserverManager {
public:
	static	void	Add( PersistentGameObjObserverClass * observer );
	static	void	Remove( PersistentGameObjObserverClass * observer );

	static	bool	Save( ChunkSaveClass & csave );
	static	bool	Load( ChunkLoadClass & cload );

	static	void	Reset( void );

private:
	static	DynamicVectorClass<PersistentGameObjObserverClass *>	ObserverList;
};
