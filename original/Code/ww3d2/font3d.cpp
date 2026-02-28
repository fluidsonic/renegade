#include "global.h"
#include "font3d.h"
#include "assetmgr.h"
#include "texture.h"
#include "surfaceclass.h"
#include "texture.h"
#include "vector2i.h"

static	SurfaceClass	*_surface;

/*********************************************************************************************** 
 *                                                                                             * 
 * Font3DDataClass::Font3DDataClass -- constructor																	  * 
 *                                                                                             * 
 * Constructs and load a Targa font image to create a texture matetial								  *
 *                                                                                             * 
 ***********************************************************************************************/
Font3DDataClass::Font3DDataClass( const char *filename )
{
	Texture = NULL;
	Load_Font_Image( filename);
	Name = strdup( filename);
	Name = strupr( Name);
}

/*********************************************************************************************** 
 *                                                                                             * 
 * Font3DDataClass::~Font3DDataClass -- destructor																	  * 
 *                                                                                             * 
 ***********************************************************************************************/
Font3DDataClass::~Font3DDataClass(void)
{
	if (Name != NULL) {
		free(Name);
		Name = NULL;
	}

	REF_PTR_RELEASE(Texture);
}

/*********************************************************************************************** 
 *                                                                                             * 
 * FontClass::Minimize_Font_Image																				  *
 *                                                                                             * 
 * Rebuilds the give image to better pack characters and to insure a square power of two size  *
 * Must be called AFTER Make_Proportional() so each chars minimal bounding box is known        *
 * Will only create a new texture of size 128x128 or 256x256, dependant on original width      *
 *                                                                                             * 
 ***********************************************************************************************/
SurfaceClass *Font3DDataClass::Minimize_Font_Image( SurfaceClass *surface )
{
	SurfaceClass::SurfaceDescription sd;

	surface->Get_Description(sd);

	float current_width = sd.Width;
	float current_height = sd.Height;

	// determine new width make the size of the new image either 128x128 or 256x256, 
	// dependant on the width of the original image
   int new_width;
	if (current_width < 256) {
		new_width = 128;
	} else {
		new_width = 256;
	}

   int new_height = new_width;
	//  create a new 4 bit alpha image to build into
	// We dont support non-homogeneous copies just yet
	SurfaceClass	*new_surface = NEW_REF(SurfaceClass,(static_cast<uint32_t>(new_width), static_cast<uint32_t>(new_height),WW3D_FORMAT_A4R4G4B4));
	//SurfaceClass	*new_surface0 = NEW_REF(SurfaceClass,(new_width, new_height,sd.Format));

	// fill with transparent black	
	new_surface->Clear();

	// indices for the location of each added char
	int	new_x = 0;
	int	new_y = 0;

	// for each character, copy minimum bounding area to (new_x, new_y) in the new image
	for (int char_index = 0; char_index < 256; char_index++) {

		// find the lop left coordinate and the height and width of the char's bounding box
		// (must convert the normalized uv tables to pixels and round off)
		int src_x = (int)(UOffsetTable[ char_index ] * current_width + 0.5f);
		int src_y = (int)(VOffsetTable[ char_index ] * current_height + 0.5f);
		int width = (int)(UWidthTable[ char_index ] * current_width + 0.5f);
		int height = (int)(VHeight * current_height + 0.5f);

		// if the character has any visible pixels at all...
		if (width != 0) {

			// if this charactger will not fit on the current line, goto the next line
			if (new_x + width > new_width) {
				new_x = 0;
				new_y += height;

				// if we have run out of lines, we have a problem
				// we assert because we have already modified tables for some of the chars
				if (new_y + height > new_height) {
					new_y -= height;
				}
			}

			// blit from original image to new image

			new_surface->Copy(static_cast<uint32_t>(new_x), static_cast<uint32_t>(new_y), static_cast<uint32_t>(src_x), static_cast<uint32_t>(src_y), static_cast<uint32_t>(width), static_cast<uint32_t>(height), surface);

		}

		// update the U and V tables to show new character location
		UOffsetTable[ char_index ] = (float)(new_x) / (float)new_width;
		VOffsetTable[ char_index ] = (float)(new_y) / (float)new_width;

		// update width in terms of new normal image width
		UWidthTable[ char_index ] *= (float)current_width / (float)new_width;

		new_x += width;
	}

	// update height in terms of new normal image height
	VHeight *= (float)current_height / (float)new_height;

	// be sure the new image is SMALLER than the old image
//	assert ( (new_width * new_height) <= (current_width * current_height));

	// release the old surface and return the new one
	REF_PTR_RELEASE(surface);

	_surface = new_surface;

	return _surface;
}

