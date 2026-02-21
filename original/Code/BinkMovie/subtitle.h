
#ifndef _SUBTITLE_H_
#define _SUBTITLE_H_

#include "always.h"
#include <wchar.h>

class SubTitleClass
{
public:
	SubTitleClass();
	~SubTitleClass();

	// Set the time (in milliseconds) at which the subtitle is to be displayed.
	void Set_Display_Time(unsigned long time) { mTimeStamp = time; }
	
	// Retrieve the time in ticks (1/60 seconds) this subtitle is to be displayed.
	unsigned long Get_Display_Time(void) const { return mTimeStamp; }

	// Set the time duration in ticks (1/60 seconds) for the subtitle to remain displayed.
	void Set_Display_Duration(unsigned long duration) { mDuration = duration; }
	
	// Retrieve the duration time in ticks (1/60 seconds) for the subtitle.
	unsigned long Get_Display_Duration(void) const { return mDuration; }

	// Set the color the subtitle caption should be displayed in.
	void Set_RGB_Color(unsigned char red, unsigned char green, unsigned char blue);

	// Retrieve the color of the subtitle
	unsigned long Get_RGB_Color(void) const { return mRGBColor; }

	// Set the line position the subtitle should be displayed at.
	void Set_Line_Position(int linePos)
	{
		mLinePosition = linePos;
	}
	
	// Retrieve the line position to display the subtitle at.
	int Get_Line_Position(void) const { return mLinePosition; }

	// Caption justifications
	typedef enum
	{
		Left,
		Right,
		Center,
	} Alignment;

	// Set the alignment of the subtitle caption
	void Set_Alignment(Alignment align)
		{mAlignment = align;}

	// Retrieve the caption justification
	Alignment Get_Alignment(void) const { return mAlignment; }

	// Set the caption text
	void Set_Caption(wchar_t* string);

	// Retrieve the caption text
	const wchar_t* Get_Caption(void) const { return mCaption; }

private:
	unsigned long mTimeStamp;
	unsigned long mDuration;
	unsigned long mRGBColor;
	int mLinePosition;
	Alignment mAlignment;
	wchar_t* mCaption;
};

#endif // _SUBTITLE_H_
