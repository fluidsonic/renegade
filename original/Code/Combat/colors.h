#ifndef COLORS_H
#define COLORS_H

#include "vector3.h"

const Vector3 COLOR_TEAM_0				= Vector3(1.0f, 0.0f, 0.0f);
const Vector3 COLOR_TEAM_1				= Vector3(1.0f, 0.8f, 0.0f);
const Vector3 COLOR_NEUTRAL			= Vector3(0.8f, 0.8f, 0.8f);
const Vector3 COLOR_RENEGADE			= Vector3(1.0f, 1.0f, 1.0f);
const Vector3 COLOR_PUBLIC_TEXT		= Vector3(1.0f, 1.0f, 1.0f);
const Vector3 COLOR_PRIVATE_TEXT		= Vector3(0.0f, 0.0f, 1.0f);
const Vector3 COLOR_PAGED_TEXT		= Vector3(0.0f, 1.0f, 1.0f);
const Vector3 COLOR_INVITE_TEXT		= Vector3(1.0f, 0.0f, 1.0f);
const Vector3 COLOR_CONSOLE_TEXT		= Vector3(1.0f, 1.0f, 1.0f);

Vector3 Get_Color_For_Team(int team);

#endif // COLORS_H











//const Vector3 COLOR_KOTH				= Vector3(0.2f, 1.0f, 0.2f);
//const Vector3 COLOR_CHAMPION			= Vector3(0.0f, 1.0f, 1.0f);
