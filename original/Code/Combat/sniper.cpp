#include "sniper.h"
#include "render2d.h"
#include "texture.h"
#include "assets.h"
#include "combat.h"
#include "ccamera.h"
#include "globalsettings.h"
#include "timemgr.h"

#include "input.h"

/*
**
*/
Vector2	SNIPER_UV_SCALE( 1.0f/128.0f, 1.0f/128.0f );

Vector2 BLACK_UV_UL( 1, 1 );
Vector2 BLACK_UV_LR( 5, 7 );
Vector2 SNIPER_VIEW_UV_UL( 1, 2 );
Vector2 SNIPER_VIEW_UV_LR( 115, 115 );
Vector2 SNIPER_VIEW_UL( 193, 153 );
Vector2 SNIPER_VIEW_LR( 839, 853 );
Vector2 BOX_SIZE( 235, 127 );
Vector2 TOP_LINE_1( 149, 209 );
Vector2 TOP_LINE_2( 151, 426 );
Vector2 TOP_LINE_3( 151, 403 );
Vector2 BOTTOM_LINE_1( 543, 768 );
Vector2 BOTTOM_LINE_2( 34, 597 );
Vector2 BOTTOM_LINE_3( 519, 10 );
Vector2 CENTER_LINE_1( 144, 42 );
Vector2 CENTER_LINE_2( 166, 117 );
Vector2 LEFT_LINE_UL( 203, 723 );
Vector2 LEFT_LINE_LR( 222, 10 );
Vector2 LEFT_LINE_2( 247, 435 );
Vector2 ZOOM_UV_UL( 117, 41 );
Vector2 ZOOM_UV_LR( 128, 62 );
Vector2 ZOOM_OFFSET_TOP( 177, 620 );
Vector2 ZOOM_OFFSET_BOTTOM( 177, 316 );

/*
**
*/
#define	SNIPER_LEFT_BLACK_COVER		RectClass( 0, 0, settings->SniperView.Left, 1 )
#define	SNIPER_RIGHT_BLACK_COVER	RectClass( settings->SniperView.Right, 0, 1, 1 )
#define	SNIPER_TOP_BLACK_COVER		RectClass( settings->SniperView.Left, 0, settings->SniperView.Right, settings->SniperView.Top )
#define	SNIPER_BOTTOM_BLACK_COVER	RectClass( settings->SniperView.Left, settings->SniperView.Bottom, settings->SniperView.Right, 1 )

/*
**
*/
Render2DClass		*	_Sniper2DBaseRenderer;
Render2DClass		*	_Sniper2DRenderer;

#define	HUD_SNIPER_TEXTURE			"hud_sniper.tga"

static	void	Info_Editor_Init( void ); 
static	void	Info_Editor_Update( void ); 
static	void	Info_Editor_Shutdown( void ); 

/*
**
*/
void 	SniperHUDClass::Init( void )
{
	_Sniper2DBaseRenderer = new Render2DClass();
	_Sniper2DBaseRenderer->Set_Texture( HUD_SNIPER_TEXTURE );
	_Sniper2DBaseRenderer->Set_Coordinate_Range( Render2DClass::Get_Screen_Resolution() );

	Build_Base();

	_Sniper2DRenderer = new Render2DClass();
	_Sniper2DRenderer->Set_Texture( HUD_SNIPER_TEXTURE );
	_Sniper2DRenderer->Set_Coordinate_Range( Render2DClass::Get_Screen_Resolution() );

//	Info_Editor_Init(); 
}

/*
**
*/
void 	SniperHUDClass::Shutdown( void )
{
	if ( _Sniper2DBaseRenderer ) {
		delete _Sniper2DBaseRenderer;
		_Sniper2DBaseRenderer = NULL;
	}

	if ( _Sniper2DRenderer ) {
		delete _Sniper2DRenderer;
		_Sniper2DRenderer = NULL;
	}

//	Info_Editor_Shutdown(); 
}

