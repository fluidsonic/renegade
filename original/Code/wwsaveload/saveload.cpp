#include "global.h"
#include "saveload.h"
#include "saveloadsubsystem.h"
#include "persist.h"
#include "persistfactory.h"
#include "chunkio.h"
#include "saveloadstatus.h"

#include <windows.h>
#include "systimer.h"
#include <typeinfo>
#include <cstdio>

SaveLoadSubSystemClass *		SaveLoadSystemClass::SubSystemListHead = NULL;
PersistFactoryClass *			SaveLoadSystemClass::FactoryListHead = NULL;
SList<PostLoadableClass>		SaveLoadSystemClass::PostLoadList;
PointerRemapClass					SaveLoadSystemClass::PointerRemapper;

bool SaveLoadSystemClass::Save (ChunkSaveClass &csave,SaveLoadSubSystemClass & subsystem)
{
	bool ok = true;

	if (subsystem.Contains_Data()) {
		csave.Begin_Chunk (subsystem.Chunk_ID ());
		ok &= subsystem.Save (csave);
		csave.End_Chunk ();
	}

	return ok;
}

bool SaveLoadSystemClass::Load (ChunkLoadClass &cload,bool auto_post_load)
{
	PointerRemapper.Reset();
	bool ok = true;

	// Load each chunk we encounter and link the manager into the PostLoad list
	while (cload.Open_Chunk ()) {
		SaveLoadStatus::Inc_Status_Count();		// Count the sub systems loaded
		SaveLoadSubSystemClass *sys = Find_Sub_System(cload.Cur_Chunk_ID ());
		if (sys != NULL) {
//
			INIT_SUB_STATUS(sys->Name());
			ok &= sys->Load(cload);
		}
		cload.Close_Chunk();
	}

	// Process all of the pointer remap requests
	PointerRemapper.Process();
	PointerRemapper.Reset();

	// Call PostLoad on each PersistClass that wanted post-load
	if (auto_post_load) {
		Post_Load_Processing(NULL);
	}

	return ok;
}

// Nework update macro for post loader.
#define UPDATE_NETWORK 											\
	if (network_callback) {                            \
		unsigned long time2 = TIMEGETTIME();            \
		if (time2 - time > 20) {                        \
			network_callback();                          \
			time = time2;                                \
		}                                               \
	}                                                  \

bool SaveLoadSystemClass::Post_Load_Processing (void(*network_callback)(void))
{
	unsigned long time = TIMEGETTIME();

	// Call PostLoad on each PersistClass that wanted post-load
	PostLoadableClass * obj = PostLoadList.Remove_Head();
	while (obj) {
		UPDATE_NETWORK;
		fprintf(stderr, "PostLoad: calling On_Post_Load %p vptr=%p\n", obj, *(void**)obj);
		fflush(stderr);
		obj->On_Post_Load();
		obj->Set_Post_Load_Registered(false);
		obj = PostLoadList.Remove_Head();
	}

	return true;
}

void SaveLoadSystemClass::Register_Sub_System (SaveLoadSubSystemClass * sys)
{
	Link_Sub_System(sys);
}

void SaveLoadSystemClass::Unregister_Sub_System (SaveLoadSubSystemClass * sys)
{
	Unlink_Sub_System(sys);
}

SaveLoadSubSystemClass * SaveLoadSystemClass::Find_Sub_System (uint32_t chunk_id)
{
	// TODO: need a d-s that gives fast searching based on chunk_id!!
	SaveLoadSubSystemClass * sys;
	for ( sys = SubSystemListHead; sys != NULL; sys = sys->NextSubSystem ) {
		if ( sys->Chunk_ID() == chunk_id ) {
			break;
		}
	}
	return sys;
}

void SaveLoadSystemClass::Register_Persist_Factory(PersistFactoryClass * factory)
{
	Link_Factory(factory);
}

void SaveLoadSystemClass::Unregister_Persist_Factory(PersistFactoryClass * factory)
{
	Unlink_Factory(factory);
}

PersistFactoryClass * SaveLoadSystemClass::Find_Persist_Factory(uint32_t chunk_id)
{
	// TODO: need a d-s that gives fast searching based on chunk_id!!
	PersistFactoryClass * fact;
	for ( fact = FactoryListHead; fact != NULL; fact = fact->NextFactory ) {
		if ( fact->Chunk_ID() == chunk_id ) {
			break;
		}
	}
	return fact;
}

bool SaveLoadSystemClass::Is_Post_Load_Callback_Registered(PostLoadableClass * obj)
{
	// obsolete!
	bool retval = false;

	SLNode<PostLoadableClass> *list_node = NULL;
	for (	list_node = PostLoadList.Head();
			retval == false && list_node != NULL;
			list_node = list_node->Next())
	{
		retval = (list_node->Data() == obj);
	}

	return retval;
}

void SaveLoadSystemClass::Register_Post_Load_Callback(PostLoadableClass * obj)
{
	if (!obj->Is_Post_Load_Registered()) {
		fprintf(stderr, "PostLoad: register %p (%s)\n", obj, typeid(*obj).name());
		obj->Set_Post_Load_Registered(true);
		PostLoadList.Add_Head(obj);
	}
}

void SaveLoadSystemClass::Register_Pointer (void *old_pointer, void *new_pointer)
{
	PointerRemapper.Register_Pointer(old_pointer,new_pointer);
}

void SaveLoadSystemClass::Request_Pointer_Remap (void **pointer_to_convert)
{
	PointerRemapper.Request_Pointer_Remap(pointer_to_convert);
}

void SaveLoadSystemClass::Request_Ref_Counted_Pointer_Remap (RefCountClass **pointer_to_convert)
{
	PointerRemapper.Request_Ref_Counted_Pointer_Remap(pointer_to_convert);
}

void SaveLoadSystemClass::Link_Sub_System(SaveLoadSubSystemClass * sys)
{
	if (sys != NULL) {
		sys->NextSubSystem = SubSystemListHead;
		SubSystemListHead = sys;
	}
}

void SaveLoadSystemClass::Unlink_Sub_System(SaveLoadSubSystemClass * sys)
{
	SaveLoadSubSystemClass * cursys = SubSystemListHead;
	SaveLoadSubSystemClass * prev = NULL;

	while (cursys != sys) {
		prev = cursys;
		cursys = cursys->NextSubSystem;
	}

	if (prev == NULL) {
		SubSystemListHead = sys->NextSubSystem;
	} else {
		prev->NextSubSystem = sys->NextSubSystem;
	}

	sys->NextSubSystem = NULL;
}

void SaveLoadSystemClass::Link_Factory(PersistFactoryClass * fact)
{
	if (fact != NULL) {
		fact->NextFactory = FactoryListHead;
		FactoryListHead = fact;
	}
}

void SaveLoadSystemClass::Unlink_Factory(PersistFactoryClass * fact)
{

	PersistFactoryClass * curfact = FactoryListHead;
	PersistFactoryClass * prev = NULL;

	while (curfact != fact) {
		prev = curfact;
		curfact = curfact->NextFactory;
	}

	if (prev == NULL) {
		FactoryListHead = fact->NextFactory;
	} else {
		prev->NextFactory = fact->NextFactory;
	}

	fact->NextFactory = NULL;
}
