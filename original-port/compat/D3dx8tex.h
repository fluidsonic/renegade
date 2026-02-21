// D3dx8tex.h compat shim for macOS - D3DX8 texture utility stubs
#pragma once
#ifndef D3DX8TEX_H_COMPAT
#define D3DX8TEX_H_COMPAT

#include "D3dx8core.h"

// D3DX texture file formats
#define D3DXIFF_BMP   0
#define D3DXIFF_JPG   1
#define D3DXIFF_TGA   2
#define D3DXIFF_PNG   3
#define D3DXIFF_DDS   4
#define D3DXIFF_PPM   5
#define D3DXIFF_DIB   6
#define D3DXIFF_HDR   7
#define D3DXIFF_PFM   8

// D3DXGetImageInfoFromFile stub
inline HRESULT D3DXGetImageInfoFromFileA(const char* pSrcFile, D3DXIMAGE_INFO* pSrcInfo) {
    if (pSrcInfo) memset(pSrcInfo, 0, sizeof(D3DXIMAGE_INFO));
    return E_NOTIMPL;
}
#define D3DXGetImageInfoFromFile D3DXGetImageInfoFromFileA

// D3DXCreateTextureFromFile stub
inline HRESULT D3DXCreateTextureFromFileA(void* pDevice, const char* pSrcFile, IDirect3DTexture8** ppTexture) {
    if (ppTexture) *ppTexture = NULL;
    return E_NOTIMPL;
}
#define D3DXCreateTextureFromFile D3DXCreateTextureFromFileA

// D3DXCreateVolumeTextureFromFile stub
inline HRESULT D3DXCreateVolumeTextureFromFileA(void* pDevice, const char* pSrcFile, void** ppVolumeTexture) {
    if (ppVolumeTexture) *ppVolumeTexture = NULL;
    return E_NOTIMPL;
}

// D3DXSaveTextureToFile stub
inline HRESULT D3DXSaveTextureToFileA(const char* pDestFile, DWORD DestFormat,
    IDirect3DBaseTexture8* pSrcTexture, const void* pSrcPalette) {
    return E_NOTIMPL;
}
#define D3DXSaveTextureToFile D3DXSaveTextureToFileA

#endif // D3DX8TEX_H_COMPAT
