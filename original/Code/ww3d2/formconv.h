#if defined(_MSC_VER)
#pragma once
#endif

#ifndef FORMCONV_H
#define FORMCONV_H

#include "ww3dformat.h"
#include <d3d8.h>

/*
** This file is used for conversions between D3DFORMAT and WW3DFormat.
*/

D3DFORMAT WW3DFormat_To_D3DFormat(WW3DFormat ww3d_format);
WW3DFormat D3DFormat_To_WW3DFormat(D3DFORMAT d3d_format);

void Init_D3D_To_WW3_Conversion();

#endif