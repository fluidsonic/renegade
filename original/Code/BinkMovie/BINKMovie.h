#pragma once

#include "global.h"

#include "wwstring.h"

class FontCharsClass;

// ----------------------------------------------------------------------------
//
// BINK movie player. You'll need to have binkw32.dll in the run directory!
//
// To start a movie call Play("movie.bik","subtitle_name");
// To end movie playing call Stop();
//
// In order to change the subtitle properties see SubTitleManagerClass.
//
// ----------------------------------------------------------------------------

class BINKMovie
{
public:
	static void Play(const char* filename,const char* subtitlename=NULL, FontCharsClass* font = NULL);
	static void Stop();
	static void Update();
	static void Render();
	static void Init();
	static void Shutdown();
	static bool	Is_Complete();
};
