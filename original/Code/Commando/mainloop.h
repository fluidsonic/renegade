#pragma once

#include "global.h"

// If the program returns this exitcode then it wants the launcher to check for patches.
#define UPDATE_EXITCODE 123456789
#define RESTART_EXITCODE 1

int	Game_Main_Loop(void);
void	Stop_Main_Loop(int exitCode);