void  SniperHUDClass::Build_Base( void ) 
{
	if ( _Sniper2DBaseRenderer == NULL ) {
		return;
	}

	_Sniper2DBaseRenderer->Reset();

	RectClass uv;
	RectClass draw;
	
	uv.Set( SNIPER_VIEW_UV_UL, SNIPER_VIEW_UV_LR );
	uv.Scale( SNIPER_UV_SCALE );

	Vector2	screen_size = Render2DClass::Get_Screen_Resolution().Lower_Right();
	Vector2	screen_center = screen_size * 0.5f;
	Vector2	screen_scale = screen_size * 0.001f;

	// Set tint color
	int tint = 0xFF0000FF;	// Blue
	tint = 0xFF00FF00;	// Green

	// Draw the center view
	draw.Set( SNIPER_VIEW_UL, SNIPER_VIEW_LR );
	draw.Scale( screen_scale );
	_Sniper2DBaseRenderer->Add_Quad( draw, uv, tint );

	// Draw the black sides
	RectClass blackuv( BLACK_UV_UL, BLACK_UV_LR );
	blackuv.Scale( SNIPER_UV_SCALE );
	RectClass edge;
	edge.Set( 0, 0, draw.Left, screen_size.Y );						// Left
	_Sniper2DBaseRenderer->Add_Quad( edge, blackuv );
	edge.Set( draw.Right, 0, screen_size.X, screen_size.Y );		// Right
	_Sniper2DBaseRenderer->Add_Quad( edge, blackuv );
	edge.Set( draw.Left, 0, draw.Right, draw.Top );					// Top
	_Sniper2DBaseRenderer->Add_Quad( edge, blackuv );
	edge.Set( draw.Left, draw.Bottom, draw.Right, screen_size.Y );	// Bottom
	_Sniper2DBaseRenderer->Add_Quad( edge, blackuv );

	// Draw the center box
	Vector2	box_corner = BOX_SIZE;
	box_corner.Scale( screen_scale );
	RectClass	box( -box_corner, box_corner );
	box += screen_center;		// Center it
	_Sniper2DBaseRenderer->Add_Outline( box, 1, blackuv );

	// Draw the top line
	Vector2	top_line_1 = TOP_LINE_1;
	top_line_1.Scale( screen_scale );
	_Sniper2DBaseRenderer->Add_Line( Vector2(screen_center.X,0), Vector2(screen_center.X,top_line_1.Y), 1, blackuv );

	// Draw the top line 2
	Vector2	top_line_2 = TOP_LINE_2;
	top_line_2.Scale( screen_scale );
	_Sniper2DBaseRenderer->Add_Line( Vector2(screen_center.X,box.Top), Vector2(screen_center.X,top_line_2.Y), 1, blackuv );

	// Draw the top line 3
	Vector2	top_line_3 = TOP_LINE_3;
	top_line_3.Scale( screen_scale );
	_Sniper2DBaseRenderer->Add_Line( Vector2(screen_center.X,box.Top), Vector2(screen_center.X,top_line_3.Y), 3, blackuv );

	// Draw the bottom line
	Vector2 bottom_line_1 = BOTTOM_LINE_1;
	bottom_line_1.Scale( screen_scale.Y, screen_scale.Y ); 
	_Sniper2DBaseRenderer->Add_Line( Vector2(screen_center.X,bottom_line_1.X), Vector2(screen_center.X,bottom_line_1.Y), 1, blackuv );

	// Draw the bottom line 2
	Vector2 bottom_line_2 = BOTTOM_LINE_2;
	bottom_line_2.Scale( screen_scale );
	_Sniper2DBaseRenderer->Add_Line( Vector2(screen_center.X - bottom_line_2.X,bottom_line_2.Y), Vector2(screen_center.X + bottom_line_2.X,bottom_line_2.Y), 1, blackuv );

	// Draw the bottom line 3
	float y;
	float left = screen_center.X + 3;
	float right = BOTTOM_LINE_3.X * screen_scale.X;
	float step = BOTTOM_LINE_3.Y * screen_scale.Y;
	for ( y = bottom_line_2.Y; y < bottom_line_1.Y; y += step ) {
		_Sniper2DBaseRenderer->Add_Line( Vector2(left,y), Vector2(right,y), 1, blackuv );
	}

	// Draw the left lines
	left = LEFT_LINE_UL.X * screen_scale.X;
	right = LEFT_LINE_LR.X * screen_scale.X;
	for ( y = screen_center.Y; y < LEFT_LINE_UL.Y * screen_scale.Y; y += LEFT_LINE_LR.Y * screen_scale.Y ) {
		float flip = screen_size.Y - y;
		_Sniper2DBaseRenderer->Add_Line(	Vector2( left, y ), Vector2( right,y ), 1, blackuv );
		_Sniper2DBaseRenderer->Add_Line(	Vector2( left, flip ), Vector2( right, flip ), 1, blackuv );
	}

	// Draw the right line
	_Sniper2DBaseRenderer->Add_Line( Vector2(box.Right,screen_center.Y), Vector2(screen_size.X,screen_center.Y), 1, blackuv );

	// Draw the center line
	Vector2 center_line( -screen_scale.X, screen_scale.X );
	center_line *= CENTER_LINE_1.X;
	center_line += Vector2( screen_center.X, screen_center.X);
	_Sniper2DBaseRenderer->Add_Line( Vector2(center_line.X,screen_center.Y), Vector2(center_line.Y,screen_center.Y), 1, blackuv );

	// Draw the center line 2
	center_line = Vector2( screen_scale.X, screen_scale.X );
	center_line.Scale( CENTER_LINE_2 );
	center_line += Vector2( screen_center.X, screen_center.X);
	_Sniper2DBaseRenderer->Add_Line( Vector2(center_line.X,screen_center.Y), Vector2(center_line.Y,screen_center.Y), 3, blackuv );
	center_line -= Vector2( screen_center.X, screen_center.X);
	center_line *= -1;
	center_line += Vector2( screen_center.X, screen_center.X);
	_Sniper2DBaseRenderer->Add_Line( Vector2(center_line.X,screen_center.Y), Vector2(center_line.Y,screen_center.Y), 3, blackuv );

	// Draw the left line
	_Sniper2DBaseRenderer->Add_Line( Vector2(0, screen_center.Y), Vector2(LEFT_LINE_2.X*screen_scale.X,screen_center.Y), 1, blackuv );
}

