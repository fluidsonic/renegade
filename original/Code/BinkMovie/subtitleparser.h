#pragma once

#include "global.h"

#include <wwlib/vector.h>

class Straw;
class SubTitleClass;

class SubTitleParserClass
{
public:
	SubTitleParserClass(Straw& input);
	~SubTitleParserClass();

	DynamicVectorClass<SubTitleClass*>* Get_Sub_Titles(const char* moviename);

private:
	enum {SUBTITLE_LINE_MAX = 1024};

	typedef struct tagTokenHook
	{
		const char16_t* Token;
		void (*Handler)(char16_t* param, SubTitleClass* subTitle);
	} TokenHook;

	// Prevent copy construction
	SubTitleParserClass(const SubTitleParserClass&);
	const SubTitleParserClass operator=(const SubTitleParserClass&);

	bool Find_Movie_Entry(const char* moviename);
	bool Parse_Sub_Title(char16_t* string, SubTitleClass* subTitle);
	void Parse_Token(char16_t* token, char16_t* param, SubTitleClass* subTitle);
	char16_t* Get_Next_Line(void);
	unsigned int Get_Line_Number(void) const	{return mLineNumber;}

	static TokenHook mTokenHooks[];
	Straw& mInput;
	char16_t mBuffer[SUBTITLE_LINE_MAX];
	unsigned int mLineNumber;
};
