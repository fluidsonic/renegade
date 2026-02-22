#include "shutdown.h"
#include "wwmath.h"
#include "wwsaveload.h"
#include "ww3d.h"
#include "WWAudio.H"
#include "wwphys.h"
#include "assets.h"
#include "ffactorylist.h"
#include "input.h"
#include "inputconfigmgr.h"
#include "gamemode.h"
//#include "gamesettings.h"
#include "miscutil.h"
#include "refcount.h"
#include "cnetwork.h"
#include "_globals.h"
#include "systemsettings.h"
#include "diagnostics.h"
#include "translatedb.h"
#include "pathmgr.h"
#include "renegadedialogmgr.h"
#include "campaign.h"
#include "diaglog.h"
#include "binkmovie.h"
//#include "helptext.h"
#include "init.h"
#include "serverfps.h"
#include "encyclopediamgr.h"
#include "playermanager.h"
#include "teammanager.h"
#include "bandwidthgraph.h"
#include "except.h"
#include "skinpackagemgr.h"
#include "modpackagemgr.h"

#include "dx8wrapper.h"
#include "pscene.h"
#include "systeminfolog.h"
#include "dx8caps.h"
#include "registry.h"
#include <windows.h>
#include <lmcons.h>	// UNLEN
extern SimpleFileFactoryClass RenegadeBaseFileFactory;

#define	SYSTEM_INFO_LOG_DISABLE "SystemInfoLogDisable"

// These are copy-pasted from WWConfig
//extern const char *KEY_NAME_SETTINGS;
extern const char *VALUE_NAME_DYN_LOD;
extern const char *VALUE_NAME_STATIC_LOD;
extern const char *VALUE_NAME_DYN_SHADOWS;
const char *VALUE_NAME_PRELIT_MODE		= "Prelit_Mode";
extern const char *VALUE_NAME_SHADOW_MODE;
extern const char *VALUE_NAME_STATIC_SHADOWS;
extern const char *VALUE_NAME_TEXTURE_RES;
const char *VALUE_NAME_SURFACE_EFFECT	= "Surface_Effect_Detail";
extern const char *VALUE_NAME_PARTICLE_DETAIL;
const char *VALUE_NAME_TEXTURE_FILTER_MODE="Texture_Filter_Mode";

