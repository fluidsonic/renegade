#include "consolemode.h"
#include "consolefunction.h"
#include "conio.h"
#include <stdio.h>
#include "systimer.h"
#include "widestring.h"
#include "vector3.h"
#include "cnetwork.h"
#include "textdisplay.h"
#include "console.h"
#include "crc.h"
#include "buildnum.h"
#include "init.h"
#include "gamesideservercontrol.h"
#include "serversettings.h"

/*
** Single instance of console.
*/
ConsoleModeClass ConsoleBox;

/*
** Console title bar text
*/
#define MASTER_TITLE_BASE "Renegade Master Server"

#define MASTER_COLORS (FOREGROUND_GREEN | FOREGROUND_BLUE | FOREGROUND_INTENSITY | BACKGROUND_BLUE)

/***********************************************************************************************
 * ConsoleModeClass::ConsoleModeClass -- Class constructor                                     *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    Nothing                                                                           *
 *                                                                                             *
 * OUTPUT:   Nothing                                                                           *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   12/17/2001 5:00PM ST : Created                                                            *
 *=============================================================================================*/
ConsoleModeClass::ConsoleModeClass(void)
{
	ConsoleOutputHandle = INVALID_HANDLE_VALUE;
	ConsoleInputHandle = INVALID_HANDLE_VALUE;
	LastKeypressTime = 0;
	Pos = 1;
	IsExclusive = false;
	LastProfileCRC = 0;
	LastProfilePrint = 0;
	ProfileMode = false;
}

/***********************************************************************************************
 * ConsoleModeClass::~ConsoleModeClass -- Class destructor                                     *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    Nothing                                                                           *
 *                                                                                             *
 * OUTPUT:   Nothing                                                                           *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   12/17/2001 5:01PM ST : Created                                                            *
 *=============================================================================================*/
ConsoleModeClass::~ConsoleModeClass(void)
{
	if (ConsoleOutputHandle != INVALID_HANDLE_VALUE) {
		FreeConsole();
		ConsoleOutputHandle = INVALID_HANDLE_VALUE;
	}
}

/***********************************************************************************************
 * ConsoleModeClass::Init -- Enable console mode - start up a console                          *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    Nothing                                                                           *
 *                                                                                             *
 * OUTPUT:   Nothing                                                                           *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   12/17/2001 5:01PM ST : Created                                                            *
 *=============================================================================================*/
void ConsoleModeClass::Init(void)
{
	if (ConsoleOutputHandle == INVALID_HANDLE_VALUE) {

		/*
		** Create a console.
		*/
		if (AllocConsole()) {

			/*
			** Get the input and output handles.
			*/
			ConsoleOutputHandle = GetStdHandle(STD_OUTPUT_HANDLE);

			ConsoleInputHandle = GetStdHandle(STD_INPUT_HANDLE);

			/*
			** Set the size of the console buffer.
			*/
			COORD coord;
			coord.X=80;
			coord.Y=4192;
			SetConsoleScreenBufferSize(ConsoleOutputHandle, coord);
			unsigned long written = 0;
			coord.X=0;
			coord.Y=0;

			FillConsoleOutputAttribute(ConsoleOutputHandle, MASTER_COLORS, 4192*50, coord, &written);
			SetConsoleTextAttribute(ConsoleOutputHandle, MASTER_COLORS);

			/*
			** Set the text in the console title bar.
			*/
			Set_Title(NULL, NULL);

			/*
			** Get an HWND for the console window.
			*/
			ConsoleWindow = FindWindow("ConsoleWindowClass", Title);

			/*
			** Bring up the console window to the foreground.
			*/
			SetForegroundWindow(ConsoleWindow);

			/*
			** Print up version info.
			*/
			DWORD version_major = 1;
			DWORD version_minor = 0;
			Get_Version_Number(&version_major, &version_minor);
// FDS: console mode was enabled by default (no graphical UI)
			Print("Renegade ");
			Print("v%d.%.3d %s-%s %s\n", (version_major >> 16), (version_major & 0xFFFF), BuildInfoClass::Get_Builder_Initials(), BuildInfoClass::Get_Build_Number_String(), BuildInfoClass::Get_Build_Date_String());
			Print("Console mode active\n");

			LastKeypressTime = 0;
		}
	}
}

