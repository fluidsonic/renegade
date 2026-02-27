#include "global.h"

// D3DX software implementations for D3DXLoadSurfaceFromSurface,
// D3DXFilterTexture, and D3DXCreateTextureFromFileExA.
//
// These replace the E_NOTIMPL stubs in D3dx8core.h.

#include <D3dx8core.h>
#include "Code/ww3d2/bitmaphandler.h"
#include "Code/ww3d2/formconv.h"
#include "Code/ww3d2/ddsfile.h"
#include "Code/ww3d2/ww3dformat.h"
#include "Code/wwlib/TARGA.H"

#include <cstring>

// ============================================================================
// D3DXLoadSurfaceFromSurface
//
// Copies pixel data from pSrcSurface to pDestSurface with format conversion
// and optional box-filter downsampling when sizes differ.
//
// pDestPalette and pSrcPalette are always NULL at all call sites.
// ColorKey is always 0 at all call sites.
// ============================================================================

HRESULT D3DXLoadSurfaceFromSurface(
    IDirect3DSurface8* pDestSurface, const void* /*pDestPalette*/, const RECT* pDestRect,
    IDirect3DSurface8* pSrcSurface,  const void* /*pSrcPalette*/,  const RECT* pSrcRect,
    DWORD /*Filter*/, DWORD /*ColorKey*/)
{
    if (!pDestSurface || !pSrcSurface) return E_POINTER;

    // Get surface descriptors
    D3DSURFACE_DESC srcDesc, dstDesc;
    pSrcSurface->GetDesc(&srcDesc);
    pDestSurface->GetDesc(&dstDesc);

    // Determine src/dst rectangles
    RECT srcR, dstR;
    if (pSrcRect) {
        srcR = *pSrcRect;
    } else {
        srcR.left = 0; srcR.top = 0;
        srcR.right  = static_cast<int32_t>(srcDesc.Width);
        srcR.bottom = static_cast<int32_t>(srcDesc.Height);
    }
    if (pDestRect) {
        dstR = *pDestRect;
    } else {
        dstR.left = 0; dstR.top = 0;
        dstR.right  = static_cast<int32_t>(dstDesc.Width);
        dstR.bottom = static_cast<int32_t>(dstDesc.Height);
    }

    uint32_t srcW = static_cast<uint32_t>(srcR.right  - srcR.left);
    uint32_t srcH = static_cast<uint32_t>(srcR.bottom - srcR.top);
    uint32_t dstW = static_cast<uint32_t>(dstR.right  - dstR.left);
    uint32_t dstH = static_cast<uint32_t>(dstR.bottom - dstR.top);

    // Lock both surfaces
    D3DLOCKED_RECT srcLock, dstLock;
    HRESULT hr = pSrcSurface->LockRect(&srcLock, nullptr, D3DLOCK_READONLY);
    if (FAILED(hr)) return hr;
    hr = pDestSurface->LockRect(&dstLock, nullptr, 0);
    if (FAILED(hr)) { pSrcSurface->UnlockRect(); return hr; }

    WW3DFormat srcFmt = D3DFormat_To_WW3DFormat(srcDesc.Format);
    WW3DFormat dstFmt = D3DFormat_To_WW3DFormat(dstDesc.Format);

    // Pointer to the origin of the src/dst regions inside the locked buffers
    uint32_t srcBpp = Get_Bytes_Per_Pixel(srcFmt);
    uint32_t dstBpp = Get_Bytes_Per_Pixel(dstFmt);

    uint8_t* srcBase = static_cast<uint8_t*>(srcLock.pBits)
                       + static_cast<uint32_t>(srcR.top)  * static_cast<uint32_t>(srcLock.Pitch)
                       + static_cast<uint32_t>(srcR.left) * srcBpp;
    uint8_t* dstBase = static_cast<uint8_t*>(dstLock.pBits)
                       + static_cast<uint32_t>(dstR.top)  * static_cast<uint32_t>(dstLock.Pitch)
                       + static_cast<uint32_t>(dstR.left) * dstBpp;

    BitmapHandlerClass::Copy_Image(
        dstBase,
        dstW, dstH,
        static_cast<uint32_t>(dstLock.Pitch),
        dstFmt,
        srcBase,
        srcW, srcH,
        static_cast<uint32_t>(srcLock.Pitch),
        srcFmt,
        nullptr,  // no palette
        0,        // palette bpp
        false);   // don't generate mip

    pDestSurface->UnlockRect();
    pSrcSurface->UnlockRect();
    return S_OK;
}

// ============================================================================
// D3DXFilterTexture
//
// Generates all mip levels for a texture using box filtering.
// SrcLevel is always 0 at the call site.
// ============================================================================

