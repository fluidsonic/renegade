#include "global.h"
#include "ridermanager.h"
#include "phys.h"

RiderManagerClass::RiderManagerClass(void)
{
}

RiderManagerClass::~RiderManagerClass(void)
{
	// Tell all riders to unlink from us. (they're also going to call back to Unlink_Rider
	// but we'll just handle that since it makes the interfaces simpler)
	while (RiderList.Peek_Head()) {
		PhysClass * obj = RiderList.Remove_Head();
		obj->Link_To_Carrier(NULL);
	}
}

void RiderManagerClass::Link_Rider(PhysClass * obj)
{
	RiderList.Add(obj);
}

void RiderManagerClass::Unlink_Rider(PhysClass * obj)
{
	RiderList.Remove(obj);
}

bool RiderManagerClass::Contains(PhysClass * obj)
{
	return RiderList.Contains(obj);
}

void RiderManagerClass::Move_Riders(const Matrix3D & delta,RenderObjClass * carrier_sub_obj)
{
	// for now, only handling translation
	Vector3 move;
	delta.Get_Translation(&move);

	
	// make all riders which are riding on this subobject ignore each other since they are 
	// going to move together
	NonRefPhysListIterator it(&RiderList);
	for (it.First(); !it.Is_Done(); it.Next()) {
		PhysClass * physobj = it.Peek_Obj();
		if ((carrier_sub_obj == NULL) || (physobj->Peek_Carrier_Sub_Object() == carrier_sub_obj)) {
			it.Peek_Obj()->Inc_Ignore_Counter();
		}
	}

	// move all of the riders (again, moving only the ones on the specified sub-object)
	for (it.First(); !it.Is_Done(); it.Next()) {
		PhysClass * physobj = it.Peek_Obj();
		if ((carrier_sub_obj == NULL) || (physobj->Peek_Carrier_Sub_Object() == carrier_sub_obj)) {
			it.Peek_Obj()->Push(move);
		}
	}

	// clear all of the riders ignore bits
	for (it.First(); !it.Is_Done(); it.Next()) {
		PhysClass * physobj = it.Peek_Obj();
		if ((carrier_sub_obj == NULL) || (physobj->Peek_Carrier_Sub_Object() == carrier_sub_obj)) {
			it.Peek_Obj()->Dec_Ignore_Counter();
		}
	}
}