static void Get_Detail_String(StringClass& str)
{
	str="";
	RegistryClass registry (APPLICATION_SUB_KEY_NAME_SYSTEM_SETTINGS);
	if (registry.Is_Valid ()) {

		//
		//	Read the values from the registry
		//
		int dynamic_lod		= registry.Get_Int (VALUE_NAME_DYN_LOD, 3000);
		int static_lod			= registry.Get_Int (VALUE_NAME_STATIC_LOD, 3000);

		int dynamic_shadows	= registry.Get_Int (VALUE_NAME_DYN_SHADOWS, 1);
		int static_shadows	= registry.Get_Int (VALUE_NAME_STATIC_SHADOWS, 1);

		int texture_filter	= registry.Get_Int (VALUE_NAME_TEXTURE_FILTER_MODE, TextureClass::TEXTURE_FILTER_BILINEAR);
		int prelit_mode		= registry.Get_Int (VALUE_NAME_PRELIT_MODE, WW3D::PRELIT_MODE_LIGHTMAP_MULTI_TEXTURE);
		int shadow_mode		= registry.Get_Int (VALUE_NAME_SHADOW_MODE, PhysicsSceneClass::SHADOW_MODE_BLOBS_PLUS);
		int texture_red		= registry.Get_Int (VALUE_NAME_TEXTURE_RES, 0);
		int surface_effect	= registry.Get_Int (VALUE_NAME_SURFACE_EFFECT, 1);
		int particle_detail	= registry.Get_Int (VALUE_NAME_PARTICLE_DETAIL, 1);

		StringClass tmp;
		tmp.Format("Dynamic LOD budget: %d\r\n",dynamic_lod);
		str+=tmp;
		tmp.Format("Static LOD budget: %d\r\n",static_lod);
		str+=tmp;

		str+="Shadow Mode: ";
		switch (shadow_mode) {
		case PhysicsSceneClass::SHADOW_MODE_NONE: str+="None\r\n"; break;
		case PhysicsSceneClass::SHADOW_MODE_BLOBS: str+="Blobs\r\n"; break;
		case PhysicsSceneClass::SHADOW_MODE_BLOBS_PLUS: str+="Blobs Plus\r\n"; break;
		case PhysicsSceneClass::SHADOW_MODE_HARDWARE: str+="Hardware\r\n"; break;
		default: str+="???\r\n"; break;
		}

		tmp.Format("Dynamic Shadows: %s\r\n",dynamic_shadows ? "On" : "Off");
		str+=tmp;
		tmp.Format("Static Shadows: %s\r\n",static_shadows ? "On" : "Off");
		str+=tmp;

		str+="Prelit Mode: ";
		switch (prelit_mode) {
		case WW3D::PRELIT_MODE_VERTEX: str+="Vertex\r\n"; break;
		case WW3D::PRELIT_MODE_LIGHTMAP_MULTI_PASS: str+="Multipass\r\n"; break;
		case WW3D::PRELIT_MODE_LIGHTMAP_MULTI_TEXTURE: str+="Multitexture\r\n"; break;
		default: str+="???\r\n"; break;
		}
		tmp.Format("Texture Resolution: %d\r\n",texture_red);
		str+=tmp;

		tmp.Format("Surface Effects (0-2): %d\r\n",surface_effect);
		str+=tmp;

		tmp.Format("Particle Detail(0-2): %d\r\n",particle_detail);
		str+=tmp;

		str+="Texture Filter Mode: ";
		switch (texture_filter) {
		case TextureClass::TEXTURE_FILTER_BILINEAR: str+="Bilinear\r\n"; break;
		case TextureClass::TEXTURE_FILTER_TRILINEAR: str+="Trilinear\r\n"; break;
		case TextureClass::TEXTURE_FILTER_ANISOTROPIC: str+="Anisotropic\r\n"; break;
		default: str+="???\r\n"; break;
		}

		tmp.Format("Screen UV Bias: %s\r\n",WW3D::Is_Screen_UV_Biased() ? "Enabled" : "Disabled");
		str+=tmp;

		// NPatch level
		str+="NPatch level: ";
		if (DX8Wrapper::Get_Current_Caps() && DX8Wrapper::Get_Current_Caps()->Support_NPatches()) {
			if (WW3D::Get_NPatches_Level()<=1) {
				str+="Disabled\r\n";
			}
			else {
				tmp.Format("%d\r\n",WW3D::Get_NPatches_Level());
				str+=tmp;
			}
		}
		else {
			str+="Not supported\r\n";
		}

		int w;
		int h;
		int bits;
		bool windowed;
		WW3D::Get_Device_Resolution(w,h,bits,windowed);
		tmp.Format("Display mode: %d * %d, %d bits %s\r\n", w,h,bits,windowed ? "Windowed" : "Fullscreen");
		str+=tmp;

		str+="\r\n";

		const char *VALUE_NAME_SOUND_DEVICE_NAME = "device name";
		RegistryClass registry_sound( APPLICATION_SUB_KEY_NAME_SOUND );
		if ( registry_sound.Is_Valid() ) {
			char temp_buffer[256] = { 0 };
			registry.Get_String (VALUE_NAME_SOUND_DEVICE_NAME, temp_buffer, sizeof (temp_buffer));
			tmp.Format("Sound device: %s\r\n",temp_buffer);
			str+=tmp;
		}

		WWAudioClass* audio=WWAudioClass::Get_Instance();
		if (audio) {
			tmp.Format("Sound effects: %s\r\n",audio->Are_Sound_Effects_On() ? "Enabled" : "Disabled");
			str+=tmp;
			tmp.Format("Sound effects volume: %2.2f\r\n",audio->Get_Sound_Effects_Volume());
			str+=tmp;
			tmp.Format("Music: %s\r\n",audio->Is_Music_On() ? "Enabled" : "Disabled");
			str+=tmp;
			tmp.Format("Music volume: %2.2f\r\n",audio->Get_Music_Volume());
			str+=tmp;
		}
	}
}

void Get_Compact_Detail_String(StringClass& str)
{
	str="";
	RegistryClass registry (APPLICATION_SUB_KEY_NAME_SYSTEM_SETTINGS);
	if (registry.Is_Valid ()) {

		//
		//	Read the values from the registry
		//
		int dynamic_lod		= registry.Get_Int (VALUE_NAME_DYN_LOD, 3000);
		int static_lod			= registry.Get_Int (VALUE_NAME_STATIC_LOD, 3000);

		int dynamic_shadows	= registry.Get_Int (VALUE_NAME_DYN_SHADOWS, 1);
		int static_shadows	= registry.Get_Int (VALUE_NAME_STATIC_SHADOWS, 1);

		int texture_filter	= registry.Get_Int (VALUE_NAME_TEXTURE_FILTER_MODE, TextureClass::TEXTURE_FILTER_BILINEAR);
		int prelit_mode		= registry.Get_Int (VALUE_NAME_PRELIT_MODE, WW3D::PRELIT_MODE_LIGHTMAP_MULTI_TEXTURE);
		int shadow_mode		= registry.Get_Int (VALUE_NAME_SHADOW_MODE, PhysicsSceneClass::SHADOW_MODE_BLOBS_PLUS);
		int texture_red		= registry.Get_Int (VALUE_NAME_TEXTURE_RES, 0);
		int surface_effect	= registry.Get_Int (VALUE_NAME_SURFACE_EFFECT, 1);
		int particle_detail	= registry.Get_Int (VALUE_NAME_PARTICLE_DETAIL, 1);

		StringClass tmp;
		tmp.Format("%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t",dynamic_lod,static_lod,shadow_mode,dynamic_shadows,static_shadows,prelit_mode,texture_red,surface_effect,particle_detail,texture_filter);
		str+=tmp;

		tmp.Format("%d\t",1);//WW3D::Get_Texture_Compression_Mode());
		str+=tmp;

		int w;
		int h;
		int bits;
		bool windowed;
		WW3D::Get_Device_Resolution(w,h,bits,windowed);
		tmp.Format("%d\t%d\t%d\t%d\t",w,h,bits,windowed);
		str+=tmp;
	}
}