/***********************************************************************************************
 * ConsoleModeClass::Compose_Window_Title -- Build a window title string from name and settings*
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    Login name                                                                        *
 *           Settings file name                                                                *
 *                                                                                             *
 * OUTPUT:   StringClass containing full title bar string                                      *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   2/4/2002 1:19PM ST : Created                                                              *
 *=============================================================================================*/
StringClass ConsoleModeClass::Compose_Window_Title(char *name, char *settings)
{
	char title[256];

	strcpy(title, MASTER_TITLE_BASE);

	if (name) {
		strcat(title, " - ");
		strcat(title, name);
	}

	if (settings) {
		strcat(title, " - ");
		strcat(title, settings);
	}
	return(StringClass(title));
}

/***********************************************************************************************
 * ConsoleModeClass::Set_Title -- Sets the text in the console title bar                       *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    Base text                                                                         *
 *           Name of settings file                                                             *
 *                                                                                             *
 * OUTPUT:   Nothing                                                                           *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   12/17/2001 5:03PM ST : Created                                                            *
 *=============================================================================================*/
void ConsoleModeClass::Set_Title(char *name, char *settings)
{
	if (ConsoleOutputHandle) {
		StringClass title = Compose_Window_Title(name, settings);
		strcpy(Title, title.Peek_Buffer());
		SetConsoleTitle(Title);
	}
}

/***********************************************************************************************
 * ConsoleModeClass::Print -- Formatted print to console box                                   *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    string                                                                            *
 *           format specifiers                                                                 *
 *                                                                                             *
 * OUTPUT:   Nothing                                                                           *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   12/17/2001 5:04PM ST : Created                                                            *
 *=============================================================================================*/
void ConsoleModeClass::Print(char const * string, ...)
{
	if (ConsoleOutputHandle != INVALID_HANDLE_VALUE) {
		char buffer[8192];

		va_list va;

		va_start(va, string);
		vsprintf(&buffer[0], string, va);
		va_end(va);

		/*
		** Have to use '%s' here or we end up doing the formatting twice.
		*/
		cprintf("%s", buffer);
		Log_To_Disk(buffer);
		//
		Apply_Attributes();
	}
}

/***********************************************************************************************
 * ConsoleModeClass::Print_Maybe -- Formatted print to console box if not busy                 *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    string                                                                            *
 *           format specifiers                                                                 *
 *                                                                                             *
 * OUTPUT:   Nothing                                                                           *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   12/17/2001 5:04PM ST : Created                                                            *
 *=============================================================================================*/
void ConsoleModeClass::Print_Maybe(char const * string, ...)
{
	if (string && !ProfileMode) {

		char buffer[8192];
		va_list va;
		va_start(va, string);
		vsprintf(&buffer[0], string, va);
		va_end(va);
		Log_To_Disk(buffer);

		if (Pos == 1 && (TIMEGETTIME() - LastKeypressTime > 5 * 1000) && ConsoleOutputHandle != INVALID_HANDLE_VALUE) {

			/*
			** Have to use '%s' here or we end up doing the formatting twice.
			*/
			cprintf("%s", buffer);
			//
			Apply_Attributes();
		}
	}
}

/***********************************************************************************************
 * ConsoleModeClass::Static_Print_Maybe -- Static version of Print_Maybe                       *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    String                                                                            *
 *                                                                                             *
 * OUTPUT:   Nothing                                                                           *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   8/14/2002 11:59AM ST : Created                                                            *
 *=============================================================================================*/
void ConsoleModeClass::Static_Print_Maybe(char const * string, ...)
{
	ConsoleBox.Print_Maybe(string);
}

