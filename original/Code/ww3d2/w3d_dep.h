#ifndef W3D_DEP_H
#define W3D_DEP_H

#include <list>

#include <strstream>
#include <string>

typedef std::list<std::string>	StringList;
bool Get_W3D_Dependencies (const char *w3d_filename, StringList &files);

#endif
