#ifndef	OBJECTIVES_H
#define	OBJECTIVES_H

#ifndef	ALWAYS_H
	#include "always.h"
#endif

#ifndef	VECTOR3_H
	#include "vector3.h"
#endif

#include "wwstring.h"
#include "widestring.h"
#include "simplevec.h"
#include "objectivesviewer.h"
#include "gameobjref.h"

class	ChunkSaveClass;
class	ChunkLoadClass;
class	ScriptableGameObj;
class PhysicalGameObj;

/*
**
*/
struct	Objective {

	int					ID;
	int					Type;
	int					Status;
	int					LongDescriptionID;
	int					ShortDescriptionID;
	StringClass			DescriptionSoundFilename;
	StringClass			HUDPogTextureName;
	int					HUDMessageStringID;
	float				HUDPriority;
	bool				DrawBlip;
	Vector3				Position;
	float				BlipIntensity;
	GameObjReference	Object;
	float				Age;

	Objective( void );
	~Objective( void );

	bool	Save( ChunkSaveClass &csave );
	bool	Load( ChunkLoadClass &cload );

	const Vector3 &	Type_To_Color( void );
	const Vector3 &	Type_To_Base_Color( void );
	const WCHAR *		Type_To_Name( void );
	const WCHAR *		Status_To_Name( void );
	const Vector3 &	Status_To_Color( void );

	int	Radar_Blip_Color_Type( void );

	void	Set_Object( PhysicalGameObj * object );
	void	Update_Object_Blip( void );

	Vector3				Get_Position( void );
};

/*
**
*/
class ObjectiveManager 
{
public:
	enum {
		TYPE_PRIMARY 			= 1,
		TYPE_SECONDARY,
		TYPE_TERTIARY,
		STATUS_IS_PENDING		= 0,
		STATUS_ACCOMPLISHED,
		STATUS_FAILED,
		STATUS_HIDDEN,
	};

	static	void	Init( void );
	static	void	Shutdown( void );
	static	void	Reset( void );
	static	void	Update( float dt );

	static	bool	Save( ChunkSaveClass &csave );
	static	bool	Load( ChunkLoadClass &cload );

	static	void	Add_Objective( int id, int type, int status, int short_description_id, int long_description_id, char * description_sound_filename );
	static	void	Remove_Objective( int id );
	static	void	Set_Objective_Status( int id, int status );
	static	void	Change_Objective_Type( int id, int type );
	static	void	Set_Objective_Radar_Blip( int id, Vector3 position );
	static	void	Set_Objective_Radar_Blip( int id, PhysicalGameObj * object );
	static	void	Set_Objective_HUD_Info( int id, float priority, const char * texture_name, int message_id );
	static	void	Set_Objective_HUD_Info( int id, float priority, const char * texture_name, int message_id, const Vector3 & position );

	static	int	Get_Objective_Count( void )					{ return ObjectiveList.Count(); }
	static	Objective * Get_Objective( int index )				{ return ObjectiveList[ index ]; }

	static	bool	Is_Objective_Debug_Mode_Enabled (void)		{ return DebugMode; }
	static	void	Enable_Objective_Debug_Mode (bool onoff)	{ DebugMode = onoff; Viewer.Update (); }

	static	bool	Is_Viewer_Displayed( void )	{ return Viewer.Is_Displayed (); }
	static	void	Display_Viewer( bool onoff )	{ Viewer.Display (onoff); }
	static	void	Page_Down_Viewer( void )		{ Viewer.Page_Down (); }
	static	void	Render_Viewer( void )			{ Viewer.Render (); }
	static	void	Reload_Viewer( void )			{ Viewer.Initialize (); }

	static	int		Get_Num_Objectives( int type );
	static	int		Get_Num_Completed_Objectives( int type );

	static	void		Set_Num_Specified_Tertiary_Objectives( int count )	{ NumSpecifiedTertiaryObjectives = count; }

	// HUD interface
	static	int				Get_Num_HUD_Objectives( void );
	static	const char *	Get_HUD_Objectives_Pog_Texture_Name( int index );
	static	const WCHAR *	Get_HUD_Objectives_Message( int index );
	static	Vector3			Get_HUD_Objectives_Location( int index );
	static	float			Get_HUD_Objectives_Age( int index );
	static	bool			Are_HUD_Objectives_Changed( void )		{ return HUDUpdate; }
	static	void			Clear_HUD_Objectives_Changed( void )	{ HUDUpdate = false; }

protected:

	static Objective * Add_Loadable_Objective( void );
	static Objective * Find_Objective( int id );

	static SimpleDynVecClass<Objective*>	ObjectiveList;

private:
	static ObjectivesViewerClass				Viewer;
	static bool										DebugMode;

	static bool										HUDUpdate;

	static int										NumSpecifiedTertiaryObjectives;

	static	int __cdecl		ObjectiveSortCallback( const void *elem1, const void *elem2 );
	static	void				Sort_Objectives( void );

	friend class ObjectivesViewerClass;
};

#endif	// OBJECTIVES_H