HRESULT D3DXFilterTexture(IDirect3DBaseTexture8* pTexture, const void* /*pPalette*/,
    UINT SrcLevel, DWORD /*Filter*/)
{
    if (!pTexture) return E_POINTER;

    // We only handle IDirect3DTexture8 (2D textures)
    IDirect3DTexture8* tex = static_cast<IDirect3DTexture8*>(pTexture);

    uint32_t levelCount = tex->GetLevelCount();
    if (levelCount <= 1) return S_OK;

    // Propagate each level from the previous one using D3DXLoadSurfaceFromSurface
    // which already performs box-filter downsampling via BitmapHandlerClass::Copy_Image.
    for (uint32_t i = SrcLevel + 1; i < levelCount; ++i) {
        IDirect3DSurface8* src = nullptr;
        IDirect3DSurface8* dst = nullptr;
        HRESULT hr = tex->GetSurfaceLevel(i - 1, &src);
        if (FAILED(hr)) return hr;
        hr = tex->GetSurfaceLevel(i, &dst);
        if (FAILED(hr)) { src->Release(); return hr; }

        hr = D3DXLoadSurfaceFromSurface(
            dst, nullptr, nullptr,
            src, nullptr, nullptr,
            D3DX_FILTER_BOX, 0);

        src->Release();
        dst->Release();

        if (FAILED(hr)) return hr;
    }
    return S_OK;
}

// ============================================================================
// D3DXCreateTextureFromFileExA
//
// Loads a texture from a file on disk.  Supports .dds and .tga.
// D3DX_DEFAULT for Width/Height means use the image dimensions.
// mip_level_count == D3DX_DEFAULT means auto-generate all mip levels.
// ============================================================================

