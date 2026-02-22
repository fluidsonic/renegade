#pragma once

#include "global.h"

#include	"straw.h"
#include	"wwfile.h"

int Read_Line(FileClass & file, char * buffer, int len, bool & eof);
int Read_Line(Straw & file, char * buffer, int len, bool & eof);
int Read_Line(Straw & file, char16_t * buffer, int len, bool & eof);