/*
**
*/
void 	SniperHUDClass::Update( void )
{
//	Info_Editor_Update();

	if ( _Sniper2DRenderer == NULL ) {
		return;
	}

	_Sniper2DRenderer->Reset();

	Vector2	screen_size = Render2DClass::Get_Screen_Resolution().Lower_Right();
	Vector2	screen_scale = screen_size * 0.001f;

	// Set tint color
	int tint = 0xFF0000FF;	// Blue
	tint = 0xFF00FF00;	// Green
	
	// Draw Zoom Indicator
	RectClass uv;
	uv.Set( ZOOM_UV_UL, ZOOM_UV_LR );
	RectClass draw = uv;
	uv.Scale( SNIPER_UV_SCALE );

	float zoom_ratio = COMBAT_CAMERA->Get_Sniper_Zoom();
	Vector2 pos = ((ZOOM_OFFSET_BOTTOM - ZOOM_OFFSET_TOP) * zoom_ratio) + ZOOM_OFFSET_TOP;
	pos.Scale( screen_scale );
	draw += pos - draw.Center();
	_Sniper2DRenderer->Add_Quad( draw, uv, tint );


}

/*
**
*/
void 	SniperHUDClass::Render( void )
{
	if ( _Sniper2DBaseRenderer ) {
		_Sniper2DBaseRenderer->Render();
	}

	if ( _Sniper2DRenderer ) {
		_Sniper2DRenderer->Render();
	}
}

