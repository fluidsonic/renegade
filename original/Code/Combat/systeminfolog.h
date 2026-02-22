#pragma once

#include "global.h"

#include "wwstring.h"


class SystemInfoLog
{
public:
	static void Set_Current_Level(const char* level_name);
	static void Set_State_Loading();
	static void Set_State_Playing();
	static void Set_State_Exiting();
	static void Reset_State();
	static void Record_Loading_Time(unsigned time_in_seconds);
	static void Record_Frame();

	static void Get_Log(StringClass& string);
	static void Get_Compact_Log(StringClass& string);

private:
	static void Get_Final_String(StringClass& string);

};

// This stuff should go to the playerinfolog...
class PlayerDataClass;

class PlayerInfoLog
{
public:
	static void Append_To_Log(PlayerDataClass* data);
	static void Set_Current_Map_Name(const char* map_name);
	static void Get_Compact_Log(StringClass& string);

	static void Report_Tally_Size(int type, int size);
};
