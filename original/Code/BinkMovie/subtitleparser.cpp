#include "subtitleparser.h"
#include "subtitle.h"
#include "straw.h"
#include "readline.h"
#include "trim.h"
#include <stdlib.h>

// Subtitle control file parsing tokens
#define BEGINMOVIE_TOKEN u"BeginMovie"
#define ENDMOVIE_TOKEN   u"EndMovie"
#define TIMEBIAS_TOKEN   u"TimeBias"
#define TIME_TOKEN       u"Time"
#define DURATION_TOKEN   u"Duration"
#define POSITION_TOKEN   u"Position"
#define COLOR_TOKEN      u"Color"
#define TEXT_TOKEN       u"Text"

unsigned long DecodeTimeString(char16_t* string);
void Parse_Time(char16_t* string, SubTitleClass* subTitle);
void Parse_Duration(char16_t* string, SubTitleClass* subTitle);
void Parse_Position(char16_t* string, SubTitleClass* subTitle);
void Parse_Color(char16_t* string, SubTitleClass* subTitle);
void Parse_Text(char16_t* string, SubTitleClass* subTitle);

SubTitleParserClass::TokenHook SubTitleParserClass::mTokenHooks[] =
{
	{TIME_TOKEN, Parse_Time},
	{DURATION_TOKEN, Parse_Duration},
	{POSITION_TOKEN, Parse_Position},
	{COLOR_TOKEN, Parse_Color},
	{TEXT_TOKEN, Parse_Text},
	{NULL, NULL}
};

/******************************************************************************
*
* NAME
*     SubTitleParserClass::SubTitleParserClass
*
* DESCRIPTION
*
* INPUTS
*     Input - Control file input stream.
*
* RESULTS
*     NONE
*
******************************************************************************/

SubTitleParserClass::SubTitleParserClass(Straw& input)
	:
	mInput(input),
	mLineNumber(0)
{
	// Check for Unicode byte-order mark.
	// All Unicode plaintext files are prefixed with the byte-order mark U+FEFF
	// or its mirror U+FFFE. This mark is  used to indicate the byte order of a
	// text stream.
	char16_t byteOrderMark = 0;
	mInput.Get(&byteOrderMark, sizeof(char16_t));

	if (byteOrderMark != 0xFEFF) {
	}
}

/******************************************************************************
*
* NAME
*     SubTitleParserClass::~SubTitleParserClass
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

SubTitleParserClass::~SubTitleParserClass()
{
}

/******************************************************************************
*
* NAME
*     SubTitleParserClass::GetSubTitles
*
* DESCRIPTION
*
* INPUTS
*     NONE
*
* RESULTS
*
******************************************************************************/

DynamicVectorClass<SubTitleClass*>* SubTitleParserClass::Get_Sub_Titles(const char* moviename)
{
	DynamicVectorClass<SubTitleClass*>* subTitleCollection = NULL;
	
	// Find the movie marker
	if (Find_Movie_Entry(moviename) == true)	{
		// Allocate container to hold subtitles
		subTitleCollection = new DynamicVectorClass<SubTitleClass*>;

		if (subTitleCollection != NULL) {
			for (;;) {
				// Retrieve a line from the control file
				char16_t* string = Get_Next_Line();

				if ((string != NULL) && (wcslen(string) > 0)) {
					// Check for subtitle entry markers
					if ((string[0] == u'<') && (string[wcslen(string) - 1] == u'>')) {
						// Trim off markers
						string++;
						string[wcslen(string) - 1] = 0;
						wcstrim(string);

						// Ignore empty caption
						if (wcslen(string) == 0) {
							continue;
						}

						// Create a new SubTitleClass
						SubTitleClass* subTitle = new SubTitleClass();

						if (subTitle == NULL) {
							break;
						}

						if (Parse_Sub_Title(string, subTitle) == true)	{
							subTitleCollection->Add(subTitle);
						}
						else {
							delete subTitle;
						}

						continue;
					}

					// Terminate if end movie token encountered.
					if (wcsnicmp(string, ENDMOVIE_TOKEN, wcslen(ENDMOVIE_TOKEN)) == 0) {
						break;
					}
				}
			}

			if (subTitleCollection->Count() == 0) {
				delete subTitleCollection;
				subTitleCollection = NULL;
			}
		}
	}

	return subTitleCollection;
}

