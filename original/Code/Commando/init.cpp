#include "global.h"
#include "init.h"
#include "wwmath.h"
#include "ww3d.h"
#include "wwphys.h"
#include "WWAudio.H"
#include "wwsaveload.h"
#include "input.h"
#include "inputconfigmgr.h"
//#include "gamesettings.h"
#include "ffactory.h"
#include "assets.h"
#include "_globals.h"
#include "combatgmode.h"	// gamemode
#include "langmode.h"		// gamemode
#include "movie.h"			// gamemode
#include "gamemenu.h"		// gamemode
#include "overlay.h"			// gamemode
#include "multihud.h"		// gamemode
#include "console.h"			// gamemode
#include "textdisplay.h"	// gamemode
#include "scorescreen.h"	// gamemode
#include "msgloop.h"
#include "Resource.H"
#include "miscutil.h"
#include "cnetwork.h"
#include "mathutil.h"
#include "win.h"
#include "Part_Ldr.H"
#include "savegame.H"
#include	"systemsettings.h"
#include "gamedata.h"
#include "sphereobj.h"
#include "ringobj.h"
#include "diagnostics.h"
#include "devoptions.h"
#include "translatedb.h"
#include "useroptions.h"
#include "pathmgr.h"
#include "string_ids.h"
#include "soundrobj.h"
#include "crandom.h"
#include "systimer.h"
#include "renegadedialogmgr.h"
#include "campaign.h"
#include "diaglog.h"
#include "mpsettingsmgr.h"
#include "mixfile.h"
#include "ffactorylist.h"
#include "gameinitmgr.h"
#include "serverfps.h"
#include "nicenum.h"
#include "encyclopediamgr.h"
#include "texturethumbnail.h"
#include "playermanager.h"
#include "teammanager.h"
#include "stackdump.h"
#include "registry.h"
#include "bandwidthgraph.h"
#include "buildnum.h"
#include "dx8wrapper.h"
#include "except.h"
#include "except.h"
#include "consolemode.h"
#include "serversettings.h"
#include "pscene.h"
#include "dazzle.h"
#include "skinpackagemgr.h"
#include "modpackagemgr.h"
#include "shutdown.h"

extern const char *VALUE_NAME_TEXTURE_FILTER_MODE;

/*
** Define IMMEDIATE_LOAD if you want to have the game start right
** up in a given level (for profiling for example)
*/

// HACK: ATI demo loads the demo level bypassing the mainmenu
#define IMMEDIATE_LOAD						0
#define IMMEDIATE_LOAD_LEVELNAME			"MDD_0803.mix"

/*
** This defines the subdirectory where the game will load all data from
*/
const char *	DATA_SUBDIRECTORY			= "DATA/";
const char *	SAVE_SUBDIRECTORY			= "DATA/SAVE/";
const char *	CONFIG_SUBDIRECTORY		= "DATA/CONFIG/";
const char *	MOVIES_SUBDIRECTORY		= "DATA/MOVIES/";

#define	STRINGS_FILENAME					"STRINGS.TDB"
#define	CONV_DB_FILENAME					"CONV10.CDB"

/*
** Used to modify where game entries are kept in the registry.
*/
extern char DefaultRegistryModifier[1024];

/*
** Static global lod settings for particles
*/
static const float _ParticleLODScreenSizes[17] =
{
	0.0025f,
	0.0050f,
	0.0075f,
	0.0100f,
	0.0300f,
	0.0500f,
	0.0700f,
	0.0900f,
	0.1000f,
	0.3000f,
	0.5000f,
	1.0000f,
	1.2500f,
	1.5000f,
	1.7500f,
	2.0000f,
	NO_MAX_SCREEN_SIZE
};

/***********************************************************************************************
 * Append_To_Assert_History --  keeps a timestamped history of asserts.                        *
 *                                                                                             *
 * INPUT:                                                                                      *
 *                                                                                             *
 * OUTPUT:                                                                                     *
 *                                                                                             *
 * WARNINGS:                                                                                   *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   9/26/2001  tss : Created.                                                                 *
 *=============================================================================================*/
