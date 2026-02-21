#include "always.h"
#include "subtitle.h"


/******************************************************************************
*
* NAME
*     SubTitleClass::SubTitleClass
*
* DESCRIPTION
*
* INPUTS
*     NONE
*
* RESULTS
*     NONE
*
******************************************************************************/

SubTitleClass::SubTitleClass()
	:
	mTimeStamp(0),
	mDuration(20 * 60),
	mRGBColor(0x00FFFFFF),
	mLinePosition(15),
	mAlignment(Center),
	mCaption(NULL)
{
}


/******************************************************************************
*
* NAME
*     SubTitleClass::~SubTitleClass
*
* DESCRIPTION
*
* INPUTS
*     NONE
*
* RESULTS
*     NONE
*
******************************************************************************/

SubTitleClass::~SubTitleClass()
{
	if (mCaption != NULL) {
		delete[] mCaption;
	}
}


/******************************************************************************
*
* NAME
*     SubTitleClass::SetRGBColor
*
* DESCRIPTION
*     Set the color the subtitle caption should be displayed in.
*
* INPUTS
*     unsigned char red
*     unsigned char green
*     unsigned char blue
*
* RESULTS
*     NONE
*
******************************************************************************/

void SubTitleClass::Set_RGB_Color(
	unsigned char red, 
	unsigned char green,
	unsigned char blue)
{
	// Combine components as 8:8:8
	mRGBColor = (
		((unsigned long)red << 16) |
		((unsigned long)green << 8) |
		(unsigned long)blue);
}


/******************************************************************************
*
* NAME
*     SubTitleClass::SetCaption
*
* DESCRIPTION
*     Set the caption text
*
* INPUTS
*     Caption - Caption string
*
* RESULTS
*     NONE
*
******************************************************************************/

void SubTitleClass::Set_Caption(wchar_t* string)
{
	// Release existing caption
	if (mCaption != NULL) {
		delete[] mCaption;
		mCaption = NULL;
	}

	// Make a copy of caption
	if (string != NULL) {
		unsigned int length = wcslen(string);
		mCaption = new wchar_t[length + 1];
		WWASSERT(mCaption != NULL);

		if (mCaption != NULL) {
			wcscpy(mCaption, string);
		}
	}
}
