// iostream.h compat shim - maps old-style pre-standard iostream.h to standard <iostream>
#pragma once
#ifndef IOSTREAM_H_COMPAT
#define IOSTREAM_H_COMPAT

#include <iostream>
using std::cin;
using std::cout;
using std::cerr;
using std::clog;
using std::endl;
using std::flush;
using std::ostream;
using std::istream;
using std::iostream;

#endif // IOSTREAM_H_COMPAT