void Append_To_Assert_History(const char * message)
{
	if (message == NULL) {
		//
		// bail
		//
		return;
	}

	FILE * file = ::fopen("_asserts.txt", "at");
   if (file == NULL) {
		return;
	}

	char line[2000] = "";

	//
	// Blank lines plus timestamp
	//
	::sprintf(line, "\n\nWhen:       %s\n", cMiscUtil::Get_Text_Time());
	::fwrite(line, 1, ::strlen(line), file);

	//
	// version #
	//
	::sprintf(line, "Version:    %d\n", 0);
	::fwrite(line, 1, ::strlen(line), file);

	//
	// Full filename
	//
	char full_filename[MAX_PATH];
	::GetModuleFileName(NULL, full_filename, sizeof(full_filename));
	::sprintf(line, "Filename:   %s\n", full_filename);
	::fwrite(line, 1, ::strlen(line), file);

	//
	// File size
	//
	HANDLE hfile = ::CreateFile(full_filename, 0, 0, NULL, OPEN_EXISTING, 0L, NULL);
	if (hfile != INVALID_HANDLE_VALUE)
	{
		DWORD file_size = ::GetFileSize(hfile, NULL);
		::CloseHandle(hfile);
		::sprintf(line, "Filesize:   %d\n", file_size);
		::fwrite(line, 1, ::strlen(line), file);
	}

	//
	// The assert message itself
	//
	::sprintf(line, "Assert:     %s\n", message);
	::fwrite(line, 1, ::strlen(line), file);

	::fclose(file);
}

/***********************************************************************************************
 * Commando_Assert_Handler -- Commando callback function for WWASSERT's                        *
 *                                                                                             *
 * INPUT:                                                                                      *
 *                                                                                             *
 * OUTPUT:                                                                                     *
 *                                                                                             *
 * WARNINGS:                                                                                   *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   3/28/2000  gth : Created.                                                                 *
 *=============================================================================================*/
void Commando_Assert_Handler(const char * message)
{
	if (message == NULL) {
		//
		// bail
		//
		return;
	}

	//
	// TSS092601
	//
	Append_To_Assert_History(message);

	//
	// If the exception handler is try to quit the game then don't show an assert.
	//
	if (Is_Trying_To_Exit()) {
		ExitProcess(0);
	}

	//
	// Hand the message off to the scrolling debug screen
	//
	//
	//
	//    IF YOU WANT TO DISABLE CERTAIN ACTIONS HERE, USE
	//    THE REGISTRY SWITCHES!!!!!!!!!!!!!
	//
	//
	RegistryClass registry( APPLICATION_SUB_KEY_NAME_DEBUG );
	if ( registry.Is_Valid() ) {
		registry.Set_Int( VALUE_NAME_APPLICATION_CRASH_VERSION, 0 );

	}

}

/*
**
*/
void _stdcall AudioTextCallback(AudibleSoundClass *sound_obj, const StringClass &text, uint32_t user_param)
{
	Vector3 red = Vector3( 1, 0.5f, 0.5f );
	StringClass str;
	str.Format( "%s\n", (const char *)text );
}

/*
**
*/
class LoggingFileFactoryClass : public SimpleFileFactoryClass {
public:
	virtual FileClass * Get_File( const char * filename ) {
		FileClass * file = BaseFactory->Get_File( filename );
		Debug_Say(( "FILE_LOG : %s %s\n", filename, ((file==NULL||!file->Is_Available())?"MISSING":"") ));
		return file;
	}
	void Set_Base_Factory( FileFactoryClass * factory ) { BaseFactory = factory; }
private:
	FileFactoryClass * BaseFactory;
};

/*
**
*/
class StrippingFileFactoryClass : public SimpleFileFactoryClass {
public:
	virtual FileClass * Get_File( const char * filename ) {
		StringClass stripped(true);
		Strip_Path_From_Filename( stripped, filename );
		return BaseFactory->Get_File( stripped );
	}
	void Set_Base_Factory( FileFactoryClass * factory ) { BaseFactory = factory; }
private:
	FileFactoryClass * BaseFactory;
};

/*
**
*/
SimpleFileFactoryClass	RenegadeWritingFileFactory;

SimpleFileFactoryClass	RenegadeBaseFileFactory;
MixFileFactoryClass	*	AlwaysMixFileFactory;
FileFactoryListClass		_RenegadeFileFactory;
StrippingFileFactoryClass	AudioFileFactory;
LoggingFileFactoryClass		LoggingFileFactory;