/***********************************************************************************************
 * ConsoleModeClass::cprintf -- let's get all the prints going through one place again         *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    string                                                                            *
 *           format specifiers                                                                 *
 *                                                                                             *
 * OUTPUT:   Nothing                                                                           *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   12/17/2001 5:04PM ST : Created                                                            *
 *=============================================================================================*/
void ConsoleModeClass::cprintf(char const * string, ...)
{
	if (string)	{
		char buffer[8192];

		va_list va;

		buffer[sizeof(buffer)-1] = 0;
		va_start(va, string);
		_vsnprintf(&buffer[0], sizeof(buffer)-1, string, va);
		va_end(va);

		/*
		** Have to use '%s' here or we end up doing the formatting twice.
		*/
		::cprintf("%s", buffer);
		GameSideServerControlClass::Print("%s", buffer);
	}
}

/***********************************************************************************************
 * ConsoleModeClass::Get_Log_File_Nmae -- Get name of log file                                 *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    Nothing                                                                           *
 *                                                                                             *
 * OUTPUT:   Nothing                                                                           *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   8/8/2002 1:07PM ST : Created                                                              *
 *=============================================================================================*/
const char *ConsoleModeClass::Get_Log_File_Name(void)
{
	static char _log_file_name[256];
	static int _last_day = -1;

	SYSTEMTIME time;
	GetLocalTime(&time);
	sprintf(_log_file_name, "renlog_%d-%d-%02d.txt", time.wMonth, time.wDay, time.wYear);

	if (_last_day != time.wDay && ServerSettingsClass::Get_Disk_Log_Size() != -1) {
		_last_day = time.wDay;

		FILETIME file_time;
		if (SystemTimeToFileTime(&time, &file_time)) {
			_int64 int_file_time;
			memcpy(&int_file_time, &file_time, sizeof(int_file_time));
			_int64 time_diff = ((_int64)10000000) * ((_int64)60*60*24*ServerSettingsClass::Get_Disk_Log_Size());
			int_file_time -= time_diff;
			memcpy(&file_time, &int_file_time, sizeof(file_time));

			/*
			** Find all log files.
			*/
  			WIN32_FIND_DATA find_data;
			HANDLE find_handle = FindFirstFile("renlog_*.txt", &find_data);
			while (find_handle != INVALID_HANDLE_VALUE) {
				if (CompareFileTime(&find_data.ftLastWriteTime, &file_time) == -1) {
					DeleteFile(find_data.cFileName);
				}
				if (!FindNextFile(find_handle, &find_data)) {
					break;
				}
			}
			FindClose(find_handle);
		}
	}

	return(_log_file_name);
}

/***********************************************************************************************
 * ConsoleModeClass::Log_To_Disk -- Log console output to disk                                 *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    String to log                                                                     *
 *                                                                                             *
 * OUTPUT:   Nothing                                                                           *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   8/8/2002 12:47PM ST : Created                                                             *
 *=============================================================================================*/
void ConsoleModeClass::Log_To_Disk(const char *string)
{
	if (ConsoleOutputHandle != INVALID_HANDLE_VALUE) {
		if (ServerSettingsClass::Get_Disk_Log_Size() > 0) {
   		FILE *log_file = fopen(Get_Log_File_Name(), "at");
   		if (log_file != NULL) {
				char timestr[256] = "?";
				GetTimeFormat(LOCALE_SYSTEM_DEFAULT, TIME_FORCE24HOURFORMAT, NULL, "'['HH':'mm':'ss'] '", timestr, 255);
			   fwrite(timestr, 1, strlen(timestr), log_file);
			   fwrite(string, 1, strlen(string), log_file);
			   fclose(log_file);
   		}
		}
	}
}

/***********************************************************************************************
 * ConsoleModeClass::Think -- Handles input to the console box                                 *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    Nothing                                                                           *
 *                                                                                             *
 * OUTPUT:   Nothing                                                                           *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   12/17/2001 5:04PM ST : Created                                                            *
 *=============================================================================================*/