HRESULT D3DXCreateTextureFromFileExA(
    void* pDeviceVoid, const char* pSrcFile,
    UINT Width, UINT Height, UINT MipLevels, DWORD /*Usage*/,
    D3DFORMAT Format, D3DPOOL /*Pool*/,
    DWORD /*Filter*/, DWORD /*MipFilter*/, DWORD /*ColorKey*/,
    D3DXIMAGE_INFO* pSrcInfo, void* /*pPalette*/,
    IDirect3DTexture8** ppTexture)
{
    if (!ppTexture) return E_POINTER;
    *ppTexture = nullptr;
    if (!pDeviceVoid || !pSrcFile) return E_POINTER;

    IDirect3DDevice8* pDevice = static_cast<IDirect3DDevice8*>(pDeviceVoid);

    // Determine extension to pick loader
    const char* dot = strrchr(pSrcFile, '.');
    bool isDDS = dot && (strcasecmp(dot, ".dds") == 0);
    bool isTGA = dot && (strcasecmp(dot, ".tga") == 0);

    if (!isDDS && !isTGA) {
        // Unknown format
        return D3DERR_INVALIDCALL;
    }

    if (isDDS) {
        // ---- DDS path ----
        DDSFileClass dds(pSrcFile, 0);
        if (!dds.Load()) return D3DERR_INVALIDCALL;

        uint32_t imgW = (Width  == D3DX_DEFAULT) ? dds.Get_Width(0)  : Width;
        uint32_t imgH = (Height == D3DX_DEFAULT) ? dds.Get_Height(0) : Height;

        // Decide output format
        WW3DFormat ww3dFmt = dds.Get_Format();
        D3DFORMAT d3dFmt   = (Format == D3DFMT_UNKNOWN)
                               ? WW3DFormat_To_D3DFormat(ww3dFmt)
                               : Format;
        if (d3dFmt == D3DFMT_UNKNOWN) d3dFmt = D3DFMT_A8R8G8B8;

        uint32_t mipCount = (MipLevels == D3DX_DEFAULT) ? dds.Get_Mip_Level_Count()
                                                         : MipLevels;
        if (mipCount == 0) mipCount = dds.Get_Mip_Level_Count();

        IDirect3DTexture8* tex = nullptr;
        HRESULT hr = pDevice->CreateTexture(imgW, imgH, mipCount, 0, d3dFmt, D3DPOOL_MANAGED, &tex);
        if (FAILED(hr)) return hr;

        // Copy each mip level
        uint32_t actualLevels = tex->GetLevelCount();
        for (uint32_t lvl = 0; lvl < actualLevels; ++lvl) {
            IDirect3DSurface8* surf = nullptr;
            hr = tex->GetSurfaceLevel(lvl, &surf);
            if (FAILED(hr)) { tex->Release(); return hr; }
            dds.Copy_Level_To_Surface(lvl, surf);
            surf->Release();
        }

        if (pSrcInfo) {
            pSrcInfo->Width       = imgW;
            pSrcInfo->Height      = imgH;
            pSrcInfo->Depth       = 1;
            pSrcInfo->MipLevels   = actualLevels;
            pSrcInfo->Format      = d3dFmt;
            pSrcInfo->ResourceType= D3DRTYPE_TEXTURE;
            pSrcInfo->ImageFileFormat = 0;
        }

        *ppTexture = tex;
        return S_OK;
    }

    // ---- TGA path ----
    Targa targa;
    if (targa.Open(pSrcFile, TGA_READMODE) != 0) return D3DERR_INVALIDCALL;

    // DX8 uses image upside down compared to TGA
    targa.Header.ImageDescriptor ^= TGAIDF_YORIGIN;

    WW3DFormat srcFmt, dstFmt;
    uint32_t   srcBpp = 0;
    Get_WW3D_Format(dstFmt, srcFmt, srcBpp, targa);

    uint32_t imgW = (Width  == D3DX_DEFAULT) ? static_cast<uint32_t>(targa.Header.Width)
                                              : Width;
    uint32_t imgH = (Height == D3DX_DEFAULT) ? static_cast<uint32_t>(targa.Header.Height)
                                              : Height;
    uint32_t srcW = static_cast<uint32_t>(targa.Header.Width);
    uint32_t srcH = static_cast<uint32_t>(targa.Header.Height);

    // Set up palette buffer
    char palette[256 * 4];
    memset(palette, 0, sizeof(palette));
    targa.SetPalette(palette);

    if (targa.Load(pSrcFile, TGAF_IMAGE, false) != 0) return D3DERR_INVALIDCALL;

    uint8_t* srcPixels = reinterpret_cast<uint8_t*>(targa.GetImage());

    // If format needs conversion (e.g. palette, 16-bit) or size differs,
    // pre-convert to A8R8G8B8 so Copy_Image handles any downscaling.
    uint8_t* convBuf = nullptr;
    if (srcFmt == WW3D_FORMAT_P8 || srcFmt == WW3D_FORMAT_A1R5G5B5 ||
        srcFmt == WW3D_FORMAT_R5G6B5 || srcFmt == WW3D_FORMAT_A4R4G4B4 ||
        srcFmt == WW3D_FORMAT_L8 || srcW != imgW || srcH != imgH)
    {
        convBuf = new uint8_t[imgW * imgH * 4];
        BitmapHandlerClass::Copy_Image(
            convBuf,
            imgW, imgH, imgW * 4,
            WW3D_FORMAT_A8R8G8B8,
            srcPixels,
            srcW, srcH, srcW * srcBpp,
            srcFmt,
            reinterpret_cast<const uint8_t*>(targa.GetPalette()),
            static_cast<uint32_t>(targa.Header.CMapDepth) >> 3,
            false);
        srcPixels = convBuf;
        srcFmt    = WW3D_FORMAT_A8R8G8B8;
        srcBpp    = 4;
        srcW      = imgW;
        srcH      = imgH;
        dstFmt    = WW3D_FORMAT_A8R8G8B8;
    }

    // Choose D3D format for the texture
    D3DFORMAT d3dFmt = (Format == D3DFMT_UNKNOWN)
                       ? WW3DFormat_To_D3DFormat(dstFmt)
                       : Format;
    if (d3dFmt == D3DFMT_UNKNOWN) d3dFmt = D3DFMT_A8R8G8B8;

    uint32_t mipCount = (MipLevels == D3DX_DEFAULT) ? 0 : MipLevels;

    IDirect3DTexture8* tex = nullptr;
    HRESULT hr = pDevice->CreateTexture(imgW, imgH, mipCount, 0, d3dFmt, D3DPOOL_MANAGED, &tex);
    if (FAILED(hr)) { delete[] convBuf; return hr; }

    // Copy base level (level 0)
    IDirect3DSurface8* surf = nullptr;
    hr = tex->GetSurfaceLevel(0, &surf);
    if (FAILED(hr)) { tex->Release(); delete[] convBuf; return hr; }

    D3DLOCKED_RECT lr;
    hr = surf->LockRect(&lr, nullptr, 0);
    if (FAILED(hr)) { surf->Release(); tex->Release(); delete[] convBuf; return hr; }

    BitmapHandlerClass::Copy_Image(
        static_cast<uint8_t*>(lr.pBits),
        imgW, imgH,
        static_cast<uint32_t>(lr.Pitch),
        D3DFormat_To_WW3DFormat(d3dFmt),
        srcPixels,
        srcW, srcH,
        srcW * srcBpp,
        srcFmt,
        reinterpret_cast<const uint8_t*>(targa.GetPalette()),
        static_cast<uint32_t>(targa.Header.CMapDepth) >> 3,
        false);

    surf->UnlockRect();
    surf->Release();

    delete[] convBuf;

    // Generate mip levels if requested (mipCount == 0 means full chain)
    if (mipCount != 1) {
        D3DXFilterTexture(tex, nullptr, 0, D3DX_FILTER_BOX);
    }

    if (pSrcInfo) {
        pSrcInfo->Width       = imgW;
        pSrcInfo->Height      = imgH;
        pSrcInfo->Depth       = 1;
        pSrcInfo->MipLevels   = tex->GetLevelCount();
        pSrcInfo->Format      = d3dFmt;
        pSrcInfo->ResourceType= D3DRTYPE_TEXTURE;
        pSrcInfo->ImageFileFormat = 0;
    }

    *ppTexture = tex;
    return S_OK;
}
