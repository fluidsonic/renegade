#include "global.h"
#include "console.h"
#include "consolefunction.h"
#include "textdisplay.h"
#include "assets.h"
#include "font3d.h"
#include "timemgr.h"
#include "input.h"
#include "miscutil.h"
#include "cnetwork.h"
#include "teammanager.h"
#include "scene.h"
#include "ww3d.h"
#include <stdio.h>
#include "wwaudio.h"
#include "audiblesound.H"
//#include "gamesettings.h"
#include "gamedata.h"
#include "overlay.h"
#include "combat.h"
#include "camera.h"
#include "ccamera.h"
#include "gameobjmanager.h"
#include "smartgameobj.h"
#include "playermanager.h"
#include "_globals.h"
#include "registry.h"
#include "phys3.h"
#include "devoptions.h"
#include "playertype.h"
#include "pscene.h"
#include "translatedb.h"
#include "string_ids.h"
#include "vehicle.h"
#include "wheelvehicle.h"
#include "wheel.h"
#include "statistics.h"
#include "meshmdl.h"
#include "w3d_file.h"		// for SURFACE_TYPE_STRINGS
#include "colors.h"
#include "dx8renderer.h"
#include "dx8wrapper.h"
#include "umbrasupport.h"
#include "render2d.h"
#include "sortingrenderer.h"
#include "sctextobj.h"
#include "textdisplay.h"
#include "trackedvehicle.h"
#include "dx8rendererdebugger.h"
#include "consolemode.h"

//#include "dlgmpingamechat.h"


#include <sstream>



//
// ConsoleGameModeClass statics
//
ConsoleGameModeClass * ConsoleGameModeClass::Instance = NULL;
const float ConsoleGameModeClass::LeftMargin = 0.02f;

/*
**	called each time through the main loop
*/
void 	ConsoleGameModeClass::Init()
{
	ConsoleFunctionManager::Init();

	ConsoleGameModeClass::Instance = this;
	InputActive = false;
	InputLine[0] = 0;
	Clear_Suggestion();

//	FPSActive = false;
	FPSFrames = 0;
	FPSTime = 0.0f;
	FPSLastTime = 0.0f;
	FPS = 0.0f;

//	ShowPlayerPosition = false;

	PerformanceSamplingActive = false;

	Load_Registry_Keys();

}

/*
**	called each time through the main loop
*/
void 	ConsoleGameModeClass::Shutdown()
{
	Save_Registry_Keys();

	ConsoleGameModeClass::Instance = NULL;

	ConsoleFunctionManager::Shutdown();
}

void ConsoleGameModeClass::Load_Registry_Keys(void)
{
//	Debug_Say(( "CombatGameModeClass::Load_Registry_Keys...\n" ));
	RegistryClass * registry = new RegistryClass( APPLICATION_SUB_KEY_NAME_OPTIONS );
	if ( registry->Is_Valid() ) {

      WW3D::Set_Screen_UV_Bias( registry->Get_Int( "ScreenUVBias", 1 ) != 0 );

      Get_Console()->Set_FPS_Active( registry->Get_Int( "FPS", 1 ) != 0 );
	}
	delete registry;
}

void ConsoleGameModeClass::Save_Registry_Keys(void)
{
//	Debug_Say(( "CombatGameModeClass::Save_Registry_Keys...\n"));
	RegistryClass * registry = new RegistryClass( APPLICATION_SUB_KEY_NAME_OPTIONS );
	if ( registry->Is_Valid() ) {
		registry->Set_Int( "ScreenUVBias", WW3D::Is_Screen_UV_Biased() );

//      registry->Set_Int( "TextureReduction", WW3D::Get_Texture_Reduction() );
//      registry->Set_Int( "TextureThumbnail", WW3D::Get_Texture_Thumbnail_Mode() );
//      registry->Set_Int( "TextureCompression", WW3D::Get_Texture_Compression_Mode() );
//      registry->Set_Int( "NPatchesLevel", WW3D::Get_NPatches_Level() );

      registry->Set_Int( "FPS", Get_Console()->Is_FPS_Active() );
	}
	delete registry;
}

#define	BACKSPACE_KEY		8
#define	ESC_KEY				27
#define	ENTER_KEY			13
#define	TAB_KEY				0x09
#define	SPACE_KEY			0x20

