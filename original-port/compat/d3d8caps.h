// d3d8caps.h compat shim for macOS
#pragma once
#ifndef D3D8CAPS_H_COMPAT
#define D3D8CAPS_H_COMPAT

#include "d3d8types.h"
#include "winnt.h"

// D3DADAPTER_IDENTIFIER8 - adapter info struct
typedef struct _D3DADAPTER_IDENTIFIER8 {
    char          Driver[512];
    char          Description[512];
    LARGE_INTEGER DriverVersion;
    DWORD         VendorId;
    DWORD         DeviceId;
    DWORD         SubSysId;
    DWORD         Revision;
    GUID          DeviceIdentifier;
    DWORD         WHQLLevel;
} D3DADAPTER_IDENTIFIER8;

#define D3DDEVCAPS_HWRASTERIZATION            0x00080000L
#define D3DDEVCAPS_HWTRANSFORMANDLIGHT        0x00010000L
#define D3DDEVCAPS_PUREDEVICE                 0x00100000L
#define D3DDEVCAPS_TLVERTEXVIDEOMEMORY        0x00000080L
#define D3DDEVCAPS_EXECUTESYSTEMMEMORY        0x00000010L
#define D3DDEVCAPS_NPATCHES                   0x01000000L

#define D3DCAPS2_FULLSCREENGAMMA              0x00020000L

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
#define D3DPRASTERCAPS_ZBIAS          0x00004000L
#define D3DPRASTERCAPS_FOGRANGE       0x00010000L

#define D3DPTFILTERCAPS_NEAREST          0x00000100L
#define D3DPTFILTERCAPS_LINEAR           0x00000200L
#define D3DPTFILTERCAPS_MINFPOINT        0x00000100L
#define D3DPTFILTERCAPS_MINFLINEAR       0x00000200L
#define D3DPTFILTERCAPS_MINFANISOTROPIC  0x00000400L
#define D3DPTFILTERCAPS_MAGFPOINT        0x01000000L
#define D3DPTFILTERCAPS_MAGFLINEAR       0x02000000L
#define D3DPTFILTERCAPS_MAGFANISOTROPIC  0x04000000L
#define D3DPTFILTERCAPS_MIPFPOINT        0x00010000L
#define D3DPTFILTERCAPS_MIPFLINEAR       0x00020000L

#define D3DTEXOPCAPS_DISABLE              0x00000001L
#define D3DTEXOPCAPS_SELECTARG1           0x00000002L
#define D3DTEXOPCAPS_SELECTARG2           0x00000004L
#define D3DTEXOPCAPS_MODULATE             0x00000008L
#define D3DTEXOPCAPS_MODULATE2X           0x00000010L
#define D3DTEXOPCAPS_MODULATE4X           0x00000020L
#define D3DTEXOPCAPS_ADD                  0x00000040L
#define D3DTEXOPCAPS_ADDSIGNED            0x00000080L
#define D3DTEXOPCAPS_ADDSIGNED2X          0x00000100L
#define D3DTEXOPCAPS_SUBTRACT             0x00000200L
#define D3DTEXOPCAPS_ADDSMOOTH            0x00000400L
#define D3DTEXOPCAPS_BLENDDIFFUSEALPHA    0x00000800L
#define D3DTEXOPCAPS_BLENDTEXTUREALPHA    0x00001000L
#define D3DTEXOPCAPS_BLENDFACTORALPHA     0x00002000L
#define D3DTEXOPCAPS_BLENDTEXTUREALPHAPM  0x00004000L
#define D3DTEXOPCAPS_BLENDCURRENTALPHA    0x00008000L
#define D3DTEXOPCAPS_PREMODULATE          0x00010000L
#define D3DTEXOPCAPS_MODULATEALPHA_ADDCOLOR 0x00020000L
#define D3DTEXOPCAPS_MODULATECOLOR_ADDALPHA 0x00040000L
#define D3DTEXOPCAPS_MODULATEINVALPHA_ADDCOLOR 0x00080000L
#define D3DTEXOPCAPS_MODULATEINVCOLOR_ADDALPHA 0x00100000L
#define D3DTEXOPCAPS_BUMPENVMAP           0x00200000L
#define D3DTEXOPCAPS_BUMPENVMAPLUMINANCE  0x00400000L
#define D3DTEXOPCAPS_DOTPRODUCT3          0x00800000L
#define D3DTEXOPCAPS_MULTIPLYADD          0x01000000L
#define D3DTEXOPCAPS_LERP                 0x02000000L

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