void ConsoleModeClass::Think(void)
{
	static char string[256] = ">";
	char key = 0;
	static char suggestion[256] = "";
	static char last_suggestion[256] = "";
	static char help[256] = "";
	static char suggestion_stub[256];
	static unsigned long last_info_time = 0;
	static int num_players = -1;	//eh?

	static int delay = 100;

	if (ConsoleInputHandle != INVALID_HANDLE_VALUE) {

		/*
		** See if there is a key waiting in the queue.
		*/
		if (_kbhit()) {

			/*
			** Get the key from the queue.
			*/
			key = _getche();

			if (key == 0 || key == 0xE0) {
				key = _getche();
			}

			/*
			** Note the time of the last keypress.
			*/
			LastKeypressTime = TIMEGETTIME();

			switch (key) {

				/*
				** TAB key used to get command line suggestions.
				*/
				case 9:
				{
					string[Pos] = 0;
					if (!strlen(last_suggestion)) {
						strcpy(suggestion_stub, string+1);
					}
					ConsoleFunctionManager::Get_Command_Suggestion(suggestion_stub, last_suggestion, suggestion, help, 256);
					strcpy(last_suggestion, suggestion);

					/*
					** Save the cursor position.
					*/
					CONSOLE_SCREEN_BUFFER_INFO info;
					int ok = GetConsoleScreenBufferInfo(ConsoleOutputHandle, &info);

					/*
					** Clear out the two lines below.
					*/
					cprintf("\r\n                                                                               ");
					cprintf("\r\n                                                                               ");

					/*
					** Move the cursor back up one line to the current command prompt line.
					*/
					if (ok) {
						COORD new_pos = info.dwCursorPosition;
						new_pos.X = 0;
						SetConsoleCursorPosition(ConsoleOutputHandle, new_pos);
					}

					/*
					** Print the suggestion on the line below.
					*/
					cprintf("\r\n%s\r", help);

					/*
					** Move the cursor back up one line to the current command prompt line.
					*/
					if (ok) {
						COORD new_pos = info.dwCursorPosition;
						new_pos.X = 0;
						SetConsoleCursorPosition(ConsoleOutputHandle, new_pos);
					}

					/*
					** Clear out the command line and print the suggestion.
					*/
					cprintf("\r                                                                              \r>%s", suggestion);
					strcpy(string+1, suggestion);
					Pos = strlen(string);
					break;
				}

				/*
				** Handle backspace.
				*/
				case 8:
					if (Pos > 1) {
						Pos--;
						last_suggestion[0] = 0;
						cprintf(" \b");
					} else {
						/*
						** Compensate for backspace going too far.
						*/
						if (Pos == 1) {
							cprintf(">");
						}
					}
					break;

				/*
				** Handle escape.
				*/
				case 27:
					if (ProfileMode) {
						StatisticsDisplayManager::Set_Display("off");
						ProfileMode = false;
						Print("\n\n\n\n");
						Print("\n\n\n\n");
						Print("\n\n\n\n");
						Print("\n\n\n\n");
					}
					cprintf("\r                                                                              \r>");
					Pos = 1;
					string[0] = '>';
					string[1] = 0;
					break;

				/*
				** Anything else gets put into the command buffer.
				*/
				default:
					if (key == 32 || isgraph(key)) {
						string[Pos++] = key;
						last_suggestion[0] = 0;
					}
					break;
			}

			/*
			** Handle user hitting enter (13).
			*/
			if (Pos > 1 && key == 13 || Pos > 200) {
				string[Pos] = 0;
				cprintf("\r\n                                                                              \r>");

				/*
				** Pass the command string to the console parser.
				*/
				ConsoleFunctionManager::Parse_Input(string+1);
				cprintf("\r\n>");
				Pos = 1;
				string[0] = '>';
				Apply_Attributes();
			}
		}

		/*
		** Print up game info if console is idle.
		*/
		delay--;
		if (delay <= 0) {
			delay = 100;
			unsigned long time = TIMEGETTIME();

			/*
			** Handle timer reset.
			*/
			if (time < last_info_time) {
				last_info_time = time;
			}

			/*
			** Print if enough time has gone by.
			*/
			if (Pos == 1 && time - LastKeypressTime > 30 * 1000) {
				if (time - last_info_time > 60 * 1000) {
					if (cNetwork::PServerConnection && cNetwork::PServerConnection->Get_Num_RHosts() != num_players) {
						num_players = cNetwork::PServerConnection->Get_Num_RHosts();
						ConsoleFunctionManager::Parse_Input("game_info");
						cprintf(">");
						last_info_time = time;
						Apply_Attributes();
					}
				}
			}
		}
	}
}

