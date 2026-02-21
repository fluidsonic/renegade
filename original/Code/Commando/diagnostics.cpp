//
// Filename:     diagnostics.cpp
// Project:      Network.lib, for Commando
// Author:       Tom Spencer-Smith
// Date:         Dec 1998
// Description:
//

#include "diagnostics.h"

#include <stdio.h>

#include "assets.h"
#include "font3d.h"
#include "render2d.h"

//
// Add any includes for exposed interfaces that you are diagnosing
//

#include "devoptions.h"
#include "cnetwork.h"
#include "gamedata.h"
#include "networkobjectmgr.h"
#include "serverfps.h"
#include "sbbomanager.h"
#include "singlepl.h"
#include "gameobjmanager.h"
#include "gamemode.h"
#include "humanphys.h"
#include "playermanager.h"
#include "useroptions.h"
#include "packetmgr.h"
#include "apppacketstats.h"
#include "connect.h"
#include "vehicle.h"
#include "csdamageevent.h"
#include "specialbuilds.h"
#include "consolemode.h"
#include "gametype.h"

static int RendererFps;
static int RendererSFps;
static int RendererPing;
static int RendererBandwidthBps;
static bool RendererGodStatus;
static bool RendererVipStatus;

//
// Class statics
//
Render2DTextClass *		cDiagnostics::PRenderer		= NULL;
Font3DInstanceClass *	cDiagnostics::PFont			= NULL;
float							cDiagnostics::DiagnosticX	= 0;
float							cDiagnostics::DiagnosticY	= 0;

//-----------------------------------------------------------------------------
void cDiagnostics::Init(void)
{
	if (!ConsoleBox.Is_Exclusive()) {
   	PFont = WW3DAssetManager::Get_Instance()->Get_Font3DInstance("FONT6x8.TGA");
		SET_REF_OWNER(PFont);
		PFont->Set_Mono_Spaced();

		PRenderer = new Render2DTextClass(PFont);

		RectClass rect = Render2DClass::Get_Screen_Resolution();
		PRenderer->Set_Coordinate_Range(rect);

		DiagnosticX = 10;
		//DiagnosticY = 10;

		RendererFps=0;
		RendererSFps=0;
		RendererPing = 0;
		RendererBandwidthBps = 0;
		RendererGodStatus=false;
		RendererVipStatus=false;
	}

}

//-----------------------------------------------------------------------------
void cDiagnostics::Close(void)
{
	if (PFont != NULL) {
		PFont->Release_Ref();
		PFont = NULL;
	}

	if (PRenderer != NULL) {
		delete PRenderer;
		PRenderer = NULL;
	}
}

//-----------------------------------------------------------------------------
void cDiagnostics::Show_Object_Tally(void)
{
	cAppPacketStats::Update_Object_Tally();

	Add_Diagnostic("");
	Add_Diagnostic("Object stats (reset with APTR command):");
	Add_Diagnostic(cAppPacketStats::Get_Heading());

	for (BYTE i = 0; i < APPPACKETTYPE_COUNT; i++)
	{
		Add_Diagnostic(cAppPacketStats::Get_Description(i));
	}
}

//-----------------------------------------------------------------------------
void cDiagnostics::Add_Diagnostic(LPCSTR format, ...)
{
	if (PRenderer) {
		va_list va;
		char buffer[1024];

		va_start(va, format);
		::vsprintf(buffer, format, va);

		PRenderer->Set_Location(Vector2(DiagnosticX, DiagnosticY));
		PRenderer->Draw_Text(buffer);

		DiagnosticY += (int)(PRenderer->Peek_Font()->Char_Height() * 1.2);
	}
}

//-----------------------------------------------------------------------------
void cDiagnostics::Render(void)
{
	if (PFont == NULL || PRenderer == NULL) {
		return;
	}

#ifdef BETACLIENT
	PRenderer->Reset();
	RectClass rect = Render2DClass::Get_Screen_Resolution();
	PRenderer->Set_Location(Vector2(rect.Left + 5, rect.Bottom - 10));
	PRenderer->Draw_Text("BETACLIENT");
	PRenderer->Render();
#endif // BETACLIENT

#ifdef FREEDEDICATEDSERVER
	PRenderer->Reset();
	RectClass rect = Render2DClass::Get_Screen_Resolution();
	PRenderer->Set_Location(Vector2(rect.Left + 5, rect.Bottom - 10));
	PRenderer->Draw_Text("FREEDEDICATEDSERVER");
	PRenderer->Render();
#endif // FREEDEDICATEDSERVER

#ifdef MULTIPLAYERDEMO
	PRenderer->Reset();
	RectClass rect = Render2DClass::Get_Screen_Resolution();
	PRenderer->Set_Location(Vector2(rect.Left + 5, rect.Bottom - 10));
	PRenderer->Draw_Text("MULTIPLAYER DEMO");
	PRenderer->Render();
#endif // MULTIPLAYERDEMO

	bool changed=false;

	if (cDevOptions::ShowFps.Is_True()) {
		int fps=cNetwork::Get_Fps();
		if (fps!=RendererFps) {
			RendererFps=fps;
			changed=true;
		}

		if (cNetwork::I_Am_Only_Client())
		{
			fps=cServerFps::Get_Instance()->Get_Fps();
			if (fps!=RendererSFps) {
				RendererSFps=fps;
				changed=true;
			}

			if (cNetwork::PClientConnection) {
				cRemoteHost *server = cNetwork::PClientConnection->Get_Remote_Host(0);
				if (server) {
					int ping = server->Get_Average_Internal_Pingtime_Ms();
					if (ping && ping != RendererPing) {
						RendererPing = ping;
						changed = true;
					}
					int bps = PacketManager.Get_Compressed_Bandwidth_In(&server->Get_Address());
					if (bps && bps != RendererBandwidthBps) {
						RendererBandwidthBps = bps;
						changed = true;
					}
				}
			}
		} else {
			int bps = PacketManager.Get_Total_Compressed_Bandwidth_Out();
			if (bps && bps != RendererBandwidthBps) {
				RendererBandwidthBps = bps;
				changed = true;
			}
		}
	}
	// If we displayed fps last frame, turn it off now
	else {
		if (RendererFps) {
			changed=true;
		}
		RendererFps=0;
	}

	if (cDevOptions::ShowFps.Is_True()) {
		// Stop the flicker
		changed = true;
	}
	// Render only if changed!
	if (!changed) {
		PRenderer->Render();
		return;
	}

	PRenderer->Reset();

	DiagnosticY = 75;

	if (cDevOptions::ShowFps.Is_True()) {
		StringClass fps_text;
		fps_text.Format("FPS = %3d", RendererFps);

		if (cNetwork::I_Am_Only_Client())
		{
			StringClass server_fps_text;
			server_fps_text.Format(", SFPS = %3d", RendererSFps);
			fps_text += server_fps_text;

			if (cNetwork::PClientConnection && cNetwork::PClientConnection->Get_Remote_Host(0)) {
				StringClass ping_time;
				ping_time.Format(", PING = %4d", min(9999, RendererPing));
				fps_text += ping_time;
			}

		}

		if (!IS_SOLOPLAY) {
			StringClass bps_text;
			bps_text.Format(", KBPS = %4d", RendererBandwidthBps / 1024);
			fps_text += bps_text;
		}

		float width = PFont->String_Width(fps_text);
		RectClass rect = Render2DClass::Get_Screen_Resolution();
		PRenderer->Set_Location(Vector2(rect.Right - width - 5, rect.Top + 2));
		PRenderer->Draw_Text(fps_text);
	}

	PRenderer->Render();

}