/******************************************************************************
*
* NAME
*     SubTitleParserClass::FindMovieEntry
*
* DESCRIPTION
*     No description provided,
*
* INPUTS
*     Moviename - Pointer to name of movie to find subtitles for.
*
* RESULTS
*     Success - True if movie entry found; False if unable to find movie entry.
*
******************************************************************************/

bool SubTitleParserClass::Find_Movie_Entry(const char* moviename)
{
	// Convert the moviename into Unicode (ASCII only — movie names are ASCII)
	char16_t wideName[32];
	{ int _i = 0; for (; _i < 31 && moviename[_i]; _i++) wideName[_i] = (char16_t)(unsigned char)moviename[_i]; wideName[_i] = 0; }

	do {
		// Retrieve line of text
		char16_t* string = Get_Next_Line();

		// Terminate if no string read.
		if (string == NULL) {
			break;
		}

		// Look for begin movie token
		if (wcsnicmp(string, BEGINMOVIE_TOKEN, wcslen(BEGINMOVIE_TOKEN)) == 0) {
			// Get moviename following the token
			char16_t* ptr = wcschr(string, u' ');

			// Check for matching moviename
			if (ptr != NULL) {
				wcstrim(ptr);

				if (wcsicmp(ptr, wideName) == 0) {
					return true;
				}
			}
		}
	} while (true);

	return false;
}

/******************************************************************************
*
* NAME
*     SubTitleParserClass::ParseSubTitle
*
* DESCRIPTION
*
* INPUTS
*     char16_t* string
*     SubTitleClass* subTitle
*
* RESULTS
*     bool
*
******************************************************************************/

bool SubTitleParserClass::Parse_Sub_Title(char16_t* string, SubTitleClass* subTitle)
{
	// Parameter check

	for (;;) {
		// Find token separator
		char16_t* separator = wcschr(string, u'=');

		if (separator == NULL) {
			return false;
		}

		// NULL terminate token part
		*separator++ = 0;

		// Tokens are to the left of the separator
		char16_t* token = string;
		wcstrim(token);

		// Parameters are to the right of the separator
		char16_t* param = separator;
		wcstrim(param);

		// Quoted parameters are treated as literals (ignore contents)
		if (param[0] == u'"') {
			// Skip leading quote
			param++;

			// Use next quote to mark end of parameter
			separator = wcschr(param, u'"');

			if (separator == NULL) {
				return false;
			}

			// NULL terminate parameter
			*separator++ = 0;

			// Skip any comma following a literal string since we used the trailing
			// quote to terminate the tokens parameters
			wcstrim(separator);
			
			if (*separator == u',') {
				separator++;
			}

			// Advance string past quoted parameter
			string = separator;
		}
		else {
			// Look for separator to next token
			separator = wcspbrk(param, u", ");

			if (separator != NULL) {
				*separator++ = 0;
				string = separator;
			}
			else {
				static char16_t _empty[1] = {0};
				string = _empty;
			}
		}

		// Error on empty tokens
		if (wcslen(token) == 0) {
			return false;
		}

		// Parse current token
		Parse_Token(token, param, subTitle);

		// Prepare for next token
		wcstrim(string);

		if (wcslen(string) == 0) {
			break;
		}
	}

	return true;
}

/******************************************************************************
*
* NAME
*     SubTitleParserClass::ParseToken
*
* DESCRIPTION
*
* INPUTS
*     char16_t* token
*     char16_t* param
*     SubTitleClass* subTitle
*
* RESULTS
*     NONE
*
******************************************************************************/

void SubTitleParserClass::Parse_Token(char16_t* token, char16_t* param, SubTitleClass* subTitle)
{
	// Parameter check

	if (token != NULL) {
		int index = 0;

		while (mTokenHooks[index].Token != NULL) {
			TokenHook& hook = mTokenHooks[index];

			if (wcsicmp(hook.Token, token) == 0) {
				hook.Handler(param, subTitle);
				return;
			}

			index++;
		}
	}
}

/******************************************************************************
*
* NAME
*     SubTitleParserClass::GetNextLine
*
* DESCRIPTION
*     Retrieve the next line of text from the control file.
*
* INPUTS
*     NONE
*
* RESULTS
*     String - Pointer to next line of text. NULL if error or EOF.
*
******************************************************************************/