/***********************************************************************************************
 * ConsoleModeClass::Add_Message -- Print colored text to the console                          *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    Formatted wide string                                                             *
 *           Color to print in                                                                 *
 *           Forced - set to true to print even if the user is currently typing at the console *
 *                                                                                             *
 * OUTPUT:   Nothing                                                                           *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   12/17/2001 5:06PM ST : Created                                                            *
 *=============================================================================================*/
void ConsoleModeClass::Add_Message(WideStringClass *formatted_text, Vector3 *text_color, bool forced)
{

	if (!ProfileMode && formatted_text && text_color && (forced || (Pos == 1 && TIMEGETTIME() - LastKeypressTime > 3 * 1000))) {

		unsigned short color = 0;

		/*
		** Convert the Vector3 RGB to text attribute colors.
		*/
		if (text_color->X != 0.0f) {
			color |= FOREGROUND_RED;
			if (text_color->X > 0.4f) {
				color |= FOREGROUND_INTENSITY;
			}
		}

		if (text_color->Y != 0.0f) {
			color |= FOREGROUND_GREEN;
			if (text_color->Y > 0.4f) {
				color |= FOREGROUND_INTENSITY;
			}
		}

		if (text_color->Z != 0.0f) {
			color |= FOREGROUND_BLUE;
			if (text_color->Z > 0.4f) {
				color |= FOREGROUND_INTENSITY;
			}
		}

		SetConsoleTextAttribute(ConsoleOutputHandle, color | BACKGROUND_BLUE);

		StringClass string(128, true);
		formatted_text->Convert_To(string);
		cprintf("%s", string.Peek_Buffer());
		Log_To_Disk(string.Peek_Buffer());

		SetConsoleTextAttribute(ConsoleOutputHandle, MASTER_COLORS);

		/*
		** Reprint the prompt.
		*/
		//cprintf(">");

		Apply_Attributes();
	}
}

/***********************************************************************************************
 * ConsoleModeClass::Apply_Attributes -- Reapply the console colors.                           *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    Nothing                                                                           *
 *                                                                                             *
 * OUTPUT:   Nothing                                                                           *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   12/24/2001 1:26PM ST : Created                                                            *
 *=============================================================================================*/
void ConsoleModeClass::Apply_Attributes(void)
{

	CONSOLE_SCREEN_BUFFER_INFO info;
	int ok = GetConsoleScreenBufferInfo(ConsoleOutputHandle, &info);

	if (ok) {
		COORD pos = info.dwCursorPosition;
		unsigned long written = 0;

		FillConsoleOutputAttribute(ConsoleOutputHandle, MASTER_COLORS, 5*80, pos, &written);
	}
}

// unrecognized character escape sequence

/***********************************************************************************************
 * ConsoleModeClass::Wait_For_Keypress -- Wait for user to press a key                         *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    Nothing                                                                           *
 *                                                                                             *
 * OUTPUT:   Nothing                                                                           *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   2/4/2002 11:10AM ST : Created                                                             *
 *=============================================================================================*/
void ConsoleModeClass::Wait_For_Keypress(void)
{
	if (Get_Console()) {
		if (ConsoleInputHandle != INVALID_HANDLE_VALUE) {
			Print("** Press any key to continue **\n");
		}
		_getch();
	}
}
