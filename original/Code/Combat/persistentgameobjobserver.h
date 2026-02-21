#ifndef	PERSISTENTGAMEOBJOBSERVER_H
#define	PERSISTENTGAMEOBJOBSERVER_H

#ifndef	ALWAYS_H
	#include "always.h"
#endif

#ifndef	GAMEOBJOBSERVER_H
	#include "gameobjobserver.h"
#endif

#ifndef	PERSIST_H
	#include "persist.h"
#endif

#ifndef	VECTOR_H
	#include "vector.h"
#endif


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

#endif	//	PERSISTENTGAMEOBJOBSERVER_H