/*
**
*/
void	Construct_Directory_Structure(void)
{
	//
	//	Lookup the path of the executable
	//
	char path[MAX_PATH] = { 0 };
	::GetModuleFileName (NULL, path, sizeof (path));

	//
	//	Strip off the filename
	//
	char *filename = ::strrchr (path, '/');
	if (filename != NULL) {
		filename[1] = 0;
	}

	StringClass data_dir(path,true);
	data_dir += "data";

	StringClass save_dir(data_dir + "/save",true);
	StringClass config_dir(data_dir + "/config",true);

	//
	//	Create the data directory if necessary
	//
	if (GetFileAttributes (data_dir) == 0xFFFFFFFF) {
		::CreateDirectory (data_dir, NULL);
	}

	//
	//	Create the save directory if necessary
	//
	if (GetFileAttributes (save_dir) == 0xFFFFFFFF) {
		::CreateDirectory (save_dir, NULL);
	}

	//
	//	Create the config directory if necessary
	//
	if (GetFileAttributes (config_dir) == 0xFFFFFFFF) {
		::CreateDirectory (config_dir, NULL);
	}

	return ;
}

static bool Verify_Log_Directory(const StringClass& folder)
{
	if (GetFileAttributes(folder)!=0xffffffff) return true;
	//HANDLE file;
	//file = CreateFile(folder, 0, 0, NULL, OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, NULL);
	//if (file!=INVALID_HANDLE_VALUE) {
	//	CloseHandle(file);
	//	return true;
	//}

	if (CreateDirectory(folder,NULL)) {
		return true;
	}
	if (GetLastError() == ERROR_ALREADY_EXISTS) {
		return(true);
	}
	return false;
}

static bool Create_Log_File_Name(const StringClass& folder, StringClass& filename,bool use_numbering)
{
	StringClass original(filename);
	if (!use_numbering) {
		filename.Format("%s/%s",(const char*)folder,(const char*)original);
		return true;
	}
	for (int i=0;i<999;++i) {
		HANDLE file;
		filename.Format("%s/%3.3d%s",(const char*)folder,i,(const char*)original);
		file = CreateFile(filename, GENERIC_WRITE, 0, NULL, CREATE_NEW, FILE_ATTRIBUTE_NORMAL, NULL);
		if (file!=INVALID_HANDLE_VALUE) {
			CloseHandle(file);
			return true;
		}
	}
	return false;
}

static void Copy_Log(const StringClass& folder,const char* filename,bool use_numbering)
{
	RawFileClass raw_log_file(filename);
	if (raw_log_file.Is_Available()) {
		int size=raw_log_file.Size();
		if (size) {
			StringClass log_file_name(filename);
			if (Create_Log_File_Name(folder,log_file_name,use_numbering)) {
				DWORD written;
				HANDLE file;
				file = CreateFile(log_file_name, GENERIC_WRITE, 0, NULL, CREATE_ALWAYS, FILE_ATTRIBUTE_NORMAL, NULL);
				if (INVALID_HANDLE_VALUE != file) {
					raw_log_file.Open();
					unsigned char* memory=new unsigned char[size];
					raw_log_file.Read(memory,size);
					raw_log_file.Close();
					WriteFile(file, memory, size, &written, NULL);
					delete[] memory;
					CloseHandle(file);
				}
			}
		}
	}
}

void Application_Exception_Callback(void)
{
	RegistryClass registry( APPLICATION_SUB_KEY_NAME_DEBUG );
	if ( registry.Is_Valid() ) {
		registry.Set_Int( VALUE_NAME_APPLICATION_CRASH_VERSION, 0 );
	}
}

bool RestartNeeded = true;

/***********************************************************************************************
 * Get_Version_Number -- Collate version information.                                          *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    Ptr to var to fill with major version number                                      *
 *           Ptr to var to fill with minor version number                                      *
 *                                                                                             *
 *                                                                                             *
 * OUTPUT:   Nothing                                                                           *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   12/3/2001 11:26PM ST : Created                                                            *
 *=============================================================================================*/