char16_t* SubTitleParserClass::Get_Next_Line(void)
{
	bool eof = false;

	while (eof == false) {
		// Read in a line of text
		Read_Line(mInput, mBuffer, SUBTITLE_LINE_MAX, eof);
		mLineNumber++;

		// Remove whitespace
		char16_t* string = wcstrim(mBuffer);

		// Skip comments and blank lines
		if ((wcslen(string) > 0) && (string[0] != u';')) {
			return string;
		}
	}

	return NULL;
}

// Convert a time string in the format hh:mm:ss:tt into 1/60 second ticks.
unsigned long Decode_Time_String(char16_t* string)
{
	#define TICKS_PER_SECOND 60
	#define TICKS_PER_MINUTE (60 * TICKS_PER_SECOND)
	#define TICKS_PER_HOUR   (60 * TICKS_PER_MINUTE)

	char16_t buffer[12];
	wcsncpy(buffer, string, 12);
	buffer[11] = 0;

	char16_t* ptr = &buffer[0];

	// Isolate hours part
	char16_t* separator = wcschr(ptr, u':');
	*separator++ = 0;
	unsigned long hours = wcstoul(ptr, NULL, 10);

	// Isolate minutes part
	ptr = separator;
	separator = wcschr(ptr, u':');
	*separator++ = 0;
	unsigned long minutes = wcstoul(ptr, NULL, 10);

	// Isolate seconds part
	ptr = separator;
	separator = wcschr(ptr, u':');
	*separator++ = 0;
	unsigned long seconds = wcstoul(ptr, NULL, 10);

	// Isolate hundredth part (1/100th of a second)
	ptr = separator;
	unsigned long hundredth = wcstoul(ptr, NULL, 10);

	unsigned long time = (hours * TICKS_PER_HOUR);
	time += (minutes * TICKS_PER_MINUTE);
	time += (seconds * TICKS_PER_SECOND);
	time += ((hundredth * TICKS_PER_SECOND) / 100);

	return time;
}

void Parse_Time(char16_t* param, SubTitleClass* subTitle)
{
	unsigned long time = Decode_Time_String(param);
	subTitle->Set_Display_Time(time);
}

void Parse_Duration(char16_t* param, SubTitleClass* subTitle)
{
	unsigned long time = Decode_Time_String(param);

	if (time > 0) {
		subTitle->Set_Display_Duration(time);
	}
}

void Parse_Position(char16_t* param, SubTitleClass* subTitle)
{
	static struct
	{
		const char16_t* Name;
		SubTitleClass::Alignment Align;
		} _alignLookup[] = {
			{u"Left", SubTitleClass::Left},
			{u"Right", SubTitleClass::Right},
			{u"Center", SubTitleClass::Center},
			{NULL, SubTitleClass::Center}
	};

	char16_t* ptr = param;

	// Line position
	char16_t* separator = wcschr(ptr, u':');

	if (separator != NULL) {
		*separator++ = 0;
		int linePos = wcstol(ptr, NULL, 0);
		subTitle->Set_Line_Position(linePos);
		ptr = separator;
	}

	// Justification
	SubTitleClass::Alignment align = SubTitleClass::Center;
	int index = 0;

	while (_alignLookup[index].Name != NULL) {
		if (wcsicmp(ptr, _alignLookup[index].Name) == 0) {
			align = _alignLookup[index].Align;
			break;
		}

		index++;
	}

	subTitle->Set_Alignment(align);
}

void Parse_Color(char16_t* param, SubTitleClass* subTitle)
{

	char16_t* ptr = param;

	char16_t* separator = wcschr(ptr, u':');
	*separator++ = 0;
	unsigned char red = (unsigned char)wcstoul(ptr, NULL, 10);
	
	ptr = separator;
	separator = wcschr(ptr, u':');
	*separator++ = 0;
	unsigned char green = (unsigned char)wcstoul(ptr, NULL, 10);

	ptr = separator;
	unsigned char blue = (unsigned char)wcstoul(ptr, NULL, 10);

	subTitle->Set_RGB_Color(red, green, blue);
}

void Parse_Text(char16_t* param, SubTitleClass* subTitle)
{

	subTitle->Set_Caption(param);
}
