#include "global.h"
#include "strtok_r.h"
#include <string.h>
#include <stdio.h>

//
// Replacement for strtok() that doesn't use a static to
//   store the current position.  The name comes from the
//   POSIX threadsafe version of strtok (r = reentrant).  The
//   user provided var lasts is used in place of the static.
//
// Yes the Windows version of strtok is already threadsafe,
//   but the fact that you can't call a function that uses strtok()
//   during a series of strtok() calls is really annoying.
//
