#pragma once

#include "global.h"

#include	"bsurface.h"
#include	"palette.h"
#include	"wwfile.h"
#include	<string.h>

struct RGB {
	unsigned char	red;
	unsigned char	green;
	unsigned char	blue;
};

struct PCX_HEADER
{
	char	id;
	char	version;
	char	encoding;
	char	pixelsize;
	short	x;
	short	y;
	short	width;
	short	height;
	short	xres;
	short	yres;
	RGB	ega_palette[16];
	char	nothing;
	char	color_planes;
	unsigned short	byte_per_line;
	short	palette_type;
	char	filler[58];
};

Surface * Read_PCX_File(FileClass & file_handle, PaletteClass * palette= NULL, void * buff=NULL, long size=0);
//Surface * Read_PCX_File (char * name, Buffer & Buff, PaletteClass * palette= NULL) ;
bool Write_PCX_File(FileClass & file, Surface & pic, PaletteClass * palette);