/*********************************************************************************************** 
 *                                                                                             * 
 * FontClass::Make_Proportional																					  * 
 *                                                                                             * 
 * Modifys U and Width tables to convert a monospace font into a proportional font.  Hieght	  *
 * remains the same.  Performed by getting the current mono-space bounding box and bringing	  *
 * in the left and right edges to the first non-transparent ( != 0 ) pixel.  Then the U and	  *
 * width tables are updated with the new values.  The image itself is not modified unless...	  *
 * 																														  *
 * we complete by calling Minimize_Font_Image to shink the image & insure a power of 2 square  * 
 *                                                                                             * 
 ***********************************************************************************************/
SurfaceClass *Font3DDataClass::Make_Proportional( SurfaceClass	*surface )
{
	SurfaceClass::SurfaceDescription sd;
	surface->Get_Description(sd);
	float width  =	sd.Width;
	float height =	sd.Height;

	// for each character in the font...
	for (int char_index = 0; char_index < 256; char_index++) {

		// find the current bounding box
		// (must convert the normalized uv tables to pixels and round off)
		int x0 = (int)(UOffsetTable[ char_index ] * width + 0.5f);
		int y0 = (int)(VOffsetTable[ char_index ] * height + 0.5f);
		int x1 = x0 + (int)(UWidthTable[ char_index ] * width + 0.5f);
		int y1 = y0 + (int)(VHeight * height + 0.5f);

		//	find minimum bounding box by finding the minimum and maximum non-0 x pixel location
		Vector2i minb(x0,y0);
		Vector2i maxb(x1,y1);

		surface->FindBB(&minb,&maxb);

		// set the new edges
		x0 = minb.I;
		x1 = maxb.I+1;

		// if we didn't find ANY non-transparent pixels, the char has no width.
		if (x1 < x0) {
			x1 = x0;
		}

		// turn off all character after del
		if (char_index > 0x80) {
			x1 = x0;
		}

		// update the U and width tables
		UOffsetTable[ char_index ] = (float)x0 / width;
		UWidthTable[ char_index ] = (float)( x1 - x0 ) / width;
		CharWidthTable[ char_index ] = static_cast<uint8_t>(x1 - x0);
	}

	// now shink the image given the minimum char sizes
//	surface = Minimize_Font_Image( surface );
	Minimize_Font_Image( _surface );
	return NULL;
}

/*********************************************************************************************** 
 *                                                                                             * 
 * Font3DDataClass::Load_Font_Image( SR_SCENE *scene, char *filename )								  *
 *                                                                                             * 
 * Loads a targa font image file, arranged as 16x16 characters, and builds u v tables to 		  *
 * find each character.  Converts the mono-space font into a proportional font, then uploads	  *
 * the image to the scene as a textur material.                                                *
 *                                                                                             * 
 ***********************************************************************************************/
