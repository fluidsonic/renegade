#ifndef	SCRIPTCOMMANDS_H
#define	SCRIPTCOMMANDS_H

#ifndef	VECTOR3_H
	#include "vector3.h"
#endif

#ifndef	COMBATSOUND_H
	#include "combatsound.h"
#endif

#ifndef	ACTIONPARAMS_H
	#include "actionparams.h"
#endif


/*
** DLL import/export macros
*/
#ifdef	BUILDING_DLL
	#define	SCRIPT_DLL_COMMAND	_declspec(dllimport)
#else
	#define	SCRIPT_DLL_COMMAND	extern "C" _declspec(dllexport)
#endif

/*
** Types
*/
class		ScriptableGameObj;
typedef	ScriptableGameObj	GameObject;
class		AudibleSoundClass;
typedef	AudibleSoundClass	Sound2D;
class		Sound3DClass;
typedef	Sound3DClass		Sound3D;
class		ScriptClass;
class		ScriptSaver;
class		ScriptLoader;


/*
** Script Commands
*/


/*
** MISC Script ENUMS
*/
enum {
		OBJECTIVE_TYPE_PRIMARY 				= 1,
		OBJECTIVE_TYPE_SECONDARY,
		OBJECTIVE_TYPE_TERTIARY,

		OBJECTIVE_STATUS_PENDING			= 0,
		OBJECTIVE_STATUS_ACCOMPLISHED,
		OBJECTIVE_STATUS_FAILED,
		OBJECTIVE_STATUS_HIDDEN,

		RADAR_BLIP_SHAPE_NONE				= 0,
		RADAR_BLIP_SHAPE_HUMAN,
		RADAR_BLIP_SHAPE_VEHICLE,
		RADAR_BLIP_SHAPE_STATIONARY,
		RADAR_BLIP_SHAPE_OBJECTIVE,

		RADAR_BLIP_COLOR_NOD					= 0,
		RADAR_BLIP_COLOR_GDI,
		RADAR_BLIP_COLOR_NEUTRAL,
		RADAR_BLIP_COLOR_MUTANT,
		RADAR_BLIP_COLOR_RENEGADE,
		RADAR_BLIP_COLOR_PRIMARY_OBJECTIVE,
		RADAR_BLIP_COLOR_SECONDARY_OBJECTIVE,
		RADAR_BLIP_COLOR_TERTIARY_OBJECTIVE,

		SCRIPT_PLAYERTYPE_SPECTATOR				= -4,		// -4
		SCRIPT_PLAYERTYPE_MUTANT,							// -3
		SCRIPT_PLAYERTYPE_NEUTRAL,							// -2
		SCRIPT_PLAYERTYPE_RENEGADE,							// -1
		SCRIPT_PLAYERTYPE_NOD,								//  0
		SCRIPT_PLAYERTYPE_GDI,								//  1
};

/*
** Script Commands List
*/

#define SCRIPT_COMMANDS_VERSION 174

