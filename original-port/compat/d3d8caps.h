// d3d8caps.h compat shim for macOS
#pragma once
#ifndef D3D8CAPS_H_COMPAT
#define D3D8CAPS_H_COMPAT

#include "d3d8types.h"

#define D3DDEVCAPS_HWRASTERIZATION            0x00080000L
#define D3DDEVCAPS_HWTRANSFORMANDLIGHT        0x00010000L
#define D3DDEVCAPS_PUREDEVICE                 0x00100000L
#define D3DDEVCAPS_TLVERTEXVIDEOMEMORY        0x00000080L
#define D3DDEVCAPS_EXECUTESYSTEMMEMORY        0x00000010L

#define D3DPTEXTURECAPS_PERSPECTIVE   0x00000001L
#define D3DPTEXTURECAPS_POW2          0x00000002L
#define D3DPTEXTURECAPS_ALPHA         0x00000004L
#define D3DPTEXTURECAPS_CUBEMAP       0x00000800L
#define D3DPTEXTURECAPS_VOLUMEMAP     0x00002000L
#define D3DPTEXTURECAPS_MIPMAP        0x00004000L
#define D3DPTEXTURECAPS_NONPOW2CONDITIONAL 0x00000100L

#define D3DPRASTERCAPS_FOGTABLE       0x00000100L
#define D3DPRASTERCAPS_FOGVERTEX      0x00000080L
#define D3DPRASTERCAPS_ANISOTROPY     0x00020000L
#define D3DPRASTERCAPS_MIPMAPLODBIAS  0x00002000L
#define D3DPRASTERCAPS_WBUFFER        0x00040000L
#define D3DPRASTERCAPS_WFOG           0x00100000L
#define D3DPRASTERCAPS_ZFOG           0x00200000L
#define D3DPRASTERCAPS_COLORPERSPECTIVE 0x00400000L

typedef struct _D3DCAPS8 {
    D3DDEVTYPE DeviceType;
    UINT       AdapterOrdinal;
    DWORD      Caps, Caps2, Caps3;
    DWORD      PresentationIntervals;
    DWORD      CursorCaps, DevCaps, PrimitiveMiscCaps, RasterCaps;
    DWORD      ZCmpCaps, SrcBlendCaps, DestBlendCaps, AlphaCmpCaps;
    DWORD      ShadeCaps, TextureCaps, TextureFilterCaps;
    DWORD      CubeTextureFilterCaps, VolumeTextureFilterCaps;
    DWORD      TextureAddressCaps, VolumeTextureAddressCaps, LineCaps;
    DWORD      MaxTextureWidth, MaxTextureHeight, MaxVolumeExtent;
    DWORD      MaxTextureRepeat, MaxTextureAspectRatio, MaxAnisotropy;
    float      MaxVertexW;
    float      GuardBandLeft, GuardBandTop, GuardBandRight, GuardBandBottom;
    float      ExtentsAdjust;
    DWORD      StencilCaps, FVFCaps, TextureOpCaps;
    DWORD      MaxTextureBlendStages, MaxSimultaneousTextures;
    DWORD      VertexProcessingCaps, MaxActiveLights, MaxUserClipPlanes;
    DWORD      MaxVertexBlendMatrices, MaxVertexBlendMatrixIndex;
    float      MaxPointSize;
    DWORD      MaxPrimitiveCount, MaxVertexIndex, MaxStreams, MaxStreamStride;
    DWORD      VertexShaderVersion, MaxVertexShaderConst;
    DWORD      PixelShaderVersion;
    float      MaxPixelShaderValue;
} D3DCAPS8;

#endif // D3D8CAPS_H_COMPAT
