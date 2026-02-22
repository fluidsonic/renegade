#pragma once

#include "global.h"

int Base64_Encode(void const * source, int slen, void * dest, int dlen);
int Base64_Decode(void const * source, int slen, void * dest, int dlen);