bool	Font3DDataClass::Load_Font_Image( const char *filename )
{
	// get the font surface
	SurfaceClass	*surface = NEW_REF(SurfaceClass,(filename));

	SurfaceClass::SurfaceDescription sd;
	surface->Get_Description(sd);

	// Derive alpha from RGB luminance — font TGAs are white-on-black with zero alpha channel
	if (sd.Format == WW3D_FORMAT_A8R8G8B8) {
		int32_t pitch = 0;
		uint8_t *bits = (uint8_t *)surface->Lock(&pitch);
		for (uint32_t y = 0; y < sd.Height; y++) {
			uint8_t *row = bits + y * static_cast<uint32_t>(pitch);
			for (uint32_t x = 0; x < sd.Width; x++) {
				uint8_t *p = row + x * 4; // BGRA
				uint8_t m = p[0];
				if (p[1] > m) m = p[1];
				if (p[2] > m) m = p[2];
				p[3] = m; // alpha = max(B,G,R)
			}
		}
		surface->Unlock();

		// Diagnostic: verify alpha derivation
		{
			int32_t dpitch = 0;
			const uint8_t *dbits = (const uint8_t *)surface->Lock(&dpitch);
			bool found_nonzero = false, found_zero = false;
			for (uint32_t dy = 0; dy < sd.Height && (!found_nonzero || !found_zero); dy++) {
				const uint8_t *drow = dbits + dy * static_cast<uint32_t>(dpitch);
				for (uint32_t dx = 0; dx < sd.Width && (!found_nonzero || !found_zero); dx++) {
					const uint8_t *dp = drow + dx * 4;
					if (!found_nonzero && dp[3] > 0) {
						fprintf(stderr, "[font3d] alpha sample nonzero: B=%u G=%u R=%u A=%u\n",
						        dp[0], dp[1], dp[2], dp[3]);
						found_nonzero = true;
					}
					if (!found_zero && dp[3] == 0) {
						fprintf(stderr, "[font3d] alpha sample zero:    B=%u G=%u R=%u A=%u\n",
						        dp[0], dp[1], dp[2], dp[3]);
						found_zero = true;
					}
				}
			}
			surface->Unlock();
		}
	}

	// If input is a font strike (strip) process it as such
	if ( sd.Width > 8 * sd.Height ) {

 		// the height of the strike is the height of the characters
		VHeight = 1;
		CharHeight = static_cast<uint8_t>(sd.Height);

		uint32_t column = 0;
		uint32_t width = sd.Width;


		// for each char, find the uv start location and set the
		// mono-spaced width and height in normalized screen units
		for (int char_index = 0; char_index < 256; char_index++) {

			if ( char_index >= 0x7F ) {

				UOffsetTable[ char_index ] = 0;
				VOffsetTable[ char_index ] = 0;
				UWidthTable[ char_index ] = 0;
				CharWidthTable[ char_index ] = 0;

			} else {

				// find the first non-transparent column...
				while (( column < width ) && ( surface->Is_Transparent_Column(column) )) column++;
				uint32_t start = column;

				// find the first transparent column...
				while (( column < width ) && ( !surface->Is_Transparent_Column(column) )) column++;
				uint32_t end = column;

				if ( end <= start ) {
				}

//				assert( end > start );

				UOffsetTable[ char_index ] = (float)start / (float)width;
				VOffsetTable[ char_index ] = 0;
				UWidthTable[ char_index ] = (float)(end - start) / (float)width;
				CharWidthTable[ char_index ] = static_cast<uint8_t>(end - start);
			}

		}

		// convert the just created mon-spaced font to proportional (optional)
//		surface = Make_Proportional( surface );
		_surface = surface;
		surface = NULL;
		Minimize_Font_Image( _surface );
		{
			// Diagnostic: log new surface dimensions and non-zero-width char count
			if (_surface) {
				SurfaceClass::SurfaceDescription msd;
				_surface->Get_Description(msd);
				fprintf(stderr, "[font3d] after Minimize_Font_Image: surface %ux%u\n",
				        msd.Width, msd.Height);
			}
			uint32_t nonzero_count = 0;
			for (int ci = 0; ci < 256; ci++) {
				if (CharWidthTable[ci] != 0) nonzero_count++;
			}
			fprintf(stderr, "[font3d] non-zero-width chars: %u\n", nonzero_count);
		}

	} else {

		// Determine the width and height of each mono spaced character in pixels
		// (assumes 16x16 array of chars)
		float	font_width = sd.Width;
		float	font_height = sd.Height;
		float	mono_pixel_width = (font_width / 16);
		float	mono_pixel_height = (font_height / 16);

		// for each char, find the uv start location and set the
		// mono-spaced width and height in normalized screen units
		for (int char_index = 0; char_index < 256; char_index++) {
			UOffsetTable[ char_index ] = (float)((char_index % 16) * mono_pixel_width) / font_width;
			VOffsetTable[ char_index ] = (float)((char_index / 16) * mono_pixel_height) / font_height;
			UWidthTable[ char_index ] = mono_pixel_width / font_width;
			CharWidthTable[ char_index ] = static_cast<uint8_t>(mono_pixel_width);
		}
		VHeight = mono_pixel_height / font_height;
		CharHeight = static_cast<uint8_t>(mono_pixel_height);

		// convert the just created mon-spaced font to proportional (optional)

		_surface = surface;
		surface = NULL;
		Make_Proportional( _surface );
		fprintf(stderr, "[font3d] after Make_Proportional: CharWidthTable[0x20]=%u [0x30]=%u [0x41]=%u\n",
		        (uint32_t)CharWidthTable[0x20], (uint32_t)CharWidthTable[0x30], (uint32_t)CharWidthTable[0x41]);
	}

	// create the texture
	if ( _surface ) {
		Texture = NEW_REF(TextureClass,(_surface,TextureClass::MIP_LEVELS_1));
		REF_PTR_RELEASE(_surface);
	}

	// return SUCCESS!
	return true;
}

