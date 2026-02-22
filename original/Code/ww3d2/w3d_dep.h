#pragma once

#include "global.h"



typedef std::list<std::string>	StringList;
bool Get_W3D_Dependencies (const char *w3d_filename, StringList &files);
