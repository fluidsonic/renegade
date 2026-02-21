#if defined(_MSC_VER)
#pragma once
#endif

#ifndef _SUBTITLEMANAGER_H_
#define _SUBTITLEMANAGER_H_

#include "always.h"
#include <wwlib/vector.h>
#include "wwstring.h"
#include <windows.h>
#include "render2dsentence.h"

class SubTitleClass;
class Surface;

class SubTitleManagerClass
{
public:
	// Instantiate a subtitle manager
	static SubTitleManagerClass* Create(const char* filename, const char* subtitlefilename, FontCharsClass* font);

	// Destroy subtitle manager
	~SubTitleManagerClass();

	// Check if there are subtitles.
	bool Has_Sub_Titles(void) const { return (mSubTitles != NULL); }

	// Reset subtitles to start
	void Reset(void);

	// Process subtitles
	bool Process(unsigned long movieTime);
	void Render();

private:
	// Prevent direct creation
	SubTitleManagerClass();

	void Set_Font(FontCharsClass* font);

	bool Load_Sub_Titles(const char* moviename, const char* subtitlefilename);
	void Draw_Sub_Title(const SubTitleClass* subtitle);

	DynamicVectorClass<class SubTitleClass*>* mSubTitles;
	int mSubTitleIndex;
	SubTitleClass* mActiveSubTitle;
	Render2DSentenceClass Renderer;
};

#endif // _SUBTITLEMANAGER_H_
