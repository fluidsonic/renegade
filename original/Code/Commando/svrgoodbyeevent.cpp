#include "svrgoodbyeevent.h"

#include <stdio.h>
#include <stdlib.h>

#include "networkobjectfactory.h"
#include "gamemode.h"
#include "cnetwork.h"
#include "translatedb.h"
#include "string_ids.h"
#include "textdisplay.h"
#include "wwaudio.h"
#include "devoptions.h"
//#include "helptext.h"
#include "dlgmessagebox.h"
#include "apppackettypes.h"
#include "string_ids.h"
#include "dlgcncwinscreen.h"
#include "dlgmpconnectionrefused.h"

// 
// TSS2001 Problem - we have lost the unreliable multiblast effect for this message
//

DECLARE_NETWORKOBJECT_FACTORY(cSvrGoodbyeEvent, NETCLASSID_SVRGOODBYEEVENT);

//-----------------------------------------------------------------------------
cSvrGoodbyeEvent::cSvrGoodbyeEvent(void)
{
	IsQuickFullExitRequested = false;

	Set_App_Packet_Type(APPPACKETTYPE_SVRGOODBYEEVENT);
}

//-----------------------------------------------------------------------------
void
cSvrGoodbyeEvent::Init(bool flag)
{

	IsQuickFullExitRequested = flag;

	Set_Object_Dirty_Bit(BIT_CREATION, true);

	/*
	if (cNetwork::I_Am_Client())
	{
		Act();
	}
	*/
}

//-----------------------------------------------------------------------------
void
cSvrGoodbyeEvent::Act(void)
{
	if (cNetwork::I_Am_Only_Client()) {

		cNetwork::PClientConnection->Destroy_Connection(0);	// destroy connection to server 
		
		DlgMsgBox::DoDialog(TRANSLATION(IDS_MENU_SERVER_MESSAGE_TITLE), TRANSLATION(IDS_MP_SERVER_SHUT_DOWN));

		//
		//	Close out the win screen dialog (if its up)
		//
		CNCWinScreenMenuClass::Close_Dialog ();

		WWAudioClass::Get_Instance()->Create_Instant_Sound("System_Message", Matrix3D(1));

      if (IsQuickFullExitRequested)
		{
			cDevOptions::QuickFullExit.Set(true);
		} 
		else 
		{
			//TSS090401
			//
			// The client needs to quit back to the game list
			//
			extern bool g_client_quit;
			g_client_quit = true;
		}
	}

	Set_Delete_Pending();
}

//-----------------------------------------------------------------------------
void
cSvrGoodbyeEvent::Export_Creation(BitStreamClass & packet)
{

	cNetEvent::Export_Creation(packet);

	packet.Add(IsQuickFullExitRequested);

	Set_Delete_Pending();
}

//-----------------------------------------------------------------------------
void
cSvrGoodbyeEvent::Import_Creation(BitStreamClass & packet)
{
	cNetEvent::Import_Creation(packet);

	packet.Get(IsQuickFullExitRequested);

	Act();
}