/*
**	called each time through the main loop
*/
void 	ConsoleGameModeClass::Think()
{

   /*
	//
	// TSS092501 - disabling this because it is fatal.
	// If you want it, you'll have to fix it!
	//
	if (Input::Get_State(INPUT_FUNCTION_VERBOSE_HELP)) {
		ConsoleFunctionManager::Next_Verbose_Help_Screen();
	}
	*/

	//cNetwork::Watch_Wol_Location(Drawer, Font);

   if ( !InputActive ) {
      bool enable_console = false;


// HACK: Disable console in ATI demo
//#ifndef ATI_DEMO_HACK
		if (Input::Get_State(INPUT_FUNCTION_BEGIN_CONSOLE)) {
         enable_console = true;
         ConsoleInputType = INPUT_FUNCTION_BEGIN_CONSOLE;
         strcpy(InputLine, "Command >");
      }
//#endif

      if (enable_console) {
		   InputActive = true;
         PromptLength = strlen(InputLine);
		   Input::Console_Enable();
			Clear_Suggestion();
      }
	}

	/*
	** Handle Console Input
	*/
	if ( InputActive ) {

		int key = Input::Console_Get_Key();

		while ( key ) {

			int len = strlen( InputLine ) ;

			switch( key ) {

				case ENTER_KEY:
					if (ConsoleInputType == INPUT_FUNCTION_BEGIN_CONSOLE) {
						Accept_Suggestion(InputLine + PromptLength);
						len = strlen(InputLine);
						Clear_Suggestion();
					}
					//Parse_Input( InputLine );
					Parse_Input( InputLine + PromptLength );

				case ESC_KEY:
					InputActive = false;
					Input::Console_Disable();
					break;

				case BACKSPACE_KEY:
					//if ( len > 0 ) {
					if ( len > PromptLength ) {
						InputLine[ --len ] = 0;
						if (ConsoleInputType == INPUT_FUNCTION_BEGIN_CONSOLE) {
							Update_Suggestion(InputLine + PromptLength,true);
						}

					}
					break;

				case TAB_KEY:
					if (ConsoleInputType == INPUT_FUNCTION_BEGIN_CONSOLE) {
						Update_Suggestion(InputLine + PromptLength,true);
					}
					break;

				case SPACE_KEY:
					// Accept any suggested command line completion and fall through to default
					if (ConsoleInputType == INPUT_FUNCTION_BEGIN_CONSOLE) {
						Accept_Suggestion(InputLine + PromptLength);
						len = strlen(InputLine);
					}

				default:
					if ( len + 1 < MAX_INPUT_LINE_LENGTH) {
						InputLine[ len++ ] = key;
						InputLine[ len ] = 0;

						if (ConsoleInputType == INPUT_FUNCTION_BEGIN_CONSOLE) {
							Update_Suggestion(InputLine + PromptLength,false);
						}
                    }
					break;
			}

			key = Input::Console_Get_Key();

		}

		/*
		** Console Input Output
		*/
		char mess[MAX_INPUT_LINE_LENGTH+2];
		strcpy( mess, InputLine );

		static int flash = 0;
		if ( (int)(TimeManager::Get_Seconds()*4) & 1 ) {
			strcat( mess, "|" );
		}
		strcat( mess, "\n" );

		Vector3	color(1.0f,1.0f,1.0f);
		switch (ConsoleInputType) {
			/*
			case INPUT_FUNCTION_BEGIN_PUBLIC_MESSAGE :
				color = COLOR_PUBLIC_TEXT;
				break;
			case INPUT_FUNCTION_BEGIN_TEAM_MESSAGE :
				color = cNetwork::Get_My_Color();
				break;
			case INPUT_FUNCTION_BEGIN_PRIVATE_MESSAGE :
				color = COLOR_PRIVATE_TEXT;
				break;
			*/
			case INPUT_FUNCTION_BEGIN_CONSOLE :
				color = COLOR_CONSOLE_TEXT;
				break;
			default :
				break;
		}

		if (Get_Text_Display()) {
			Get_Text_Display()->Set_Input_Text( mess );
			Get_Text_Display()->Set_Help_Text( HelpLine );
		}

	} else {

		if (Get_Text_Display()) {
			Vector3 color(1,1,1);
			Get_Text_Display()->Set_Input_Text( "" );
			Get_Text_Display()->Set_Help_Text( "" );
		}

	}

	// Note:  As you add more stats, also add to the stats help command.

	/****************************************************************************************
	**
	** Frame-Rate Stats.  Activate with 'stats fps'
	**
	****************************************************************************************/

	if (Get_Text_Display()) {

		// Update text if it's visible
		if (StatisticsDisplayManager::Is_Current_Display("histogram")) {
			StringClass working_string(true);
			StringClass message(true);
			float	cur_time = TimeManager::Get_Seconds();

			FPSTime += cur_time - FPSLastTime;
			FPSLastTime = cur_time;
			FPSFrames++;
			if ( FPSTime >= 1.0f ) {
				FPS = (float)FPSFrames/FPSTime;
				FPSTime = 0.0f;
				FPSFrames = 0;
			}

			working_string.Format("%2.0f fps\n\n",FPS);
			message += working_string;

			unsigned slot_count=TimeManager::Peek_Frame_Time_Histogram().Get_Slot_Count();
			if (slot_count) {
				unsigned char* slots=new unsigned char[slot_count];
				TimeManager::Peek_Frame_Time_Histogram().Get_Packed_Report(slots);
				unsigned i;
				for (i=0;i<slot_count;++i) {
					working_string.Format("%d: %d\n",
						unsigned(TimeManager::Peek_Frame_Time_Histogram().Get_Step()*float(i)),
						slots[i]);
					message+=working_string;
				}
				delete[] slots;
			}
			StatisticsDisplayManager::Set_Stat( "histogram", message );
		}

		// Update text if it's visible
		if (StatisticsDisplayManager::Is_Current_Display("fps")) {
			StringClass working_string(true);
			StringClass message(true);
			float	cur_time = TimeManager::Get_Seconds();

			FPSTime += cur_time - FPSLastTime;
			FPSLastTime = cur_time;
			FPSFrames++;
			if ( FPSTime >= 1.0f ) {
				FPS = (float)FPSFrames/FPSTime;
				FPSTime = 0.0f;
				FPSFrames = 0;
			}

			working_string.Format("%2.0f fps\n",FPS);
			message += working_string;

			working_string.Format(
				"\npolys/frame: %7d\npolys/second %4dk\n",
				WW3D::Get_Last_Frame_Poly_Count(),
				int(WW3D::Get_Last_Frame_Poly_Count()*FPS/1000));
			message += working_string;

			unsigned ffheap=0;
			unsigned ffuse=0;
			unsigned actualuse=0;
			unsigned count=0;
			working_string.Format(
				"\nMalloc count: %d\n"
				"Free count: %d\n"
				"FF Heap: %d.%3.3d.%3.3d (%d Mb)\n"
				"FF Use: %d.%3.3d.%3.3d (%d Mb)\n"
				"Actual Use: %d.%3.3d.%3.3d (%d Mb)\n"
				"FF Count: %d\n"
				,
				WW3D::Get_Last_Frame_Memory_Allocation_Count(),
				WW3D::Get_Last_Frame_Memory_Free_Count(),
				ffheap/(1000*1000),(ffheap/1000)%1000,ffheap%1000, ffheap/(1024*1024),
				ffuse/(1000*1000),(ffuse/10000)%1000,ffuse%1000, ffuse/(1024*1024),
				actualuse/(1000*1000),(actualuse/1000)%1000,actualuse%1000, actualuse/(1024*1024),
				count);
			message += working_string;

			working_string.Format("verts/frame: %7d v/p ratio: %2.2f\n",
					WW3D::Get_Last_Frame_Vertex_Count(),
					float(WW3D::Get_Last_Frame_Vertex_Count())/float(WW3D::Get_Last_Frame_Poly_Count()));
			message += working_string;

			int texture_reduction = WW3D::Get_Texture_Reduction();
			if (texture_reduction > 0) {
				working_string.Format("Tex. Red. %d\n\n", texture_reduction);
				message += working_string;
			}

			// Only display texture and vertex processor statistics if texture statistics recording is enabled

			if (Debug_Statistics::Get_Record_Texture_Mode()!=Debug_Statistics::RECORD_TEXTURE_NONE) {
				// Texture usage
				int red_size=Debug_Statistics::Get_Record_Texture_Size();
				int lightmap_size=Debug_Statistics::Get_Record_Lightmap_Texture_Size();
				int procedural_size=Debug_Statistics::Get_Record_Procedural_Texture_Size();
				working_string.Format(
					"\n"
					"textures/frame: %5d\n"
					"tex memory used: %d.%dMb\n"
					"texture changes: %d\n"
					,
					Debug_Statistics::Get_Record_Texture_Count(),
					red_size>>20,
					(10*(red_size>>10)>>10)%10,
					Debug_Statistics::Get_Record_Texture_Change_Count());
				message += working_string;

				// Lightmap info
				working_string.Format(
					"\n"
					"lightmaps/frame: %5d\n"
					"lightmap memory used: %d.%dMb\n"
					,
					Debug_Statistics::Get_Record_Lightmap_Texture_Count(),
					lightmap_size>>20,
					(10*(lightmap_size>>10)>>10)%10);
				message += working_string;

				// Procedural texture info
				working_string.Format(
					"\n"
					"procedural textures/frame: %5d\n"
					"procedural memory used: %d.%dMb\n\n"
					,
					Debug_Statistics::Get_Record_Procedural_Texture_Count(),
					procedural_size>>20,
					(10*(procedural_size>>10)>>10)%10);
				message += working_string;

				// Texture info
				red_size=TextureClass::_Get_Total_Texture_Size();
				lightmap_size=TextureClass::_Get_Total_Lightmap_Texture_Size();
				procedural_size=TextureClass::_Get_Total_Procedural_Texture_Size();
				working_string.Format(
					"\n"
					"total tex loaded: %5d\n"
					"total size of textures: %d.%dMb\n"
					"\n"
					"total lightmaps: %5d\n"
					"total size of lightmaps: %dMb\n"
					"\n"
					"total procedural textures: %5d\n"
					"total size of procedural textures: %dMb\n"
					,
					TextureClass::_Get_Total_Texture_Count(),
					red_size>>20,
					(10*(red_size>>10)>>10)%10,
					TextureClass::_Get_Total_Lightmap_Texture_Count(),
					lightmap_size>>20,
					(10*(lightmap_size>>10)>>10)%10,
					TextureClass::_Get_Total_Procedural_Texture_Count(),
					procedural_size>>20,
					(10*(procedural_size>>10)>>10)%10);
				message += working_string;

				// Thumbnail info
				red_size=TextureClass::_Get_Total_Locked_Surface_Size();
				working_string.Format(
					"total thumbnails: %5d\n"
					"total size of thumbnails: %d.%dMb\n"
					,
					TextureClass::_Get_Total_Locked_Surface_Count(),
					red_size>>20,
					(10*(red_size>>10)>>10)%10);
				message += working_string;

				if (Debug_Statistics::Get_Record_Texture_Mode()==Debug_Statistics::RECORD_TEXTURE_DETAILS) {
					message+="\n"
								"<F9 + F5> Scroll up\n"
								"<F9 + F6> Scroll down\n"
								"\n";

					message+=Debug_Statistics::Get_Record_Texture_String();
				}

			}

			if (Debug_Statistics::Get_Record_Texture_Mode()==Debug_Statistics::RECORD_TEXTURE_DETAILS) {
				StatisticsDisplayManager::Set_Stat( "fps", message, 0xffffffcf );
			}
			else {
				StatisticsDisplayManager::Set_Stat( "fps", message );
			}
		}
	}

   if ( Input::Get_State( INPUT_FUNCTION_CNC )) {
		ConsoleFunctionManager::Parse_Input( "CNC" );
	}
}

