#include "global.h"
#include	"misc.h"
#include	"dsurface.h"
#include	"data.h"
#include	"_timer.h"
#include	<assert.h>
#include <stdio.h>

LPDIRECTDRAW DirectDrawObject = NULL;	// Pointer to the direct draw object
LPDIRECTDRAW2 DirectDraw2Interface = NULL;  	// Pointer to direct draw 2 interface

static PALETTEENTRY PaletteEntries[256];		// 256 windows palette entries
static LPDIRECTDRAWPALETTE	PalettePtr;					// Pointer to direct draw palette object
static bool FirstPaletteSet = false;	// Is this the first time 'Set_Palette' has been called?
LPDIRECTDRAWSURFACE	PaletteSurface = NULL;
bool SurfacesRestored = false;
static bool CanVblankSync = true;

unsigned char CurrentPalette[768];
bool Debug_Windowed;

int (*DirectDrawErrorHandler)(HRESULT error) = NULL;

void Set_Palette(PaletteClass const & pal, int time, void (*callback)())
{
	CDTimerClass<SystemTimerClass> timer = time;
	PaletteClass original;
	memcpy(&original, CurrentPalette, sizeof(CurrentPalette));
	PaletteClass newpal = pal;

	while (timer) {

		/*
		**	Build an intermediate palette that is as close to the destination palette
		**	as the current time is proportional to the ending time.
		*/
		PaletteClass palette = original;
		int adjust = ((time - timer) * 256) / time;
		adjust = MIN(adjust, 255);
		palette.Adjust(adjust, newpal);

		/*
		**	Remember the current time so that multiple palette sets within the same game
		**	time tick won't occur. This is probably unnecessary since the palette setting
		**	code, at the time of this writing, delays at least one game tick in the process
		**	of setting the palette.
		*/
		int holdtime = timer;

		/*
		**	Set the palette to this intermediate palette and then loop back
		**	to calculate and set a new intermediate palette.
		*/
		Set_Palette((void*)&palette[0]);

		/*
		**	If the callback routine was specified, then call it once per palette
		**	setting loop.
		*/
		if (callback) {
			callback();
		}

		/*
		**	This loop ensures that the palette won't be set more than once per game tick. Setting
		**	the palette more than once per game tick will have no effect since the calculation will
		**	result in the same intermediate palette that was previously calculated.
		*/
		while (timer == holdtime && holdtime != 0) {
			if (callback) callback();
		}
	}

	/*
	**	Ensure that the final palette exactly matches the requested
	**	palette before exiting the fading routine.
	*/
	Set_Palette((void*)&newpal[0]);
}

/***********************************************************************************************
 * Process_DD_Result -- Does a message box based on the result of a DD command                 *
 *                                                                                             *
 * INPUT:		HRESULT result				- the result returned from the direct draw command		  *
 *             int     display_ok_msg	- should a message be displayed if command ok			  *                                                                                      *
 *                                                                                             *
 * OUTPUT:		none																									  *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   09/27/1995 PWG : Created.                                                                 *
 *=============================================================================================*/
void Process_DD_Result(HRESULT result, int display_ok_msg)
{
	/*
	**	If there iwas no error detected, then either bail out or display a message to
	**	this effect as indicated by the "display_ok_msg" parameter.
	*/
	if (result == DD_OK) {
		if (display_ok_msg) {
			MessageBox(MainWindow, "Direct Draw operation processed without error", "Note", MB_OK);
		}
		return;
	}

	if (DirectDrawErrorHandler) {
		DirectDrawErrorHandler(result);
		return;
	}

	/*
	**	Since it fell out of the above loop, this must be an unrecognized error code.
	*/
	char str[80];
	sprintf(str, "DDRAW.DLL Error code = %08X", result);
	MessageBox(MainWindow, str, "Direct X", MB_ICONEXCLAMATION|MB_OK);
}

/***********************************************************************************************
 * Check_Overlapped_Blit_Capability -- See if video driver supports blitting overlapped regions*
 *                                                                                             *
 *  We will check for this by drawing something to a video page and blitting it over itself.   *
 * If we end up with the top line repeating then overlapped region blits dont work.            *
 *                                                                                             *
 * INPUT:    Nothing                                                                           *
 *                                                                                             *
 * OUTPUT:   Nothing                                                                           *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *    6/7/96 5:06PM ST : Created                                                               *
 *=============================================================================================*/
void Check_Overlapped_Blit_Capability(void)
{
//	OverlappedVideoBlits = false;

}