/*
**
*/
void Debug_Refs(void);

/*
**
*/
void Game_Shutdown(void)
{
	BINKMovie::Shutdown();

	CampaignManager::Shutdown();

	SystemSettings::Shutdown();
	CombatManager::Shutdown();

	//Analyze_Soldier_Bandwidth();

   cNetwork::Onetime_Shutdown();

	cServerFps::Destroy_Instance();

	cPlayerManager::Onetime_Shutdown();
	cTeamManager::Onetime_Shutdown();
	cGameData::Onetime_Shutdown();
	cBandwidthGraph::Onetime_Shutdown();

	GameModeManager::Destroy_All();
  EncyclopediaMgrClass::Shutdown();
	RenegadeDialogMgrClass::Shutdown();

	// Free the sound library
	if (WWAudioClass::Get_Instance () != NULL) {
		//WWAudioClass::Get_Instance ()->Shutdown ();
		delete WWAudioClass::Get_Instance ();
	}

	//GameSettings::Shutdown();

	SkinPackageMgrClass::Shutdown ();
	ModPackageMgrClass::Shutdown ();
	TranslateDBClass::Shutdown();

	//
	//	Shutdown the input control system
	//
	InputConfigMgrClass::Shutdown();
	Input::Save_Registry( APPLICATION_SUB_KEY_NAME_CONTROLS );
	Input::Shutdown();

	DiagLogClass::Shutdown();

//	CommandoAssetManager::Shutdown();
	WW3D::_Invalidate_Textures();
	WW3DAssetManager::Delete_This();
//	if ( WW3DAssetManager::Get_Instance() ) {
//		delete WW3DAssetManager::Get_Instance();
//	}

	cDiagnostics::Close();
	//cHelpText::Close();

	PhysicsSceneClass * scene = PhysicsSceneClass::Get_Instance();
	scene->Set_Max_Simultaneous_Shadows(0);

	PathMgrClass::Shutdown();
	WWMath::Shutdown();
	WWSaveLoad::Shutdown();
	WW3D::Shutdown();
	WWPhys::Shutdown();

//	WW3DAssetManager::Get_Instance()->Free_Assets();
	Debug_Refs();

	WSA_CHECK(WSACleanup());

	/*
	** Remove any old file factories still lying around.
	*/
	if (FileFactoryListClass::Get_Instance() != NULL) {
		FileFactoryListClass::Get_Instance()->Remove_FileFactory(&RenegadeBaseFileFactory);
	}
	while (FileFactoryListClass::Get_Instance() != NULL) {
		FileFactoryClass * factory = FileFactoryListClass::Get_Instance()->Remove_FileFactory();
		if (factory != NULL) {
			delete factory;
		} else {
			break;
		}
	}

	RegistryClass registry( APPLICATION_SUB_KEY_NAME_DEBUG );
	if ( registry.Is_Valid() ) {
		registry.Set_Int( VALUE_NAME_APPLICATION_CRASH_VERSION, 0 );
	}

	return ;
}

/*
**
*/
void Debug_Refs(void)
{
#ifndef NDEBUG
//	Debug_Say(("Detecting Active Refs...\r\n"));
	RefCountNodeClass * first = RefCountClass::ActiveRefList.First();
	RefCountNodeClass * node = first;
	while (node->Is_Valid())
	{
		RefCountClass * obj = node->Get();
		ActiveRefStruct * ref = &(obj->ActiveRefInfo);

		bool display = true;
		int	count = 0;
		RefCountNodeClass * search = first;
		while (search->Is_Valid()) {

			if (search == node) {	// if this is not the first one
				if (count != 0) {
					display = false;
					break;
				}
			}

			RefCountClass * search_obj = search->Get();
			ActiveRefStruct * search_ref = &(search_obj->ActiveRefInfo);

			if ( ref->File && search_ref->File &&
				  !strcmp(search_ref->File, ref->File) &&
				  (search_ref->Line == ref->Line) ) {
				count++;
			} else if ( (ref->File == NULL) &&  (search_ref->File == NULL) ) {
				count++;
			}

			search = search->Next();
		}

		if ( display ) {
			Debug_Say(( "%d Active Ref: %s %d %p\n", count, ref->File,ref->Line,obj));

			static int num_printed = 0;
			if (++num_printed > 20) {
				Debug_Say(( "And Many More......\n"));
				break;
			}
		}

		node = node->Next();
	}
//	Debug_Say(("Done.\r\n"));
#endif
}