void Get_Version_Number(DWORD *major, DWORD *minor)
{
	// Version info removed per Legal review requirements. LFeenanEA - 8th February 2025

	unsigned long version_major = 0;
	unsigned long version_minor = 0;
}

/*
**
*/
/*
**
*/
bool Game_Init(void)
{
	fprintf(stderr, "[trace] Game_Init() start\n");

	// Set registry key to 1 for the duration of the init. This way we know if the program crashed while the init.
	RegistryClass registry( APPLICATION_SUB_KEY_NAME_DEBUG );
	if ( registry.Is_Valid() ) {
		registry.Set_Int( VALUE_NAME_GAME_INITIALIZATION_IN_PROGRESS, 1 );
		unsigned crash_version=registry.Get_Int( VALUE_NAME_APPLICATION_CRASH_VERSION, 0 );
		registry.Set_Int( VALUE_NAME_APPLICATION_CRASH_VERSION, 0 );
	}

	//
	//	Ensure our directory structure exists
	//
	fprintf(stderr, "[trace] Construct_Directory_Structure...\n");
	Construct_Directory_Structure ();

	//
	//	Initialize our debugging framework
	//

	BuildInfoClass::Log_Build_Info();

//CRC_Check();

	Get_Version_Number(NULL, NULL);

	fprintf(stderr, "[trace] Setting up file factories...\n");
	// setup Writing Factory
	RenegadeWritingFileFactory.Set_Sub_Directory( DATA_SUBDIRECTORY );
	_TheWritingFileFactory = &RenegadeWritingFileFactory;

	RenegadeBaseFileFactory.Set_Sub_Directory( DATA_SUBDIRECTORY );
	RenegadeBaseFileFactory.Append_Sub_Directory( SAVE_SUBDIRECTORY );
	RenegadeBaseFileFactory.Append_Sub_Directory( CONFIG_SUBDIRECTORY );

	_TheSimpleFileFactory->Set_Sub_Directory( DATA_SUBDIRECTORY );
	_TheSimpleFileFactory->Append_Sub_Directory( SAVE_SUBDIRECTORY );
	_TheSimpleFileFactory->Append_Sub_Directory( CONFIG_SUBDIRECTORY );

	_TheSimpleFileFactory->Set_Strip_Path( true );

	_RenegadeFileFactory.Add_FileFactory( &RenegadeBaseFileFactory, "" );
	_RenegadeFileFactory.Add_FileFactory( new MixFileFactoryClass( "Always2.dat", &RenegadeBaseFileFactory ), "Always2.dat" );
	_RenegadeFileFactory.Add_FileFactory( new MixFileFactoryClass( "Always.dbs", &RenegadeBaseFileFactory ), "Always.dbs" );
	_RenegadeFileFactory.Add_FileFactory( new MixFileFactoryClass( "Always.dat", &RenegadeBaseFileFactory ), "Always.dat" );

	//
	//	Search for all mix files in the data directory
	//
	WIN32_FIND_DATA find_info	= { 0 };
	BOOL keep_going				= TRUE;
	HANDLE file_find				= NULL;
	for (file_find = ::FindFirstFile ("data/*.mix", &find_info);
		 (file_find != INVALID_HANDLE_VALUE) && keep_going;
		  keep_going = ::FindNextFile (file_find, &find_info))
	{
		//
		//	Add this mix file to our mix file factory list
		//
		_RenegadeFileFactory.Add_FileFactory( new MixFileFactoryClass (find_info.cFileName, &RenegadeBaseFileFactory ), find_info.cFileName );
	}

	//
	//	Close the search handle
	//
	if (file_find != INVALID_HANDLE_VALUE) {
		::FindClose (file_find);
	}

	_TheFileFactory = &_RenegadeFileFactory;

	// Logging File Factory
	if ( false ) {
		LoggingFileFactory.Set_Base_Factory( &_RenegadeFileFactory );
		_TheFileFactory = &LoggingFileFactory;
	}

	AudioFileFactory.Set_Base_Factory( _TheFileFactory );

	//Setup_Mix_File();

	// Lets seed the Random Generator, a little
	int count = TIMEGETTIME() & 0xFF;
	while ( count-- > 0 ) {
		FreeRandom.Get_Int();
	}

	// Thumbnail manager pre init will ensure that thumbnail database
	// is up-to-date.
	bool show_thumbnail_pre_init_dialog = true;
//	ThumbnailManagerClass::Pre_Init(show_thumbnail_pre_init_dialog);

	//
	// Create an instance of the sound library
	//
	fprintf(stderr, "[trace] WWAudioClass init...\n");
	new WWAudioClass(ConsoleBox.Is_Exclusive());
	WWAudioClass::Get_Instance()->Initialize( APPLICATION_SUB_KEY_NAME_SOUND );
	WWAudioClass::Get_Instance()->Set_File_Factory( &AudioFileFactory );
	// Install text callback
	WWAudioClass::Get_Instance()->Register_Text_Callback(AudioTextCallback,0);

	//
	//	Load the multiplayer settings
	//
	MPSettingsMgrClass::Load_Settings ();

	/*
	**
	*/
	WW3DAssetManager * asset_manager = new WW3DAssetManager;
//	asset_manager->Open_Texture_File_Cache("cache_");
	asset_manager->Set_WW3D_Load_On_Demand( true );
	asset_manager->Register_Prototype_Loader(&_ParticleEmitterLoader);
	asset_manager->Register_Prototype_Loader(&_SphereLoader);
	asset_manager->Register_Prototype_Loader(&_RingLoader);
	asset_manager->Register_Prototype_Loader (&_SoundRenderObjLoader);
	asset_manager->Set_Activate_Fog_On_Load (true);

	//GameSettings::Init();

	// Initialize WWMath
	WWMath::Init();

	// Initialize the pathfind system
	PathMgrClass::Initialize ();

	// Initialize WW3D
	fprintf(stderr, "[trace] WW3D::Init(MainWindow=%p, exclusive=%d)...\n", (void*)MainWindow, ConsoleBox.Is_Exclusive());
	int ww3d_init_result = WW3D::Init(MainWindow, NULL, ConsoleBox.Is_Exclusive() ? true : false);
	fprintf(stderr, "[trace] WW3D::Init returned %d\n", ww3d_init_result);
	switch (ww3d_init_result) {
	case WW3D_ERROR_OK:	// Success!
		break;
	case WW3D_ERROR_DIRECTX8_INITIALIZATION_FAILED:
	default:
		fprintf(stderr, "[trace] WW3D::Init FAILED (code=%d) - returning false\n", ww3d_init_result);
		::MessageBox(NULL,
			"DirectX 8.0 or later is required to play C&C:Renegade.",
			"Renegade Graphics Initialization Error.",
			MB_OK);
		return false;
	}

	if (ConsoleBox.Is_Exclusive()) {
		WW3D::Enable_Decals(false);
		PhysicsSceneClass * scene = PhysicsSceneClass::Get_Instance();
		scene->Set_Max_Simultaneous_Shadows(0);
		DazzleRenderObjClass::Enable_Dazzle_Rendering(false);
	} else {
		fprintf(stderr, "[trace] WW3D::Registry_Load_Render_Device...\n");
		if ( WW3D::Registry_Load_Render_Device( APPLICATION_SUB_KEY_NAME_RENDER, true ) != WW3D_ERROR_OK ) {
			fprintf(stderr, "[trace] WW3D::Registry_Load_Render_Device FAILED - returning false\n");
			return false;
		}
		fprintf(stderr, "[trace] WW3D::Registry_Load_Render_Device OK\n");

		fprintf(stderr, "[trace] WW3D::Registry_Save_Render_Device...\n");
		if ( WW3D::Registry_Save_Render_Device( APPLICATION_SUB_KEY_NAME_RENDER ) != WW3D_ERROR_OK ) {
			fprintf(stderr, "[trace] WW3D::Registry_Save_Render_Device FAILED - returning false\n");
			return false;
		}
		fprintf(stderr, "[trace] WW3D::Registry_Save_Render_Device OK\n");
		WW3D::Enable_Static_Sort_Lists (true);
	}
	if (ServerSettingsClass::Is_Command_Line_Mode() || ConsoleBox.Is_Exclusive()) {
		if (!ConsoleBox.Is_Exclusive()) {
			::ShowWindow( MainWindow, SW_MINIMIZE );	// minimize if we are starting automatically.
		}
		ConsoleBox.Init();
	} else {
		::ShowWindow( MainWindow, SW_SHOW );	// show the (initially hidden) window
	}

	// Clear screen
	for (int frame=0;frame<3;++frame) {
		WW3D::Begin_Render(true,true,Vector3(0.0f,0.0f,0.0f));
		WW3D::End_Render();
	}

	ParticleEmitterClass::Set_Default_Remove_On_Complete(false);	// (gth) 09/17/2000 - by default emitters shouldn't self-destruct

	for (int i=0; i<17; i++) {
		ParticleBufferClass::Set_LOD_Max_Screen_Size(i,_ParticleLODScreenSizes[i]);
	}

	if ( cUserOptions::PermitDiagLogging.Is_True() &&
		  false ) {

		DiagLogClass::Init();
	}

	fprintf(stderr, "[trace] WWPhys::Init / WWSaveLoad::Init...\n");
	WWPhys::Init();
	WWSaveLoad::Init();

	//
	//	Load the strings table
	//
	TranslateDBClass::Initialize();
	FileClass *file	= _TheFileFactory->Get_File( STRINGS_FILENAME );
	if (file != NULL) {
		file->Open (FileClass::READ);				//	Open or the file
		if ( file->Is_Available() ) {
			ChunkLoadClass cload (file);				// Load the database
			SaveLoadSystemClass::Load(cload);
		}
		file->Close ();								// Close the file
		_TheFileFactory->Return_File (file);
	}
	//TranslateDBClass::Set_Current_Language (TranslateDBClass::LANGID_CHINESE);

	//
	//	Initialize the input control system
	//
	bool dinput_avail = (ConsoleBox.Is_Exclusive()) ? false : true;

	fprintf(stderr, "[trace] Input::Init(dinput_avail=%d)...\n", dinput_avail);
	Input::Init(dinput_avail);
	Input::Load_Registry( APPLICATION_SUB_KEY_NAME_CONTROLS );
	InputConfigMgrClass::Initialize();

	//
	//	Initialize the skin selection framework
	//
	SkinPackageMgrClass::Initialize ();
	ModPackageMgrClass::Initialize ();
	ModPackageMgrClass::Build_List ();

	//
	//	Load the conversation database
	//
	file	= _TheFileFactory->Get_File( CONV_DB_FILENAME );
	if (file != NULL) {
		file->Open (FileClass::READ);				//	Open or the file
		if ( file->Is_Available() ) {
			ChunkLoadClass cload (file);				// Load the database
			SaveLoadSystemClass::Load(cload);
		}
		file->Close ();								// Close the file
		_TheFileFactory->Return_File (file);
	}

	//
	//	Check to make sure the code version matches the strings
	// table version
	//
	if (TranslateDBClass::Get_Version_Number () != STRINGS_VER) {
		MessageBox( 0,
			"This build of Renegade is out of sync with the strings database (strings.tdb).  Strings will be incorrect and may cause the game to crash.",
			"Version Error",
			MB_OK | MB_ICONEXCLAMATION | MB_SETFOREGROUND  );
	}

	//
	//	Note:  Due to interdependencies (yuck!) between these subsystems, here's the
	// order they need to be initialized in:
	//
	//		- Physics Scene Class (via CombatManager::Scene_Init)
	//		- SystemSettings
	//		- RenegadeDialogMgrClass
	//
	//
	fprintf(stderr, "[trace] CombatManager::Scene_Init...\n");
	CombatManager::Scene_Init();
	if (!ConsoleBox.Is_Exclusive()) {
		SystemSettings::Init();
	}
   RenegadeDialogMgrClass::Initialize();

	cNetwork::Onetime_Init();

	cServerFps::Create_Instance();

	cPlayerManager::Onetime_Init();
	cTeamManager::Onetime_Init();
	cGameData::Onetime_Init();
	cBandwidthGraph::Onetime_Init();

   fprintf(stderr, "[trace] cNetUtil::Wsa_Init...\n");
   cNetUtil::Wsa_Init();

	CombatManager::Init(ConsoleBox.Is_Exclusive() ? false : true);

	CampaignManager::Init();

	// This order is also draw and think order
	GameModeManager::Add( new CombatGameModeClass );
	GameModeManager::Add( new LanGameModeClass );
	GameModeManager::Add( new OverlayGameModeClass );
	GameModeManager::Add( new MenuGameModeClass2 );		// Putting menu after overlay so we see the text
	GameModeManager::Add( new MovieGameModeClass );
	GameModeManager::Add( new ConsoleGameModeClass );
	GameModeManager::Add( new ScoreScreenGameModeClass );
	//GameModeManager::Add( new LobbyGameModeClass );
	GameModeManager::Add( new TextDisplayGameModeClass );
	GameModeManager::Add( new Overlay3DGameModeClass );

	GameModeManager::Find( "Overlay3D" )->Activate();
	GameModeManager::Find( "Overlay" )->Activate();
	GameModeManager::Find( "Console" )->Activate();
	GameModeManager::Find( "Menu" )->Activate();
	GameModeManager::Find( "TextDisplay" )->Activate();

	// After TextDisplay is created, install the Display Handler
	//DEADMENU MenuManager::Set_Menu( "Menu_Main" );

	// Load the accelerator table and hand it off to WWLIB.
	// Note:  Accelerator tables that are loaded from resources (like
	// we are doing here) do not need to be manually freed.  Windows
	// will cleanup for us when the process terminates.
	HACCEL haccel = ::LoadAccelerators (::GetModuleHandle (NULL), MAKEINTRESOURCE (IDR_ACCELERATOR));
	if (haccel) {
		::Add_Accelerator (MainWindow, haccel);
	}

	//WW3D::Set_Texture_Reduction( 1 );

	//
	//	Initialize the encyclopedia logic
	//
	EncyclopediaMgrClass::Initialize ();

#if (IMMEDIATE_LOAD)
	// Immediately load up the specified level
	GameInitMgrClass::Initialize_SP ();
	GameInitMgrClass::Start_Game (IMMEDIATE_LOAD_LEVELNAME);
#endif

	cDiagnostics::Init();
	//cHelpText::Init();

	//
	// NIC initialization
	//
	cNicEnum::Init();

	if ( registry.Is_Valid() ) {
		registry.Set_Int( VALUE_NAME_GAME_INITIALIZATION_IN_PROGRESS, 0 );
		registry.Set_Int( VALUE_NAME_APPLICATION_CRASH_VERSION, 0 );
	}

#if (IMMEDIATE_LOAD==0)


	//
	// Parse the server settings files if they will be used soon to make sure there are no errors.
	//
	if (ServerSettingsClass::Is_Command_Line_Mode()) {
		fprintf(stderr, "[trace] ServerSettingsClass::Parse...\n");
		if (!ServerSettingsClass::Parse(false)) {
			fprintf(stderr, "[trace] ServerSettingsClass::Parse FAILED - returning false\n");
			Game_Shutdown();
			return (false);
		}
	}

	//
	// Start up intro movies.
	//
	MovieGameModeClass * mode = (MovieGameModeClass *)GameModeManager::Find ("Movie");
	if ( mode ) {
		mode->Activate();
		mode->Startup_Movies( );
	}


#endif

	fprintf(stderr, "[trace] Game_Init() complete - returning true\n");
	return true;
}

/***********************************************************************************************
 * Build_Registry_Location_String -- Get a complete path to a registry location                *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    Game base path                                                                    *
 *           Base modifier path                                                                *
 *           Sub path                                                                          *
 *                                                                                             *
 * OUTPUT:   Ptr to complete path                                                              *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   11/9/2001 3:39PM ST : Created                                                             *
 *=============================================================================================*/
char *Build_Registry_Location_String(char *base, char *modifier, char *sub)
{
	static char _whole_registry_string[1024];

	if (modifier == NULL) {
		modifier = DefaultRegistryModifier;
	}

	if (base && *base != 0) {
		strcpy(_whole_registry_string, base);
	}
	if (modifier && *modifier != 0) {
		strcat(_whole_registry_string, "\\");
		strcat(_whole_registry_string, modifier);
	}
	if (sub && *sub != 0) {
		strcat(_whole_registry_string, "\\");
		strcat(_whole_registry_string, sub);
	}
	return(_whole_registry_string);
}
