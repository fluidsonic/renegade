#include "global.h"
#include "floodprotectionmgr.h"
#include "combat.h"
#include "messagewindow.h"
#include "translatedb.h"
#include "string_ids.h"

//////////////////////////////////////////////////////////////////////
//	Static member initialization
//////////////////////////////////////////////////////////////////////
SimpleDynVecClass<FloodProtectionMgrClass::FLOOD_ENTRY>	FloodProtectionMgrClass::FloodList;

//////////////////////////////////////////////////////////////////////
//
//	Reset
//
//////////////////////////////////////////////////////////////////////
void
FloodProtectionMgrClass::Reset (void)
{
	FloodList.Delete_All ();
	return ;
}

//////////////////////////////////////////////////////////////////////
//
//	Decay_Old_Entries
//
//////////////////////////////////////////////////////////////////////
void
FloodProtectionMgrClass::Decay_Old_Entries (void)
{
	const int DECAY_TIME	= 15000;

	uint32_t curr_time = ::GetTickCount ();

	//
	//	Loop over all the entries in the list
	//
	int count = FloodList.Count ();
	for (int index = 0; index < count; index ++) {

		//
		//	Has this entry expired?
		//
		uint32_t time = FloodList[index].time;
		if ((curr_time - time) >= DECAY_TIME) {
			FloodList.Delete (index);
			index --;
			count --;
		}
	}

	return ;
}

//////////////////////////////////////////////////////////////////////
//
//	Detect_Flooding
//
//////////////////////////////////////////////////////////////////////
bool
FloodProtectionMgrClass::Detect_Flooding (const WCHAR *text)
{
	//
	//	First, remove any old entries
	//
	Decay_Old_Entries ();

	//
	//	The user is "flooding" if they have sent more then
	// 10 messages in the last 15 seconds
	//
	const int FLOOD_THRESHOLD	= 5;
	bool retval = (FloodList.Count () >= FLOOD_THRESHOLD);

	//
	//	If the user is spamming, then let them know it
	//
	if (retval) {
		Display_Flood_Message ();
	} else {

		//
		//	Add a new entry to the list
		//
		FLOOD_ENTRY entry;
		entry.time		= ::GetTickCount ();
		entry.text_len	= 0;
		FloodList.Add (entry);
	}

	return retval;
}

//////////////////////////////////////////////////////////////////////
//
//	Display_Flood_Message
//
//////////////////////////////////////////////////////////////////////
void
FloodProtectionMgrClass::Display_Flood_Message (void)
{
	MessageWindowClass *message_window = CombatManager::Get_Message_Window ();
	if (message_window != NULL) {

		//
		//	Display a message letting the user know they've been caught spamming
		//
		message_window->Add_Message (TRANSLATE (IDS_FLOOD_MSG), Vector3 (1.0F, 1.0F, 1.0F));
	}

	return ;
}
