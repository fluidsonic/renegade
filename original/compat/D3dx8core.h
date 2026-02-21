// D3dx8core.h compat shim for macOS - D3DX8 core utility stubs
#pragma once
#ifndef D3DX8CORE_H_COMPAT
#define D3DX8CORE_H_COMPAT

#include "d3d8.h"

// D3DXGetFVFVertexSize - compute stride in bytes from FVF flags
inline UINT D3DXGetFVFVertexSize(DWORD dwFVF) {
    UINT size = 0;
    // Position components
    if      ((dwFVF & D3DFVF_XYZRHW) == D3DFVF_XYZRHW)  size += 4*sizeof(float); // xyzw
    else if ((dwFVF & D3DFVF_XYZB5)  == D3DFVF_XYZB5)   size += 8*sizeof(float); // xyz + 5 blend
    else if ((dwFVF & D3DFVF_XYZB4)  == D3DFVF_XYZB4)   size += 7*sizeof(float); // xyz + 4 blend
    else if ((dwFVF & D3DFVF_XYZB3)  == D3DFVF_XYZB3)   size += 6*sizeof(float); // xyz + 3 blend
    else if ((dwFVF & D3DFVF_XYZB2)  == D3DFVF_XYZB2)   size += 5*sizeof(float); // xyz + 2 blend
    else if ((dwFVF & D3DFVF_XYZB1)  == D3DFVF_XYZB1)   size += 4*sizeof(float); // xyz + 1 blend
    else if ((dwFVF & D3DFVF_XYZ)    == D3DFVF_XYZ)     size += 3*sizeof(float); // xyz
    // Normal
    if (dwFVF & D3DFVF_NORMAL)   size += 3*sizeof(float);
    // Point size
    if (dwFVF & D3DFVF_PSIZE)    size += sizeof(float);
    // Diffuse / specular color
    if (dwFVF & D3DFVF_DIFFUSE)  size += sizeof(DWORD);
    if (dwFVF & D3DFVF_SPECULAR) size += sizeof(DWORD);
    // Texture coordinates (each is 2 floats by default)
    DWORD tex_count = (dwFVF >> 8) & 0xf;
    size += tex_count * 2 * sizeof(float);
    return size;
}

// D3DXGetErrorStringA - stub: fills buffer with empty string, returns S_OK
inline HRESULT D3DXGetErrorStringA(HRESULT hr, char* pBuffer, UINT BufferLen) {
    if (pBuffer && BufferLen > 0) pBuffer[0] = '\0';
    return S_OK;
}

// D3DX default value (used to let D3DX choose a value)
#define D3DX_DEFAULT     ((UINT)0xFFFFFFFF)
#define D3DX_DEFAULT_NONPOW2  ((UINT)0xFFFFFFFE)

// D3DX filter flags
#define D3DX_FILTER_NONE    0x00000001
#define D3DX_FILTER_POINT   0x00000002
#define D3DX_FILTER_LINEAR  0x00000003
#define D3DX_FILTER_TRIANGLE 0x00000004
#define D3DX_FILTER_BOX     0x00000005
#define D3DX_FILTER_MIRROR  0x00010000

// D3DXIMAGE_INFO stub
typedef struct _D3DXIMAGE_INFO {
    UINT  Width, Height, Depth, MipLevels;
    D3DFORMAT Format;
    D3DRESOURCETYPE ResourceType;
    DWORD ImageFileFormat;
} D3DXIMAGE_INFO;

// D3DXCreateTexture — delegates to IDirect3DDevice8::CreateTexture (real device backed by GL)
inline HRESULT D3DXCreateTexture(
    IDirect3DDevice8* pDevice, UINT Width, UINT Height, UINT MipLevels,
    DWORD Usage, D3DFORMAT Format, D3DPOOL Pool,
    IDirect3DTexture8** ppTexture)
{
    if (!pDevice || !ppTexture) return E_POINTER;
    return pDevice->CreateTexture(Width, Height, MipLevels, Usage, Format, Pool, ppTexture);
}

// D3DXCreateTextureFromFileExA stub
inline HRESULT D3DXCreateTextureFromFileExA(
    void* pDevice, const char* pSrcFile,
    UINT Width, UINT Height, UINT MipLevels, DWORD Usage,
    D3DFORMAT Format, D3DPOOL Pool,
    DWORD Filter, DWORD MipFilter, DWORD ColorKey,
    D3DXIMAGE_INFO* pSrcInfo, void* pPalette,
    IDirect3DTexture8** ppTexture)
{
    if (ppTexture) *ppTexture = NULL;
    return E_NOTIMPL;
}

// D3DXCreateCubeTextureFromFileExA stub
inline HRESULT D3DXCreateCubeTextureFromFileExA(
    void* pDevice, const char* pSrcFile,
    UINT Size, UINT MipLevels, DWORD Usage,
    D3DFORMAT Format, D3DPOOL Pool,
    DWORD Filter, DWORD MipFilter, DWORD ColorKey,
    D3DXIMAGE_INFO* pSrcInfo, void* pPalette,
    void** ppCubeTexture)
{
    if (ppCubeTexture) *ppCubeTexture = NULL;
    return E_NOTIMPL;
}

// D3DXLoadSurfaceFromSurface stub
inline HRESULT D3DXLoadSurfaceFromSurface(
    IDirect3DSurface8* pDestSurface, const void* pDestPalette, const RECT* pDestRect,
    IDirect3DSurface8* pSrcSurface, const void* pSrcPalette, const RECT* pSrcRect,
    DWORD Filter, DWORD ColorKey)
{
    return E_NOTIMPL;
}

// D3DXFilterTexture stub
inline HRESULT D3DXFilterTexture(IDirect3DBaseTexture8* pTexture, const void* pPalette,
    UINT SrcLevel, DWORD Filter)
{
    return E_NOTIMPL;
}

#endif // D3DX8CORE_H_COMPAT