void Prep_Direct_Draw(void)
{
	//
	// If there is not currently a direct draw object then we need to define one.
	//
	if ( DirectDrawObject == NULL ) {
		HRESULT result = DirectDrawCreate(NULL, &DirectDrawObject, NULL);
		Process_DD_Result(result, false);
		if (result == DD_OK) {
			if (Debug_Windowed) {
				result = DirectDrawObject->SetCooperativeLevel(MainWindow, DDSCL_NORMAL);
			} else {
				result = DirectDrawObject->SetCooperativeLevel(MainWindow, DDSCL_EXCLUSIVE | DDSCL_FULLSCREEN);
			}
			Process_DD_Result(result, false);
		}
	}
}

/***********************************************************************************************
 * Set_Video_Mode -- Initializes Direct Draw and sets the required Video Mode                  *
 *                                                                                             *
 * INPUT:  		int width   			- the width of the video mode in pixels						  *
 *					int height           - the height of the video mode in pixels                   *
 *					int bits_per_pixel	- the number of bits per pixel the video mode supports     *
 *                                                                                             *
 * OUTPUT:     none                                                                            *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   09/26/1995 PWG : Created.                                                                 *
 *=============================================================================================*/
bool Set_Video_Mode(HWND , int w, int h, int bits_per_pixel)
{
	HRESULT result;

	Prep_Direct_Draw();

	//
	// Set the required display mode with 8 bits per pixel
	//
	//MessageBox(MainWindow, "In Set_Video_Mode. About to call call SetDisplayMode.","Note", MB_ICONEXCLAMATION|MB_OK);
	result = DirectDrawObject->SetDisplayMode(w, h, bits_per_pixel);
	if (result != DD_OK) {
//		Process_DD_Result(result, false);
		DirectDrawObject->Release();
		DirectDrawObject = NULL;
		return(false);
	}

	//
	// Create a direct draw palette object
	//
	//MessageBox(MainWindow, "In Set_Video_Mode. About to call CreatePalette.","Note", MB_ICONEXCLAMATION|MB_OK);
	result = DirectDrawObject->CreatePalette( DDPCAPS_8BIT | DDPCAPS_ALLOW256, &PaletteEntries[0], &PalettePtr, NULL);
	Process_DD_Result(result, false);
	if (result != DD_OK) {
		return (false);
	}

	Check_Overlapped_Blit_Capability();

	//MessageBox(MainWindow, "In Set_Video_Mode. About to return success.","Note", MB_ICONEXCLAMATION|MB_OK);

	//MessageBox(MainWindow, "In Set_Video_Mode. About to return success.","Note", MB_ICONEXCLAMATION|MB_OK);

	return (true);

}

/***********************************************************************************************
 * Reset_Video_Mode -- Resets video mode and deletes Direct Draw Object                        *
 *                                                                                             *
 * INPUT:		none                                                                            *
 *                                                                                             *
 * OUTPUT:     none                                                                            *
 *                                                                                             *
 * WARNINGS:                                                                                   *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   09/26/1995 PWG : Created.                                                                 *
 *=============================================================================================*/
void Reset_Video_Mode(void)
{
	HRESULT result;

	//
	// If a direct draw object has been declared and a video mode has been set
	// then reset the video mode and release the direct draw object.
	//
	if ( DirectDrawObject ) {
		result = DirectDrawObject->RestoreDisplayMode();
		Process_DD_Result(result, false);
		result = DirectDrawObject->Release();
		Process_DD_Result(result, false);

		DirectDrawObject = NULL;
	}
}

/***********************************************************************************************
 * Get_Free_Video_Memory -- returns amount of free video memory                                *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    Nothing                                                                           *
 *                                                                                             *
 * OUTPUT:   bytes of available video RAM                                                      *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *    11/29/95 12:52PM ST : Created                                                            *
 *=============================================================================================*/
unsigned int Get_Free_Video_Memory(void)
{
	DDCAPS	video_capabilities;

	if (DirectDrawObject) {

		video_capabilities.dwSize = sizeof (video_capabilities);

		if (DD_OK == DirectDrawObject->GetCaps (&video_capabilities, NULL)) {
			char string [256];
			wsprintf (string, "In Get_Free_Video_Memory. About to return %d bytes",video_capabilities.dwVidMemFree);
			return (video_capabilities.dwVidMemFree);
		}
	}

	return (0);
}

/***********************************************************************************************
 * Get_Video_Hardware_Caps -- returns bitmask of direct draw video hardware support            *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    Nothing                                                                           *
 *                                                                                             *
 * OUTPUT:   hardware flags                                                                    *
 *                                                                                             *
 * WARNINGS: Must call Set_Video_Mode 1st to create the direct draw object                     *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *    1/12/96 9:14AM ST : Created                                                              *
 *=============================================================================================*/