/*********************************************************************************************** 
 *                                                                                             * 
 * Font3DInstanceClass::Font3DInstanceClass -- constructor											     * 
 *                                                                                             * 
 * Constructs and load a Targa font image to create a texture matetial								  *
 *                                                                                             * 
 ***********************************************************************************************/
Font3DInstanceClass::Font3DInstanceClass( const char *filename )
{
	FontData = WW3DAssetManager::Get_Instance()->Get_Font3DData( filename);
	MonoSpacing = 0.0f;
	Scale = 1.0f;
	SpaceSpacing = (int)(FontData->Char_Width('H') / 2.0f);
	InterCharSpacing = 1;
	Build_Cached_Tables();
}

/*********************************************************************************************** 
 *                                                                                             * 
 * Font3DInstanceClass::~Font3DInstanceClass -- destructor																	  * 
 *                                                                                             * 
 ***********************************************************************************************/
Font3DInstanceClass::~Font3DInstanceClass(void)
{
	REF_PTR_RELEASE(FontData);
}

/*
**
*/
void	Font3DInstanceClass::Set_Mono_Spaced( void )
{ 
	MonoSpacing = FontData->Char_Width('W') + 1;
	Build_Cached_Tables(); 
}

void	Font3DInstanceClass::Build_Cached_Tables()
{
	// Rebuild the cached tables
	for (int a=0;a<256;++a) {
		float width = (float)FontData->Char_Width(static_cast<char16_t>(a));
		if ( a == ' ' ) {
			width = SpaceSpacing;
		}

		ScaledWidthTable[a] = Scale * width;
		if (MonoSpacing != 0.0f) {
			ScaledSpacingTable[a] = Scale * MonoSpacing;
		} else {
			ScaledSpacingTable[a] = Scale * (width + InterCharSpacing);
		}
	}
	ScaledHeight = floor(Scale * (float)FontData->Char_Height('A'));
}

/*********************************************************************************************** 
 *                                                                                             * 
 * Font3DInstanceClass::String_Screen_Width( char *test_str )									        *
 *                                                                                             * 
 * Finds the normalized screenspace width of a character string - useful for checking before   *
 * printing to avoid overflowing the screen.																	  *                                                                                             * 
 ***********************************************************************************************/
float	Font3DInstanceClass::String_Width( const WCHAR *test_str )
{
	float width = 0.0;
	for (; *test_str; test_str++) {
		width += Char_Spacing(*test_str);
	}

	return width;
}

float	Font3DInstanceClass::String_Width( const char *test_str )
{
	float width = 0.0;
	for (; *test_str; test_str++) {
		width += Char_Spacing(static_cast<char16_t>(*test_str));
	}

	return width;
}
