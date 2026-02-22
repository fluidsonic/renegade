#pragma once

#include "global.h"


#include "vector3.h"

#include "vector.h"

#include "rect.h"

/*
**
*/
class	ChunkSaveClass;
class	ChunkLoadClass;
class	Matrix3D;
class	Vector2;
class	Render2DClass;
class	PhysicalGameObj;
class	Render2DSentenceClass;

/*
** Radar mode in multiplayer
*/
enum RadarModeEnum {
	RADAR_NOBODY,
	RADAR_TEAMMATES,
	RADAR_ALL,
};

/*
**
*/
class			RadarMarkerClass {
public:
	RadarMarkerClass( void );
	bool		operator == ( const RadarMarkerClass & other) const	{ return false; }
	bool		operator != ( const RadarMarkerClass & other) const	{ return true; }

	bool								Save( ChunkSaveClass &csave );
	bool								Load( ChunkLoadClass &cload );

	int								ID;
	int								Type;
	int								Color;
	Vector3							Position;
	float								Intensity;

};

/*
** System to display the Radar
*/
class	RadarManager {

public:
	static	void	Init( void );
	static	void	Shutdown( void );

	static	bool	Save( ChunkSaveClass &csave );
	static	bool	Load( ChunkLoadClass &cload );

	static	void	Update( const Matrix3D & player_tm, const Vector2 & offset_2d );
	static	void	Set_Bracket_Object( const PhysicalGameObj * obj )		{ BracketObj = obj; }

	static	void	Render( void );
	static	void	Set_Hidden( bool onoff );
	static	bool	Is_Hidden( void )				{ return IsHidden; }

	// Blip Types
	enum	{
		BLIP_SHAPE_TYPE_NONE					= 0,
		BLIP_SHAPE_TYPE_HUMAN,
		BLIP_SHAPE_TYPE_VEHICLE,
		BLIP_SHAPE_TYPE_STATIONARY,
		BLIP_SHAPE_TYPE_OBJECTIVE,
		NUM_BLIP_SHAPE_TYPES,
		BLIP_BRACKET							= NUM_BLIP_SHAPE_TYPES,
		BLIP_SWEEP,
		NUM_BLIP_TYPES
	}; 
	static	int	Get_Num_Blip_Shape_Types( void ) { return NUM_BLIP_SHAPE_TYPES; }
	static const char * Get_Blip_Shape_Type_Name( int index );

	// Colors
	enum	{
		BLIP_COLOR_TYPE_NOD					= 0,
		BLIP_COLOR_TYPE_GDI,
		BLIP_COLOR_TYPE_NEUTRAL,
		BLIP_COLOR_TYPE_MUTANT,
		BLIP_COLOR_TYPE_RENEGADE,
		BLIP_COLOR_TYPE_PRIMARY_OBJECTIVE,
		BLIP_COLOR_TYPE_SECONDARY_OBJECTIVE,
		BLIP_COLOR_TYPE_TERTIARY_OBJECTIVE,
		NUM_BLIP_COLOR_TYPES
	}; 

	// Special Markers
	static	void	Clear_Marker( int id );
	static	void	Clear_Markers( void )									{ Markers.Delete_All(); }
	static	void	Add_Marker( const RadarMarkerClass & marker )	{ Markers.Add(marker); }
	static	void	Change_Marker_Color( int id, int color );

	// Multiplayer support
   static	void	Set_Radar_Mode(RadarModeEnum mode)					{ RadarMode = mode; }
   static	RadarModeEnum	Get_Radar_Mode(void)							{ return RadarMode; }

private:
	static	DynamicVectorClass<RadarMarkerClass>	Markers;
	static	Render2DClass						*	Renderer;
	static	Render2DSentenceClass *				CompassRenderers[8];
	static	int										CurrentCompassRendererIndex;
	static	unsigned long							BlipColors[ NUM_BLIP_COLOR_TYPES ];
	static	const PhysicalGameObj				*	BracketObj;
	static	RectClass								BlipUV[ NUM_BLIP_TYPES ];
	static	bool									IsHidden;
	static	float									HiddenTimer;
	static	RadarModeEnum						RadarMode;

	// Possibily add a blip, return the new intensity
	static	float		Add_Blip( const Vector3 & pos, int shape_type, int color_type, float intensity, bool bracket, bool altitude_fade = false );
};