unsigned Get_Video_Hardware_Capabilities(void)
{
	DDCAPS	video_capabilities;
	unsigned	video;

	/*
	** Fail if the direct draw object has not been initialised
	*/
	if (!DirectDrawObject) return (0);

	/*
	** Get the capabilities of the direct draw object
	*/
	video_capabilities.dwSize = sizeof(video_capabilities);
	//MessageBox(MainWindow, "In Get_Video_Hardware_Capabilities. About to call GetCaps","Note", MB_ICONEXCLAMATION|MB_OK);
	HRESULT result = DirectDrawObject->GetCaps (&video_capabilities, NULL);
	if (result != DD_OK) {
		Process_DD_Result(result, false);
		return (0);
	}

	/*
	** Set flags to indicate the presence of the features we are interested in
	*/
	video = 0;

	/* Hardware blits supported? */
	if (video_capabilities.dwCaps & DDCAPS_BLT) 				video |= VIDEO_BLITTER;

	/* Hardware blits asyncronous? */
	if (video_capabilities.dwCaps & DDCAPS_BLTQUEUE) 		video |= VIDEO_BLITTER_ASYNC;

	/* Can palette changes be synced to vertical refresh? */
	if (video_capabilities.dwCaps & DDCAPS_PALETTEVSYNC) 	video |= VIDEO_SYNC_PALETTE;

	/* Is the video cards memory bank switched? */
	if (video_capabilities.dwCaps & DDCAPS_BANKSWITCHED) 	video |= VIDEO_BANK_SWITCHED;

	/* Can the blitter do filled rectangles? */
	if (video_capabilities.dwCaps & DDCAPS_BLTCOLORFILL)	video |= VIDEO_COLOR_FILL;

	/* Is there no hardware assistance avaailable at all? */
	if (video_capabilities.dwCaps & DDCAPS_NOHARDWARE) 	video |= VIDEO_NO_HARDWARE_ASSIST;

	//MessageBox(MainWindow, "In Get_Video_Hardware_Capabilities. About to return success.","Note", MB_ICONEXCLAMATION|MB_OK);
	return (video);
}

/***********************************************************************************************
 * Wait_Vert_Blank -- Waits for the start (leading edge) of a vertical blank                   *
 *                                                                                             *
 * INPUT:                                                                                      *
 *                                                                                             *
 * OUTPUT:                                                                                     *
 *                                                                                             *
 * WARNINGS:                                                                                   *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *=============================================================================================*/
void Wait_Vert_Blank(void)
{
	if (CanVblankSync) {
		HRESULT result = DirectDrawObject->WaitForVerticalBlank(DDWAITVB_BLOCKBEGIN, 0);
		if (result == E_NOTIMPL) {
			CanVblankSync = false;
			return;
		}
		Process_DD_Result(result, false);
	}
}

/***********************************************************************************************
 * Set_Palette -- set a direct draw palette                                                    *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    ptr to 768 rgb palette bytes                                                      *
 *                                                                                             *
 * OUTPUT:   Nothing                                                                           *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *    10/11/95 3:33PM ST : Created                                                             *
 *=============================================================================================*/
void Set_Palette(void const * palette)
{
	assert(palette != NULL);

	if (&CurrentPalette[0] != palette) {
		memmove(CurrentPalette, palette, sizeof(CurrentPalette));
	}

	if (DirectDrawObject != NULL && PaletteSurface != NULL) {
		unsigned char * palette_get = (unsigned char *)palette;
		for (int index = 0; index < 256; index++) {

			int red = *palette_get++;
			int green = *palette_get++;
			int blue = *palette_get++;

			PaletteEntries[index].peRed = (unsigned char)red;
			PaletteEntries[index].peGreen = (unsigned char)green;
			PaletteEntries[index].peBlue = (unsigned char)blue;
		}

		if (PalettePtr != NULL) {
			if (!FirstPaletteSet) {
				PaletteSurface->SetPalette(PalettePtr);
				FirstPaletteSet = true;
			}

			PalettePtr->SetEntries(0, 0, 256, &PaletteEntries[0]);
		}
	}
}

/***********************************************************************************************
 * Wait_Blit -- waits for the DirectDraw blitter to become idle                                *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    Nothing                                                                           *
 *                                                                                             *
 * OUTPUT:   Nothing                                                                           *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   07-25-95 03:53pm ST : Created                                                             *
 *=============================================================================================*/
void Wait_Blit (void)
{
	HRESULT	return_code;

	do {
		return_code=PaletteSurface->GetBltStatus (DDGBS_ISBLTDONE);
	} while (return_code != DD_OK && return_code != DDERR_SURFACELOST);
}