/*
**
*/
void 	ConsoleGameModeClass::Parse_Input( char * string )
{
	if ( ConsoleInputType == INPUT_FUNCTION_BEGIN_PUBLIC_MESSAGE ||
        ConsoleInputType == INPUT_FUNCTION_BEGIN_TEAM_MESSAGE   ||
        ConsoleInputType == INPUT_FUNCTION_BEGIN_PRIVATE_MESSAGE ) {

      if (GameModeManager::Find("Combat")->Is_Active()) {
			/*
			if (cNetwork::I_Am_Client()) {
				cNetwork::Send_Client_Text_Message(string, ConsoleInputType);
			} else {

				WideStringClass text;
				text.Convert_From(string);
				if (!text.Is_Empty()) {
					cScTextObj * p_test_obj = new cScTextObj;
					p_test_obj->Init(text, TEXT_MESSAGE_PUBLIC, HOST_TEXT_SENDER);
				}
			}
			*/
      }
	} else {
		ConsoleFunctionManager::Parse_Input( string );
	}
}

void ConsoleGameModeClass::Clear_Suggestion(void)
{
	memset(Suggestion,0,sizeof(Suggestion));
	memset(HelpLine,0,sizeof(HelpLine));
}

void ConsoleGameModeClass::Accept_Suggestion(char * cmd)
{
	if (ConsoleInputType == INPUT_FUNCTION_BEGIN_CONSOLE) {
		// If a space has already been entered or there is no suggestion, do nothing
		if ((strchr(cmd,' ') == NULL) && (strlen(Suggestion) > 0)) {
			strcpy(cmd,Suggestion);
		}
	}
}

void ConsoleGameModeClass::Update_Suggestion(char * cmd,bool go_to_next)
{
	if (ConsoleInputType == INPUT_FUNCTION_BEGIN_CONSOLE) {

		// If a space has already been entered don't update so that the help stays up
		if ((strlen(cmd) > 0) && (strchr(cmd,' ') == NULL)) {
			char * cur_suggestion = NULL;
			if ((go_to_next) && (strlen(Suggestion) > 0)) {
				cur_suggestion = &(Suggestion[0]);
			}

			bool gotone = ConsoleFunctionManager::Get_Command_Suggestion(cmd,cur_suggestion,Suggestion,HelpLine,sizeof(Suggestion));
			if (!gotone) {
				Clear_Suggestion();
			}
		}

	}
}