typedef struct {
	unsigned int Size;
	unsigned int Version;

	// Debug messages
	void (*	Debug_Message )( char *format, ... );

	// Action Commands
	void ( * Action_Reset )( GameObject * obj, float priority );
	void ( * Action_Goto )( GameObject * obj, const ActionParamsStruct & params );
	void ( * Action_Attack )( GameObject * obj, const ActionParamsStruct & params );
	void ( * Action_Play_Animation )( GameObject * obj, const ActionParamsStruct & params );
	void ( * Action_Enter_Exit )( GameObject * obj, const ActionParamsStruct & params );
	void ( * Action_Face_Location )( GameObject * obj, const ActionParamsStruct & params );
	void ( * Action_Dock )( GameObject * obj, const ActionParamsStruct & params );
	void ( * Action_Follow_Input )( GameObject * obj, const ActionParamsStruct & params );

#ifdef __clang__
	struct { typedef void (*_ft)(GameObject*,int,const ActionParamsStruct&,bool,bool); _ft ptr; void operator=(_ft f){ptr=f;} void operator()(GameObject*o,int ai,const ActionParamsStruct&p,bool mm=true,bool ma=true)const{ptr(o,ai,p,mm,ma);} } Modify_Action;
#else
	void ( * Modify_Action )( GameObject * obj, int action_id, const ActionParamsStruct & params, bool modify_move = true, bool modify_attack = true );
#endif

	// Action information queries
	int	( * Get_Action_ID )( GameObject * obj );
	bool	( * Get_Action_Params )( GameObject * obj, ActionParamsStruct & params );
	bool	( * Is_Performing_Pathfind_Action )( GameObject * obj );

	// Physical control
	void ( * Set_Position )( GameObject * obj, const Vector3 & position );
	Vector3 ( * Get_Position )( GameObject * obj );
	Vector3 ( * Get_Bone_Position )( GameObject * obj, const char * bone_name );
	float ( * Get_Facing )( GameObject * obj );
	void ( * Set_Facing )( GameObject * obj, float degrees );

	// Collision Control
	void	( * Disable_All_Collisions )( GameObject * obj );
	void	( * Disable_Physical_Collisions )( GameObject * obj );
	void	( * Enable_Collisions )( GameObject * obj );
	
	// Object Management
	void ( * Destroy_Object )( GameObject * obj );
	GameObject * ( * Find_Object )( int obj_id );
	GameObject * ( * Create_Object )( const char * type_name, const Vector3 & position );
	GameObject * ( * Create_Object_At_Bone )( GameObject * host_obj, const char * new_obj_type_name, const char * bone_name );
	int	( * Get_ID )( GameObject * obj );
	int	( * Get_Preset_ID )( GameObject * obj );
	const char * ( * Get_Preset_Name )( GameObject * obj );
	void (*Attach_Script)(GameObject* object, const char* scriptName, const char* scriptParams);
	void (*Add_To_Dirty_Cull_List)(GameObject* object);

	// Timers
	void	( * Start_Timer )( GameObject * obj, ScriptClass * script, float duration, int timer_id );

	// Weapons
#ifdef __clang__
	struct { typedef void (*_ft)(GameObject*,bool,const Vector3&,bool); _ft ptr; void operator=(_ft f){ptr=f;} void operator()(GameObject*o,bool tr,const Vector3&t,bool p=true)const{ptr(o,tr,t,p);} } Trigger_Weapon;
#else
	void	( * Trigger_Weapon )( GameObject * obj, bool trigger, const Vector3 & target, bool primary = true );
#endif
	void	( * Select_Weapon )( GameObject * obj, const char * weapon_name );

	// Custom Script
#ifdef __clang__
	struct { typedef void (*_ft)(GameObject*,GameObject*,int,int,float); _ft ptr; void operator=(_ft f){ptr=f;} void operator()(GameObject*fr,GameObject*to,int t=0,int p=0,float d=0)const{ptr(fr,to,t,p,d);} } Send_Custom_Event;
#else
	void	( * Send_Custom_Event )( GameObject * from, GameObject * to, int type = 0, int param = 0, float delay = 0 );
#endif
	void	( * Send_Damaged_Event )( GameObject * object, GameObject * damager );

	// Random Numbers
	float	( * Get_Random )( float min, float max );
	int	( * Get_Random_Int )( int min, int max );  // Get a random number between min and max-1, INCLUSIVE 

	//	Random Selection
	GameObject * ( * Find_Random_Simple_Object )( const char *preset_name );

	// Object Display
	void	( * Set_Model )( GameObject * obj, const char * model_name );
#ifdef __clang__
	struct { typedef void (*_ft)(GameObject*,const char*,bool,const char*,float,float,bool); _ft ptr; void operator=(_ft f){ptr=f;} void operator()(GameObject*o,const char*a,bool l,const char*s=NULL,float sf=0.0F,float ef=-1.0F,bool b=false)const{ptr(o,a,l,s,sf,ef,b);} } Set_Animation;
#else
	void	( * Set_Animation )( GameObject * obj, const char * anim_name, bool looping, const char * sub_obj_name = NULL, float start_frame = 0.0F, float end_frame = -1.0F, bool is_blended = false );
#endif
	void	( * Set_Animation_Frame )( GameObject * obj, const char * anim_name, int frame );

	// Sounds
	// Note: Each sound creation function returns the ID of the new sound (0 on error)
	int	( * Create_Sound )( const char * sound_preset_name, const Vector3 & position, GameObject * creator );
	int	( * Create_2D_Sound )( const char * sound_preset_name );
	int	( * Create_2D_WAV_Sound )( const char * wav_filename );
	int	( * Create_3D_WAV_Sound_At_Bone )( const char * wav_filename, GameObject * obj, const char * bone_name );
	int	( * Create_3D_Sound_At_Bone )( const char * sound_preset_name, GameObject * obj, const char * bone_name );
	int	( * Create_Logical_Sound )( GameObject * creator, int type, const Vector3 & position, float radius );	
	void	( * Start_Sound )( int sound_id );
#ifdef __clang__
	struct { typedef void (*_ft)(int,bool); _ft ptr; void operator=(_ft f){ptr=f;} void operator()(int s,bool d=true)const{ptr(s,d);} } Stop_Sound;
#else
	void	( * Stop_Sound )( int sound_id, bool destroy_sound = true );
#endif
	void	( * Monitor_Sound )( GameObject * game_obj, int sound_id );
	void	( * Set_Background_Music )( const char * wav_filename );
	void	( * Fade_Background_Music )( const char * wav_filename, int fade_out_time, int fade_in_time );
	void	( * Stop_Background_Music )( void );

	// Object Properties
	float	( * Get_Health )( GameObject * obj );
	float	( * Get_Max_Health )( GameObject * obj );
	void	( * Set_Health )( GameObject * obj, float health );
	float	( * Get_Shield_Strength )( GameObject * obj );
	float	( * Get_Max_Shield_Strength )( GameObject * obj );
	void	( * Set_Shield_Strength )( GameObject * obj, float strength );
	void	( * Set_Shield_Type )( GameObject * obj, const char * name );
	int	( * Get_Player_Type )( GameObject * obj );
	void	( * Set_Player_Type )( GameObject * obj, int type );

	// Math
	float	( * Get_Distance )( const Vector3 & p1, const Vector3 & p2 );

	// Set Camera Host
	void	( * Set_Camera_Host )( GameObject * obj );
	void	( * Force_Camera_Look )( const Vector3 & target );

	// Get the Star
	GameObject * ( * Get_The_Star )( void );
	GameObject * ( * Get_A_Star )( const Vector3 & pos );
#ifdef __clang__
	struct { typedef GameObject* (*_ft)(const Vector3&,float,float,bool); _ft ptr; void operator=(_ft f){ptr=f;} GameObject* operator()(const Vector3&p,float mn,float mx,bool h=true)const{return ptr(p,mn,mx,h);} } Find_Closest_Soldier;
#else
	GameObject * ( * Find_Closest_Soldier )( const Vector3 & pos, float min_dist, float max_dist, bool only_human = true );
#endif
	bool		    ( * Is_A_Star )( GameObject * obj );

	// Object Control
	void	( * Control_Enable )( GameObject * obj, bool enable );

	// Hack
	const char * ( * Get_Damage_Bone_Name )( void );
	bool			 ( * Get_Damage_Bone_Direction )( void ); // true means shot in the back

	// Visibility
	bool	( * Is_Object_Visible)( GameObject * looker, GameObject * obj );
#ifdef __clang__
	struct { typedef void (*_ft)(GameObject*,bool); _ft ptr; void operator=(_ft f){ptr=f;} void operator()(GameObject*o,bool e=true)const{ptr(o,e);} } Enable_Enemy_Seen;
#else
	void	( * Enable_Enemy_Seen)( GameObject * obj, bool enable = true );
#endif

	// Display Text
#ifdef __clang__
	struct { typedef void (*_ft)(unsigned char,unsigned char,unsigned char); _ft ptr; void operator=(_ft f){ptr=f;} void operator()(unsigned char r=255,unsigned char g=255,unsigned char b=255)const{ptr(r,g,b);} } Set_Display_Color;
#else
	void	(*	Set_Display_Color )( unsigned char red = 255, unsigned char green = 255, unsigned char blue = 255 );
#endif
	void	(*	Display_Text )( int string_id );
#ifdef __clang__
	struct { typedef void (*_ft)(float,const char*); _ft ptr; void operator=(_ft f){ptr=f;} void operator()(float v,const char*fmt="%f")const{ptr(v,fmt);} } Display_Float;
	struct { typedef void (*_ft)(int,const char*); _ft ptr; void operator=(_ft f){ptr=f;} void operator()(int v,const char*fmt="%d")const{ptr(v,fmt);} } Display_Int;
#else
	void	(*	Display_Float )( float value, const char * format = "%f" );
	void	(*	Display_Int )( int value, const char * format = "%d" );
#endif

	// SaveLoad
	void	(*	Save_Data )( ScriptSaver & saver, int id, int size, void * data );
	void	(*	Save_Pointer )( ScriptSaver & saver, int id, void * pointer );
	bool	(*	Load_Begin )( ScriptLoader & loader, int * id );
	void	(*	Load_Data )( ScriptLoader & loader, int size, void * data );
	void	(*	Load_Pointer )( ScriptLoader & loader, void ** pointer );
	void	(*	Load_End )( ScriptLoader & loader );

	void (*Begin_Chunk)(ScriptSaver& saver, unsigned int chunkID);
	void (*End_Chunk)(ScriptSaver& saver);
	bool (*Open_Chunk)(ScriptLoader& loader, unsigned int* chunkID);
	void (*Close_Chunk)(ScriptLoader& loader);

	// Radar Effects
	void	(* Clear_Radar_Markers )( void );
	void	(* Clear_Radar_Marker )( int id );
	void	(* Add_Radar_Marker )( int id, const Vector3& position, int shape_type, int color_type );
	void	(* Set_Obj_Radar_Blip_Shape )( GameObject * obj, int shape_type );	// Set to -1 to reset default
	void	(* Set_Obj_Radar_Blip_Color )( GameObject * obj, int color_type );	// Set to -1 to reset default
	void	(* Enable_Radar )( bool enable );

	//
	//	Map support
	//
	void	(* Clear_Map_Cell )( int cell_x, int cell_y );	
	void	(* Clear_Map_Cell_By_Pos )( const Vector3 &world_space_pos );
	void	(* Clear_Map_Cell_By_Pixel_Pos )( int pixel_pos_x, int pixel_pos_y );
	void	(* Clear_Map_Region_By_Pos )( const Vector3 &world_space_pos, int pixel_radius );
	void	(* Reveal_Map )( void );
	void	(* Shroud_Map )( void );
	void	(* Show_Player_Map_Marker )( bool onoff );

	//
	//	Height DB access
	//
	float	(* Get_Safe_Flight_Height )( float x_pos, float y_pos );

	// Explosions
#ifdef __clang__
	struct { typedef void (*_ft)(const char*,const Vector3&,GameObject*); _ft ptr; void operator=(_ft f){ptr=f;} void operator()(const char*n,const Vector3&p,GameObject*c=NULL)const{ptr(n,p,c);} } Create_Explosion;
	struct { typedef void (*_ft)(const char*,GameObject*,const char*,GameObject*); _ft ptr; void operator=(_ft f){ptr=f;} void operator()(const char*n,GameObject*o,const char*b,GameObject*c=NULL)const{ptr(n,o,b,c);} } Create_Explosion_At_Bone;
#else
	void	(* Create_Explosion )( const char * explosion_def_name, const Vector3 & pos, GameObject * creator = NULL );
	void	(* Create_Explosion_At_Bone )( const char * explosion_def_name, GameObject * object, const char * bone_name, GameObject * creator = NULL );
#endif

	// HUD
	void	(* Enable_HUD )( bool enable );
	void	(* Mission_Complete )( bool success );

#ifdef __clang__
	struct { typedef void (*_ft)(GameObject*,const char*,bool); _ft ptr; void operator=(_ft f){ptr=f;} void operator()(GameObject*o,const char*n,bool d=false)const{ptr(o,n,d);} } Give_PowerUp;
#else
	void	(* Give_PowerUp )( GameObject * obj, const char * preset_name, bool display_on_hud = false );
#endif

	// Administration
	void (*Innate_Disable)(GameObject* object);
	void (*Innate_Enable)(GameObject* object);

	// Innate Soldier AI Enable/Disable (returns old value)
	bool	(* Innate_Soldier_Enable_Enemy_Seen )( GameObject * obj, bool state );
	bool	(* Innate_Soldier_Enable_Gunshot_Heard )( GameObject * obj, bool state );
	bool	(* Innate_Soldier_Enable_Footsteps_Heard )( GameObject * obj, bool state );
	bool	(* Innate_Soldier_Enable_Bullet_Heard )( GameObject * obj, bool state );
	bool	(* Innate_Soldier_Enable_Actions )( GameObject * obj, bool state );
#ifdef __clang__
	struct { typedef void (*_ft)(GameObject*,const Vector3&,float); _ft ptr; void operator=(_ft f){ptr=f;} void operator()(GameObject*o,const Vector3&p,float r=999999)const{ptr(o,p,r);} } Set_Innate_Soldier_Home_Location;
#else
	void	(* Set_Innate_Soldier_Home_Location )( GameObject * obj, const Vector3& home_pos, float home_radius = 999999 );
#endif
	void	(* Set_Innate_Aggressiveness )( GameObject * obj, float aggressiveness );
	void	(* Set_Innate_Take_Cover_Probability )( GameObject * obj, float probability );
	void	(* Set_Innate_Is_Stationary )( GameObject * obj, bool stationary );

	void	(* Innate_Force_State_Bullet_Heard )( GameObject * obj, const Vector3 & pos );
	void	(* Innate_Force_State_Footsteps_Heard )( GameObject * obj, const Vector3 & pos );
	void	(* Innate_Force_State_Gunshots_Heard )( GameObject * obj, const Vector3 & pos );
	void	(* Innate_Force_State_Enemy_Seen )( GameObject * obj, GameObject * enemy );

	// Control of StaticAnimPhys
#ifdef __clang__
	struct { typedef void (*_ft)(int,float,const char*); _ft ptr; void operator=(_ft f){ptr=f;} void operator()(int i,float fr,const char*a=NULL)const{ptr(i,fr,a);} } Static_Anim_Phys_Goto_Frame;
	struct { typedef void (*_ft)(int,const char*); _ft ptr; void operator=(_ft f){ptr=f;} void operator()(int i,const char*a=NULL)const{ptr(i,a);} } Static_Anim_Phys_Goto_Last_Frame;
#else
	void	(* Static_Anim_Phys_Goto_Frame )( int obj_id, float frame, const char * anim_name = NULL );
	void	(* Static_Anim_Phys_Goto_Last_Frame )( int obj_id, const char * anim_name = NULL );
#endif

	// Timing
	unsigned int (* Get_Sync_Time)( void );

	// Objectives
#ifdef __clang__
	struct { typedef void (*_ft)(int,int,int,int,char*,int); _ft ptr; void operator=(_ft f){ptr=f;} void operator()(int id,int t,int s,int sid,char*dsf=NULL,int ldid=0)const{ptr(id,t,s,sid,dsf,ldid);} } Add_Objective;
#else
	void	(* Add_Objective)( int id, int type, int status, int short_description_id, char * description_sound_filename = NULL, int long_description_id = 0 );
#endif
	void	(* Remove_Objective)( int id );
	void	(* Set_Objective_Status)( int id, int status );
	void	(* Change_Objective_Type)( int id, int type );
	void	(* Set_Objective_Radar_Blip)( int id, const Vector3 & position );
	void	(* Set_Objective_Radar_Blip_Object)( int id, ScriptableGameObj * unit );
	void	(* Set_Objective_HUD_Info)( int id, float priority, const char * texture_name, int message_id );
	void	(* Set_Objective_HUD_Info_Position)( int id, float priority, const char * texture_name, int message_id, const Vector3 & position );

	// Camaera Shakes
#ifdef __clang__
	struct { typedef void (*_ft)(const Vector3&,float,float,float); _ft ptr; void operator=(_ft f){ptr=f;} void operator()(const Vector3&p,float r=25,float i=0.25f,float d=1.5f)const{ptr(p,r,i,d);} } Shake_Camera;
#else
	void	(* Shake_Camera)( const Vector3 & pos, float radius = 25, float intensity = 0.25f, float duration = 1.5f );
#endif

	// Spawners
	void	(* Enable_Spawner)( int id, bool enable );
	GameObject * (* Trigger_Spawner)( int id );

	// Vehicles
	void	(* Enable_Engine)( GameObject* object, bool onoff );

	// Difficulty Level
	int	(* Get_Difficulty_Level)( void );

	// Keys
#ifdef __clang__
	struct { typedef void (*_ft)(GameObject*,int,bool); _ft ptr; void operator=(_ft f){ptr=f;} void operator()(GameObject*o,int k,bool g=true)const{ptr(o,k,g);} } Grant_Key;
#else
	void	(* Grant_Key)( GameObject* object, int key, bool grant = true );
#endif
	bool	(* Has_Key)( GameObject* object, int key );

	// Hibernation
	void	(* Enable_Hibernation)( GameObject * object, bool enable );

	void	(* Attach_To_Object_Bone)( GameObject * object, GameObject * host_object, const char * bone_name );

	// Conversation
#ifdef __clang__
	struct { typedef int (*_ft)(const char*,int,float,bool); _ft ptr; void operator=(_ft f){ptr=f;} int operator()(const char*n,int p=0,float d=0,bool i=true)const{return ptr(n,p,d,i);} } Create_Conversation;
	struct { typedef void (*_ft)(GameObject*,int,bool,bool,bool); _ft ptr; void operator=(_ft f){ptr=f;} void operator()(GameObject*o,int id,bool am=true,bool ah=true,bool af=true)const{ptr(o,id,am,ah,af);} } Join_Conversation;
#else
	int	(* Create_Conversation)( const char *conversation_name, int priority = 0, float max_dist = 0, bool is_interruptable = true );
	void	(* Join_Conversation)( GameObject * object, int active_conversation_id, bool allow_move = true, bool allow_head_turn = true, bool allow_face = true );
#endif
	void	(* Join_Conversation_Facing)( GameObject * object, int active_conversation_id, int obj_id_to_face );
#ifdef __clang__
	struct { typedef void (*_ft)(int,int); _ft ptr; void operator=(_ft f){ptr=f;} void operator()(int cid,int aid=0)const{ptr(cid,aid);} } Start_Conversation;
#else
	void	(* Start_Conversation)( int active_conversation_id, int action_id = 0 );
#endif
	void	(* Monitor_Conversation)( GameObject * object, int active_conversation_id );
	void	(* Start_Random_Conversation)( GameObject * object );
	void	(* Stop_Conversation)( int active_conversation_id );
	void	(* Stop_All_Conversations)( void );

	// Locked facing support
	void	(* Lock_Soldier_Facing)( GameObject * object, GameObject * object_to_face, bool turn_body );
	void	(* Unlock_Soldier_Facing)( GameObject * object );

	// Apply Damage
#ifdef __clang__
	struct { typedef void (*_ft)(GameObject*,float,const char*,GameObject*); _ft ptr; void operator=(_ft f){ptr=f;} void operator()(GameObject*o,float a,const char*w,GameObject*d=NULL)const{ptr(o,a,w,d);} } Apply_Damage;
#else
	void	(* Apply_Damage)( GameObject * object, float amount, const char * warhead_name, GameObject * damager = NULL );
#endif

	// Soldier
	void	(* Set_Loiters_Allowed)( GameObject * object, bool allowed );

	void	(* Set_Is_Visible)( GameObject * object, bool visible );
	void	(* Set_Is_Rendered)( GameObject * object, bool rendered );

	// Points
	float	(* Get_Points)( GameObject * object );
	void	(* Give_Points)( GameObject * object, float points, bool entire_team );

	// Money (points and money were separated 09/06/01)
	float	(* Get_Money)( GameObject * object );
	void	(* Give_Money)( GameObject * object, float money, bool entire_team );

	// Buildings
	bool	(* Get_Building_Power)( GameObject * object );
	void	(* Set_Building_Power)( GameObject * object, bool onoff );
	void	(* Play_Building_Announcement)( GameObject * object, int text_id );
	GameObject * (* Find_Nearest_Building_To_Pos )( const Vector3 & position, const char * mesh_prefix );
	GameObject * (* Find_Nearest_Building )( GameObject * object, const char * mesh_prefix );

	// Zones
	int	(* Team_Members_In_Zone)( GameObject * object, int player_type );

	// Background
	void  (*Set_Clouds) (float cloudcover, float cloudgloominess, float ramptime);
	void  (*Set_Lightning) (float intensity, float startdistance, float enddistance, float heading, float distribution, float ramptime);
	void  (*Set_War_Blitz) (float intensity, float startdistance, float enddistance, float heading, float distribution, float ramptime);

	// Weather
	void	(*Set_Wind)			(float heading, float speed, float variability, float ramptime);
	void	(*Set_Rain)			(float density, float ramptime, bool prime);
	void	(*Set_Snow)			(float density, float ramptime, bool prime);
	void	(*Set_Ash)			(float density, float ramptime, bool prime);
	void  (*Set_Fog_Enable) (bool enabled);
	void  (*Set_Fog_Range)	(float startdistance, float enddistance, float ramptime);

	// Stealth control
	void	(*Enable_Stealth)	(GameObject * object, bool onoff);

	// Sniper control
	void	(*Cinematic_Sniper_Control)	(bool enabled, float zoom);

	// File Access
	int	(*Text_File_Open)			( const char * filename );
	bool	(*Text_File_Get_String)	( int handle, char * buffer, int size );
	void	(*Text_File_Close)		( int handle );

	// Vehicle Transitions
	void	(*Enable_Vehicle_Transitions)	( GameObject * object, bool enable );

	// Player terminal support
	void	(*Display_GDI_Player_Terminal)		();
	void	(*Display_NOD_Player_Terminal)		();
	void	(*Display_Mutant_Player_Terminal)	();

	// Encyclopedia support
	bool	(*Reveal_Encyclopedia_Character)	( int object_id );
	bool	(*Reveal_Encyclopedia_Weapon)		( int object_id );
	bool	(*Reveal_Encyclopedia_Vehicle)	( int object_id );
	bool	(*Reveal_Encyclopedia_Building)	( int object_id );
	void	(*Display_Encyclopedia_Event_UI) ( void );

	void	(* Scale_AI_Awareness)( float sight_scale, float hearing_scale );

	// Cinematic Freeze
	void	(* Enable_Cinematic_Freeze)( GameObject * object, bool enable );

	void	(* Expire_Powerup )( GameObject * object );

	// Hud stuff
	void	(* Set_HUD_Help_Text )( int string_id, const Vector3 &color );
	void	(* Enable_HUD_Pokable_Indicator )( GameObject * object, bool enable );

	void	(* Enable_Innate_Conversations )( GameObject * object, bool enable );

	void	(* Display_Health_Bar )( GameObject * object, bool display );

	// Shadow control.  In certain cases we need to manually disable shadow casting
	// on an object.  Cinematics with too many characters are an example of this.
	void	(* Enable_Shadow) ( GameObject * object, bool enable );

	void	(* Clear_Weapons) ( GameObject * object );

	void	(* Set_Num_Tertiary_Objectives) ( int count );

	// Letterbox and screen fading controls
	void	(* Enable_Letterbox) ( bool onoff, float seconds );
	void	(* Set_Screen_Fade_Color) ( float r, float g, float b, float seconds );
	void	(* Set_Screen_Fade_Opacity) ( float opacity, float seconds );

} ScriptCommands;


/*
** Build a class to wrap the struct
*/
class ScriptCommandsClass {
public:
	ScriptCommands	*Commands;
};

/*
** Get Script Commands 
** This should only be called in the host application
** and not from the DLL
*/
ScriptCommands	*Get_Script_Commands( void );

#endif	// SCRIPTCOMMANDS_H
