// d3d8_gl.cpp — D3D8 → OpenGL 2.1 translation layer (Phase A)
// Implements IDirect3D8 and IDirect3DDevice8 backed by SDL2+OpenGL.
// Phase A: enough to pass WW3D::Init() and run the game loop with glClear.

#define SDL_MAIN_HANDLED
#include "global.h"
#include <SDL2/SDL.h>
#include <OpenGL/gl.h>
#include <OpenGL/glu.h>
#include <mutex>
#include <stdio.h>
#include <string.h>
#include <vector>

#include "d3d8.h"
#include "d3d8caps.h"
#include "d3d8types.h"
#include "sdl2_platform.h"

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------
static inline float ColorComponent(D3DCOLOR c, int shift) {
    return ((c >> shift) & 0xFF) / 255.0f;
}

// ---------------------------------------------------------------------------
// Deferred GL texture deletion queue
// Background threads (e.g. audio) may destroy textures without a GL context.
// Queue deletions here and drain them on the render thread in Present().
// ---------------------------------------------------------------------------
static std::vector<GLuint> g_pendingTexDeletes;
static std::mutex           g_texDeleteMutex;

// ---------------------------------------------------------------------------
// Stub resource objects (Phase A: just enough to not crash)
// ---------------------------------------------------------------------------

// Forward declaration so D3D8Surface_GL::UnlockRect can notify the parent texture
struct D3D8Texture_GL;

struct D3D8Surface_GL : public IDirect3DSurface8 {
    uint32_t           refCount      = 1;
    UINT            width         = 1;
    UINT            height        = 1;
    D3DFORMAT       fmt           = D3DFMT_A8R8G8B8;
    BYTE*           pixels        = NULL;
    bool            ownsPixels    = true;
    D3D8Texture_GL* parentTexture = nullptr;  // non-null if this is a texture surface level
    UINT            mipLevel      = 0;

    static UINT bytes_per_pixel(D3DFORMAT f) {
        switch (f) {
        case D3DFMT_A4R4G4B4:
        case D3DFMT_A1R5G5B5:
        case D3DFMT_X1R5G5B5:
        case D3DFMT_R5G6B5:
        case D3DFMT_A8L8:  return 2;
        case D3DFMT_L8:
        case D3DFMT_A8:
        case D3DFMT_P8:    return 1;
        default:           return 4;
        }
    }

    static bool is_dxt(D3DFORMAT f) {
        return f == D3DFMT_DXT1 || f == D3DFMT_DXT2 || f == D3DFMT_DXT3
            || f == D3DFMT_DXT4 || f == D3DFMT_DXT5;
    }
    // DXT1 = 8 bytes/block; DXT2/3/4/5 = 16 bytes/block
    static UINT dxt_bytes_per_block(D3DFORMAT f) {
        return (f == D3DFMT_DXT1) ? 8u : 16u;
    }
    // Total bytes to store a compressed surface (block-aligned)
    static UINT total_surface_bytes(UINT w, UINT h, D3DFORMAT f) {
        if (is_dxt(f)) {
            UINT bw = (w + 3) / 4; if (bw < 1) bw = 1;
            UINT bh = (h + 3) / 4; if (bh < 1) bh = 1;
            return bw * bh * dxt_bytes_per_block(f);
        }
        return w * h * bytes_per_pixel(f);
    }
    // LockRect pitch: bytes per row (or per row-of-blocks for DXT)
    static UINT lock_pitch(UINT w, D3DFORMAT f) {
        if (is_dxt(f)) {
            UINT bw = (w + 3) / 4; if (bw < 1) bw = 1;
            return bw * dxt_bytes_per_block(f);
        }
        return w * bytes_per_pixel(f);
    }

    // Default: small owned placeholder (for stub RenderTarget/DepthStencil)
    D3D8Surface_GL()
        : width(1), height(1), fmt(D3DFMT_A8R8G8B8), ownsPixels(true)
    { pixels = new BYTE[4](); }

    // Owned pixel storage (CreateImageSurface, CreateRenderTarget, etc.)
    D3D8Surface_GL(UINT w, UINT h, D3DFORMAT f)
        : width(w ? w : 1), height(h ? h : 1), fmt(f), ownsPixels(true)
    { pixels = new BYTE[total_surface_bytes(width, height, f)](); }

    // Non-owned: shares pixel buffer from parent (e.g., texture surface level)
    D3D8Surface_GL(UINT w, UINT h, D3DFORMAT f, BYTE* external_pixels)
        : width(w ? w : 1), height(h ? h : 1), fmt(f),
          pixels(external_pixels), ownsPixels(false)
    {}

    ~D3D8Surface_GL() { if (ownsPixels) delete[] pixels; }

    // IUnknown
    uint32_t AddRef()  override { return ++refCount; }
    uint32_t Release() override { if (--refCount == 0) { delete this; return 0; } return refCount; }
    HRESULT QueryInterface(REFIID, void**) override { return E_NOINTERFACE; }

    HRESULT GetDesc(D3DSURFACE_DESC* desc) override {
        if (desc) {
            memset(desc, 0, sizeof(*desc));
            desc->Format = fmt;
            desc->Width  = width;
            desc->Height = height;
        }
        return S_OK;
    }
    HRESULT LockRect(D3DLOCKED_RECT* lr, const RECT* pRect, DWORD) override {
        if (lr) {
            lr->Pitch = (int)lock_pitch(width, fmt);
            if (pRect) {
                uint32_t bpp = (uint32_t)bytes_per_pixel(fmt);
                lr->pBits = (uint8_t*)pixels
                          + (uint32_t)pRect->top  * (uint32_t)lr->Pitch
                          + (uint32_t)pRect->left * bpp;
            } else {
                lr->pBits = pixels;
            }
        }
        return S_OK;
    }
    HRESULT UnlockRect() override;  // defined after D3D8Texture_GL (needs parentTexture->Upload_To_GL)
};

// ---------------------------------------------------------------------------
// DXT (S3TC) software decompressor
// Apple Silicon OpenGL has no GL_EXT_texture_compression_s3tc, so we decode
// DXT1/2/3/4/5 to RGBA8 on the CPU before uploading.
// ---------------------------------------------------------------------------
static void dxt1_decode_block(const uint8_t* src, uint8_t out[16][4], bool force_opaque)
{
    uint16_t c0   = (uint16_t)(src[0] | (src[1] << 8));
    uint16_t c1   = (uint16_t)(src[2] | (src[3] << 8));
    uint32_t bits = src[4] | ((uint32_t)src[5]<<8) | ((uint32_t)src[6]<<16) | ((uint32_t)src[7]<<24);

    // Expand RGB565 → RGB888
    uint8_t r[4], g[4], b[4], a[4] = {255,255,255,255};
    r[0] = (uint8_t)(((c0>>11)&0x1F)*255/31); g[0] = (uint8_t)(((c0>>5)&0x3F)*255/63); b[0] = (uint8_t)((c0&0x1F)*255/31);
    r[1] = (uint8_t)(((c1>>11)&0x1F)*255/31); g[1] = (uint8_t)(((c1>>5)&0x3F)*255/63); b[1] = (uint8_t)((c1&0x1F)*255/31);

    if (c0 > c1 || force_opaque) {
        r[2]=(uint8_t)((2*r[0]+r[1])/3); g[2]=(uint8_t)((2*g[0]+g[1])/3); b[2]=(uint8_t)((2*b[0]+b[1])/3);
        r[3]=(uint8_t)((r[0]+2*r[1])/3); g[3]=(uint8_t)((g[0]+2*g[1])/3); b[3]=(uint8_t)((b[0]+2*b[1])/3);
    } else {
        r[2]=(uint8_t)((r[0]+r[1])/2); g[2]=(uint8_t)((g[0]+g[1])/2); b[2]=(uint8_t)((b[0]+b[1])/2);
        r[3]=0; g[3]=0; b[3]=0; a[3]=0; // transparent black
    }
    for (int i = 0; i < 16; i++) {
        int idx = (bits >> (i*2)) & 3;
        out[i][0]=r[idx]; out[i][1]=g[idx]; out[i][2]=b[idx]; out[i][3]=a[idx];
    }
}

// Decompress a full DXT1/2/3/4/5 texture to RGBA8 (R,G,B,A byte order).
// dst must be w*h*4 bytes.
static void decompress_dxt(D3DFORMAT fmt, const uint8_t* src, uint8_t* dst, UINT w, UINT h)
{
    UINT bw = (w+3)/4; if (bw<1) bw=1;
    UINT bh = (h+3)/4; if (bh<1) bh=1;
    bool dxt1   = (fmt == D3DFMT_DXT1);
    bool interp = (fmt == D3DFMT_DXT4 || fmt == D3DFMT_DXT5);

    for (UINT by = 0; by < bh; by++) {
        for (UINT bx = 0; bx < bw; bx++) {
            uint8_t rgba[16][4];
            uint8_t alpha[16] = {};
            const uint8_t* block = src;

            if (!dxt1) {
                if (!interp) {
                    // DXT2/3: 8 bytes of explicit 4-bit alpha
                    for (int i = 0; i < 8; i++) {
                        alpha[i*2+0] = (uint8_t)((block[i] & 0x0F) * 17);
                        alpha[i*2+1] = (uint8_t)((block[i] >> 4)   * 17);
                    }
                } else {
                    // DXT4/5: 2 endpoint bytes + 6 bytes of 48 3-bit indices
                    uint8_t a0=block[0], a1=block[1], apal[8];
                    apal[0]=a0; apal[1]=a1;
                    if (a0 > a1) {
                        apal[2]=(uint8_t)((6*a0+1*a1)/7); apal[3]=(uint8_t)((5*a0+2*a1)/7);
                        apal[4]=(uint8_t)((4*a0+3*a1)/7); apal[5]=(uint8_t)((3*a0+4*a1)/7);
                        apal[6]=(uint8_t)((2*a0+5*a1)/7); apal[7]=(uint8_t)((1*a0+6*a1)/7);
                    } else {
                        apal[2]=(uint8_t)((4*a0+1*a1)/5); apal[3]=(uint8_t)((3*a0+2*a1)/5);
                        apal[4]=(uint8_t)((2*a0+3*a1)/5); apal[5]=(uint8_t)((1*a0+4*a1)/5);
                        apal[6]=0; apal[7]=255;
                    }
                    uint64_t abits=0;
                    for (int i=0;i<6;i++) abits|=((uint64_t)block[2+i]<<(i*8));
                    for (int i=0;i<16;i++) alpha[i]=apal[(abits>>(i*3))&7];
                }
                block += 8;
            }

            dxt1_decode_block(block, rgba, !dxt1);
            if (!dxt1) { for (int i=0;i<16;i++) rgba[i][3]=alpha[i]; }

            for (UINT py=0; py<4; py++) {
                for (UINT px=0; px<4; px++) {
                    UINT dx=bx*4+px, dy=by*4+py;
                    if (dx<w && dy<h) {
                        uint8_t* p = dst+(dy*w+dx)*4;
                        int i=(int)(py*4+px);
                        p[0]=rgba[i][0]; p[1]=rgba[i][1]; p[2]=rgba[i][2]; p[3]=rgba[i][3];
                    }
                }
            }
            src += dxt1 ? 8 : 16;
        }
    }
}

struct D3D8Texture_GL : public IDirect3DTexture8 {
    uint32_t     refCount  = 1;
    UINT      width     = 1;
    UINT      height    = 1;
    D3DFORMAT fmt       = D3DFMT_A8R8G8B8;
    UINT      numLevels = 1;
    BYTE*     levelPixels[16];     // CPU-side pixel store per mip level
    bool      levelHasData[16];    // true when level has been written
    GLuint    glTexId   = 0;           // GL texture object (0 = not yet uploaded)
    bool      dirty     = false;       // pixels changed since last upload

    // Compute mip level count for given base dimensions (D3D levels=0 means all)
    static UINT compute_mip_levels(UINT w, UINT h) {
        UINT m = (w > h) ? w : h;
        UINT n = 1;
        while (m > 1) { m >>= 1; n++; }
        return n;
    }
    // Width/height of mip level N
    static UINT mip_dim(UINT base, UINT level) {
        UINT d = base >> level;
        return d ? d : 1;
    }

    D3D8Texture_GL(UINT w, UINT h, D3DFORMAT f = D3DFMT_A8R8G8B8, UINT levels = 0)
        : width(w ? w : 1), height(h ? h : 1), fmt(f)
    {
        numLevels = (levels == 0) ? compute_mip_levels(width, height) : levels;
        memset(levelPixels, 0, sizeof(levelPixels));
        memset(levelHasData, 0, sizeof(levelHasData));
        for (UINT i = 0; i < numLevels && i < 16; i++) {
            UINT mw = mip_dim(width, i);
            UINT mh = mip_dim(height, i);
            levelPixels[i] = new BYTE[D3D8Surface_GL::total_surface_bytes(mw, mh, f)]();
        }
    }
    ~D3D8Texture_GL() {
        for (int32_t i = 0; i < 16; i++) delete[] levelPixels[i];
        if (glTexId) {
            std::lock_guard<std::mutex> lock(g_texDeleteMutex);
            g_pendingTexDeletes.push_back(glTexId);
        }
    }

    static void get_gl_format(D3DFORMAT f, GLenum& internal, GLenum& format, GLenum& type) {
        switch (f) {
        case D3DFMT_A8R8G8B8:
            internal = GL_RGBA8;   format = GL_BGRA_EXT; type = GL_UNSIGNED_BYTE; break;
        case D3DFMT_X8R8G8B8:
            internal = GL_RGB8;    format = GL_BGRA_EXT; type = GL_UNSIGNED_BYTE; break;
        case D3DFMT_A4R4G4B4:
            internal = GL_RGBA4;   format = GL_BGRA_EXT; type = GL_UNSIGNED_SHORT_4_4_4_4_REV; break;
        case D3DFMT_A1R5G5B5:
            internal = GL_RGB5_A1; format = GL_BGRA_EXT; type = GL_UNSIGNED_SHORT_1_5_5_5_REV; break;
        case D3DFMT_R5G6B5:
            internal = GL_RGB;     format = GL_RGB;       type = GL_UNSIGNED_SHORT_5_6_5_REV; break;
        case D3DFMT_A8:
        case D3DFMT_L8:
            internal = GL_ALPHA8;  format = GL_ALPHA;     type = GL_UNSIGNED_BYTE; break;
        default:
            internal = GL_RGBA8;   format = GL_BGRA_EXT;  type = GL_UNSIGNED_BYTE; break;
        }
    }

    void Upload_To_GL() {
        if (!glTexId) glGenTextures(1, &glTexId);
        glBindTexture(GL_TEXTURE_2D, glTexId);

        UINT uploadLevels = 0;
        for (UINT i = 0; i < numLevels && i < 16; i++) {
            if (!levelHasData[i]) break;
            uploadLevels++;
        }

        if (uploadLevels == 0) {
            dirty = false;
            return;
        }

        GLenum gl_internal = GL_RGBA8, gl_format = GL_BGRA_EXT, gl_type = GL_UNSIGNED_BYTE;

        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        for (UINT i = 0; i < uploadLevels; i++) {
            UINT mw = mip_dim(width, i);
            UINT mh = mip_dim(height, i);
            if (D3D8Surface_GL::is_dxt(fmt)) {
                uint8_t* rgba = new uint8_t[mw * mh * 4]();
                decompress_dxt(fmt, levelPixels[i], rgba, mw, mh);
                glTexImage2D(GL_TEXTURE_2D, (GLint)i, GL_RGBA8, (GLsizei)mw, (GLsizei)mh,
                             0, GL_RGBA, GL_UNSIGNED_BYTE, rgba);
                delete[] rgba;
            } else if (fmt == D3DFMT_A4R4G4B4) {
                // macOS Metal backend may mishandle GL_RGBA4 — expand to RGBA8
                uint8_t* rgba = new uint8_t[mw * mh * 4];
                const uint16_t* src = reinterpret_cast<const uint16_t*>(levelPixels[i]);
                for (uint32_t p = 0; p < mw * mh; p++) {
                    uint16_t s = src[p];
                    rgba[p * 4 + 0] = static_cast<uint8_t>(((s >>  0) & 0xF) * 17); // B
                    rgba[p * 4 + 1] = static_cast<uint8_t>(((s >>  4) & 0xF) * 17); // G
                    rgba[p * 4 + 2] = static_cast<uint8_t>(((s >>  8) & 0xF) * 17); // R
                    rgba[p * 4 + 3] = static_cast<uint8_t>(((s >> 12) & 0xF) * 17); // A
                }
                glTexImage2D(GL_TEXTURE_2D, (GLint)i, GL_RGBA8, (GLsizei)mw, (GLsizei)mh,
                             0, GL_BGRA_EXT, GL_UNSIGNED_BYTE, rgba);
                delete[] rgba;
            } else {
                get_gl_format(fmt, gl_internal, gl_format, gl_type);
                glTexImage2D(GL_TEXTURE_2D, (GLint)i, static_cast<GLint>(gl_internal), (GLsizei)mw, (GLsizei)mh,
                             0, gl_format, gl_type, levelPixels[i]);
            }
        }

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, (GLint)(uploadLevels - 1));
        GLenum minFilter = (uploadLevels > 1) ? GL_LINEAR_MIPMAP_LINEAR : GL_LINEAR;
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, static_cast<GLint>(minFilter));
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        GLenum err = glGetError();
        if (err) fprintf(stderr, "[d3d8] Upload_To_GL(%ux%u fmt=%u): GL error 0x%x\n",
                         width, height, (unsigned)fmt, err);
        dirty = false;
    }

    // IUnknown
    uint32_t AddRef()  override { return ++refCount; }
    uint32_t Release() override { if (--refCount == 0) { delete this; return 0; } return refCount; }
    HRESULT QueryInterface(REFIID, void**) override { return E_NOINTERFACE; }

    // IDirect3DResource8
    HRESULT GetDevice(IDirect3DDevice8**) override { return E_NOTIMPL; }
    HRESULT SetPrivateData(REFGUID, const void*, DWORD, DWORD) override { return S_OK; }
    HRESULT GetPrivateData(REFGUID, void*, DWORD*) override { return S_OK; }
    HRESULT FreePrivateData(REFGUID) override { return S_OK; }
    DWORD   SetPriority(DWORD) override { return 0; }
    DWORD   GetPriority() override { return 0; }
    void    PreLoad() override {}
    D3DRESOURCETYPE GetType() override { return D3DRTYPE_TEXTURE; }

    // IDirect3DBaseTexture8
    DWORD SetLOD(DWORD) override { return 0; }
    DWORD GetLOD() override { return 0; }
    DWORD GetLevelCount() override { return numLevels; }

    // IDirect3DTexture8
    HRESULT GetLevelDesc(UINT level, D3DSURFACE_DESC* d) override {
        if (d) {
            memset(d, 0, sizeof(*d));
            d->Width  = mip_dim(width,  level);
            d->Height = mip_dim(height, level);
            d->Format = fmt;
        }
        return S_OK;
    }
    HRESULT GetSurfaceLevel(UINT level, IDirect3DSurface8** pp) override {
        if (pp) {
            UINT mw = mip_dim(width, level);
            UINT mh = mip_dim(height, level);
            if (level < numLevels && level < 16 && levelPixels[level]) {
                auto* surf = new D3D8Surface_GL(mw, mh, fmt, levelPixels[level]);
                surf->parentTexture = this;
                surf->mipLevel      = level;
                *pp = surf;
            } else {
                auto* surf = new D3D8Surface_GL(mw, mh, fmt);
                *pp = surf;
            }
        }
        return S_OK;
    }
    HRESULT LockRect(UINT level, D3DLOCKED_RECT* lr, const RECT* pRect, DWORD) override {
        if (lr) {
            if (level < numLevels && level < 16 && levelPixels[level]) {
                UINT mw = mip_dim(width, level);
                lr->Pitch = (int)D3D8Surface_GL::lock_pitch(mw, fmt);
                if (pRect) {
                    uint32_t bpp = (uint32_t)D3D8Surface_GL::bytes_per_pixel(fmt);
                    lr->pBits = (uint8_t*)levelPixels[level]
                              + (uint32_t)pRect->top  * (uint32_t)lr->Pitch
                              + (uint32_t)pRect->left * bpp;
                } else {
                    lr->pBits = levelPixels[level];
                }
            } else {
                lr->Pitch = 0;
                lr->pBits = nullptr;
            }
        }
        return S_OK;
    }
    HRESULT UnlockRect(UINT level) override {
        if (level < numLevels && level < 16) {
            levelHasData[level] = true;
            dirty = true;
        }
        return S_OK;
    }
    HRESULT AddDirtyRect(const RECT*) override { dirty = true; return S_OK; }
};

HRESULT D3D8Surface_GL::UnlockRect() {
    if (parentTexture) {
        parentTexture->levelHasData[mipLevel] = true;
        parentTexture->dirty = true;
    }
    return S_OK;
}

struct D3D8VertexBuffer_GL : public IDirect3DVertexBuffer8 {
    BYTE* data = NULL;
    UINT  size = 0;

    D3D8VertexBuffer_GL(UINT sz) : size(sz) { data = new BYTE[sz]; memset(data, 0, sz); }
    ~D3D8VertexBuffer_GL() { delete[] data; }

    uint32_t AddRef()  override { return 1; }
    uint32_t Release() override { delete this; return 0; }
    HRESULT QueryInterface(REFIID, void**) override { return E_NOINTERFACE; }

    HRESULT GetDevice(IDirect3DDevice8**) override { return E_NOTIMPL; }
    HRESULT SetPrivateData(REFGUID, const void*, DWORD, DWORD) override { return S_OK; }
    HRESULT GetPrivateData(REFGUID, void*, DWORD*) override { return S_OK; }
    HRESULT FreePrivateData(REFGUID) override { return S_OK; }
    DWORD   SetPriority(DWORD) override { return 0; }
    DWORD   GetPriority() override { return 0; }
    void    PreLoad() override {}
    D3DRESOURCETYPE GetType() override { return D3DRTYPE_VERTEXBUFFER; }

    HRESULT Lock(UINT offset, UINT, BYTE** pp, DWORD) override {
        if (pp) *pp = data + offset;
        return S_OK;
    }
    HRESULT Unlock() override { return S_OK; }
    HRESULT GetDesc(void* d) override { return S_OK; }
};

struct D3D8IndexBuffer_GL : public IDirect3DIndexBuffer8 {
    BYTE* data = NULL;
    UINT  size = 0;

    D3D8IndexBuffer_GL(UINT sz) : size(sz) { data = new BYTE[sz]; memset(data, 0, sz); }
    ~D3D8IndexBuffer_GL() { delete[] data; }

    uint32_t AddRef()  override { return 1; }
    uint32_t Release() override { delete this; return 0; }
    HRESULT QueryInterface(REFIID, void**) override { return E_NOINTERFACE; }

    HRESULT GetDevice(IDirect3DDevice8**) override { return E_NOTIMPL; }
    HRESULT SetPrivateData(REFGUID, const void*, DWORD, DWORD) override { return S_OK; }
    HRESULT GetPrivateData(REFGUID, void*, DWORD*) override { return S_OK; }
    HRESULT FreePrivateData(REFGUID) override { return S_OK; }
    DWORD   SetPriority(DWORD) override { return 0; }
    DWORD   GetPriority() override { return 0; }
    void    PreLoad() override {}
    D3DRESOURCETYPE GetType() override { return D3DRTYPE_INDEXBUFFER; }

    HRESULT Lock(UINT offset, UINT, BYTE** pp, DWORD) override {
        if (pp) *pp = data + offset;
        return S_OK;
    }
    HRESULT Unlock() override { return S_OK; }
    HRESULT GetDesc(void* d) override { return S_OK; }
};


// ---------------------------------------------------------------------------
// Helpers: D3D → GL conversions
// ---------------------------------------------------------------------------

// D3DBLEND → GL blend factor
static GLenum d3dblend_to_gl(DWORD d) {
    switch (d) {
    case 1:  return GL_ZERO;
    case 2:  return GL_ONE;
    case 3:  return GL_SRC_COLOR;
    case 4:  return GL_ONE_MINUS_SRC_COLOR;
    case 5:  return GL_SRC_ALPHA;
    case 6:  return GL_ONE_MINUS_SRC_ALPHA;
    case 7:  return GL_DST_ALPHA;
    case 8:  return GL_ONE_MINUS_DST_ALPHA;
    case 9:  return GL_DST_COLOR;
    case 10: return GL_ONE_MINUS_DST_COLOR;
    case 11: return GL_SRC_ALPHA_SATURATE;
    default: return GL_ONE;
    }
}

// D3DCMPFUNC → GL comparison function
static GLenum d3dcmp_to_gl(DWORD d) {
    switch (d) {
    case 1: return GL_NEVER;    case 2: return GL_LESS;
    case 3: return GL_EQUAL;    case 4: return GL_LEQUAL;
    case 5: return GL_GREATER;  case 6: return GL_NOTEQUAL;
    case 7: return GL_GEQUAL;   case 8: return GL_ALWAYS;
    default: return GL_LEQUAL;
    }
}

// D3DSTENCILOP → GL stencil operation
static GLenum d3dstencilop_to_gl(DWORD d) {
    switch (d) {
    case 1: return GL_KEEP;     case 2: return GL_ZERO;
    case 3: return GL_REPLACE;  case 4: return GL_INCR;
    case 5: return GL_DECR;     case 6: return GL_INVERT;
    case 7: return GL_INCR_WRAP; case 8: return GL_DECR_WRAP;
    default: return GL_KEEP;
    }
}

// D3DTEXTUREADDRESS → GL wrap mode
static GLenum d3dtaddress_to_gl(DWORD a) {
    switch (a) {
    case 2: return GL_MIRRORED_REPEAT;  // D3DTADDRESS_MIRROR
    case 3: return GL_CLAMP_TO_EDGE;    // D3DTADDRESS_CLAMP
    case 4: return GL_CLAMP_TO_BORDER;  // D3DTADDRESS_BORDER
    default: return GL_REPEAT;          // D3DTADDRESS_WRAP
    }
}

// D3DPRIMITIVETYPE → GL primitive enum
static GLenum d3dprim_to_gl(D3DPRIMITIVETYPE t) {
    switch (t) {
    case D3DPT_POINTLIST:     return GL_POINTS;
    case D3DPT_LINELIST:      return GL_LINES;
    case D3DPT_LINESTRIP:     return GL_LINE_STRIP;
    case D3DPT_TRIANGLELIST:  return GL_TRIANGLES;
    case D3DPT_TRIANGLESTRIP: return GL_TRIANGLE_STRIP;
    case D3DPT_TRIANGLEFAN:   return GL_TRIANGLE_FAN;
    default:                  return GL_TRIANGLES;
    }
}

// primCount → number of vertices/indices to submit
static UINT prim_vertex_count(D3DPRIMITIVETYPE t, UINT n) {
    switch (t) {
    case D3DPT_POINTLIST:     return n;
    case D3DPT_LINELIST:      return n * 2;
    case D3DPT_LINESTRIP:     return n + 1;
    case D3DPT_TRIANGLELIST:  return n * 3;
    case D3DPT_TRIANGLESTRIP: return n + 2;
    case D3DPT_TRIANGLEFAN:   return n + 2;
    default:                  return n * 3;
    }
}

// Transpose a D3D row-major matrix to a GL column-major float[16]
// Convert a D3D matrix (row-major, row-vector convention: v' = v * M_d3d) to a
// GL column-major matrix array (column-vector convention: v' = M_gl * v).
//
// The game's DX8Wrapper already transposes its internal GL-convention Matrix4
// before calling IDirect3DDevice8::SetTransform, so the D3DMATRIX we receive
// here is M_game^T.  To recover M_game for GL we must transpose once more:
//   M_gl = (M_game^T)^T = M_game
//
// Concretely: D3DMATRIX stores elements row-major (_ij = row i, col j).
// glLoadMatrixf reads column-major (out[col*4+row]).  The correct mapping is:
//   out[col*4 + row] = m->_(col+1)(row+1)
// which is identical to a straight memcpy of the 16 floats.
static void d3d_to_gl_matrix(const D3DMATRIX* m, float out[16]) {
    out[0]  = m->_11;  out[1]  = m->_12;  out[2]  = m->_13;  out[3]  = m->_14;
    out[4]  = m->_21;  out[5]  = m->_22;  out[6]  = m->_23;  out[7]  = m->_24;
    out[8]  = m->_31;  out[9]  = m->_32;  out[10] = m->_33;  out[11] = m->_34;
    out[12] = m->_41;  out[13] = m->_42;  out[14] = m->_43;  out[15] = m->_44;
}

// ---------------------------------------------------------------------------
// Dirty flags — set by state setters, cleared by Apply_GL_State/Transforms
// ---------------------------------------------------------------------------
enum DirtyFlags : uint32_t {
    DIRTY_BLEND       = 1u << 0,   // rs[19,20,27,171]
    DIRTY_DEPTH       = 1u << 1,   // rs[7,14,23]
    DIRTY_ALPHA_TEST  = 1u << 2,   // rs[15,24,25]
    DIRTY_FILL_SHADE  = 1u << 3,   // rs[8,9]
    DIRTY_COLOR_WRITE = 1u << 4,   // rs[168]
    DIRTY_STENCIL     = 1u << 5,   // rs[52..59]
    DIRTY_FOG         = 1u << 6,   // rs[28,34..38,140]
    DIRTY_LIGHTING    = 1u << 7,   // rs[137,139,145], material
    DIRTY_TEXTURES    = 1u << 8,   // tss[][], currentTextures[], rs[60]
    DIRTY_TRANSFORMS  = 1u << 9,   // worldMatrix, viewMatrix, projMatrix
    DIRTY_LIGHTS      = 1u << 10,  // lights[], lightEnabled[]
    DIRTY_ALL         = 0xFFFFFFFFu
};

// Map a D3DRENDERSTATETYPE index to its dirty-flag group
static uint32_t rs_dirty_flag(uint32_t state) {
    switch (state) {
    case 19: case 20: case 27: case 171:                    return DIRTY_BLEND;
    case 7:  case 14: case 23:                              return DIRTY_DEPTH;
    case 15: case 24: case 25:                              return DIRTY_ALPHA_TEST;
    case 8:  case 9:                                        return DIRTY_FILL_SHADE;
    case 168:                                               return DIRTY_COLOR_WRITE;
    case 52: case 53: case 54: case 55:
    case 56: case 57: case 58: case 59:                     return DIRTY_STENCIL;
    case 28: case 34: case 35: case 36:
    case 37: case 38: case 140:                             return DIRTY_FOG;
    case 137: case 139: case 145:                           return DIRTY_LIGHTING;
    case 60:                                                return DIRTY_TEXTURES;
    default:                                                return 0;
    }
}

// ---------------------------------------------------------------------------
// IDirect3DDevice8_GL — Phase A/B implementation
// ---------------------------------------------------------------------------
struct IDirect3DDevice8_GL : public IDirect3DDevice8 {
    uint32_t   refCount = 1;
    int     width    = 800;
    int     height   = 600;

    // -----------------------------------------------------------------------
    // Render state shadow
    // -----------------------------------------------------------------------
    // D3DRENDERSTATETYPE values up to ~200 are enough for all used states
    DWORD                rs[256] = {};          // shadow of SetRenderState values
    DWORD                currentFVF = 0;

    // Texture stage state shadow — tss[stage][state_index]
    // D3DTEXTURESTAGESTATETYPE indices used: 1=COLOROP,2=COLORARG1,3=COLORARG2,
    //   4=ALPHAOP,5=ALPHAARG1,6=ALPHAARG2
    DWORD                tss[8][32] = {};

    // Bound stream source and index buffer
    D3D8VertexBuffer_GL* currentVB           = nullptr;
    UINT                 currentVBStride     = 0;
    D3D8IndexBuffer_GL*  currentIB           = nullptr;
    UINT                 currentIBBaseVertex = 0;

    // Bound textures (per stage)
    D3D8Texture_GL*      currentTextures[8]  = {};

    // Transform matrices (D3D row-major)
    D3DMATRIX worldMatrix, viewMatrix, projMatrix;
    D3DMATRIX texMatrix[8];  // per-stage texture transforms (D3DTS_TEXTURE0..7)

    // Lighting state
    D3DLIGHT8    lights[8]       = {};
    BOOL         lightEnabled[8] = {};
    D3DMATERIAL8 material        = {};

    uint32_t dirty = DIRTY_ALL;  // start fully dirty; cleared per-group by Apply_GL_*

    IDirect3DDevice8_GL() {
        // Identity matrices
        memset(&worldMatrix, 0, sizeof(worldMatrix));
        memset(&viewMatrix,  0, sizeof(viewMatrix));
        memset(&projMatrix,  0, sizeof(projMatrix));
        worldMatrix._11 = worldMatrix._22 = worldMatrix._33 = worldMatrix._44 = 1.0f;
        viewMatrix._11  = viewMatrix._22  = viewMatrix._33  = viewMatrix._44  = 1.0f;
        projMatrix._11  = projMatrix._22  = projMatrix._33  = projMatrix._44  = 1.0f;
        for (int32_t s = 0; s < 8; s++) {
            memset(&texMatrix[s], 0, sizeof(D3DMATRIX));
            texMatrix[s]._11 = texMatrix[s]._22 = texMatrix[s]._33 = texMatrix[s]._44 = 1.0f;
        }
        // D3D render state defaults
        rs[7]  = 1;  // D3DRS_ZENABLE = TRUE
        rs[14] = 1;  // D3DRS_ZWRITEENABLE = TRUE
        rs[19] = 2;  // D3DRS_SRCBLEND = D3DBLEND_ONE
        rs[20] = 1;  // D3DRS_DESTBLEND = D3DBLEND_ZERO
        rs[8]   = 3;    // D3DRS_FILLMODE = D3DFILL_SOLID
        rs[9]   = 2;    // D3DRS_SHADEMODE = D3DSHADE_GOURAUD
        rs[22]  = 3;    // D3DRS_CULLMODE = D3DCULL_CCW
        rs[23]  = 4;    // D3DRS_ZFUNC = D3DCMP_LESSEQUAL (D3D default; GL default is GL_LESS)
        rs[25]  = 8;    // D3DRS_ALPHAFUNC = D3DCMP_ALWAYS
        rs[137] = 1;    // D3DRS_LIGHTING = TRUE (D3D default)
        rs[168] = 0xF;  // D3DRS_COLORWRITEENABLE = all channels (R=1,G=2,B=4,A=8)
        rs[171] = 1;    // D3DRS_BLENDOP = D3DBLENDOP_ADD
        memset(lights, 0, sizeof(lights));
        memset(lightEnabled, 0, sizeof(lightEnabled));
        memset(&material, 0, sizeof(material));
        // D3D texture stage state defaults for stage 0
        // D3DTSS_COLOROP=1: D3DTOP_MODULATE=4
        // D3DTSS_COLORARG1=2: D3DTA_TEXTURE=2
        // D3DTSS_COLORARG2=3: D3DTA_DIFFUSE=0
        // D3DTSS_ALPHAOP=4: D3DTOP_SELECTARG1=2
        // D3DTSS_ALPHAARG1=5: D3DTA_TEXTURE=2
        // D3DTSS_ALPHAARG2=6: D3DTA_DIFFUSE=0
        tss[0][1] = 4;  // COLOROP = D3DTOP_MODULATE
        tss[0][2] = 2;  // COLORARG1 = D3DTA_TEXTURE
        tss[0][3] = 0;  // COLORARG2 = D3DTA_DIFFUSE
        tss[0][4] = 2;  // ALPHAOP = D3DTOP_SELECTARG1
        tss[0][5] = 2;  // ALPHAARG1 = D3DTA_TEXTURE
        tss[0][6] = 0;  // ALPHAARG2 = D3DTA_DIFFUSE
    }

    // Apply current render states and texture to GL before a draw call
    void Apply_GL_State() {
        // Always upload dirty texture content, even when no other state flags
        // require processing.  This catches the case where Lock/Unlock updated
        // texture pixels without changing the texture binding (e.g. Bink video).
        for (int32_t stage = 0; stage < 8; stage++) {
            D3D8Texture_GL* tex = currentTextures[stage];
            if (tex && tex->dirty) {
                glActiveTexture(static_cast<GLenum>(GL_TEXTURE0 + stage));
                tex->Upload_To_GL();
            }
        }
        glActiveTexture(GL_TEXTURE0);

        // Skip if no flags this function can actually handle are set.
        // DIRTY_TRANSFORMS and DIRTY_LIGHTS are handled by Apply_GL_Transforms(), not here.
        if (!(dirty & ~(uint32_t)(DIRTY_TRANSFORMS | DIRTY_LIGHTS))) {
            return;
        }

        // -- Alpha blending + blend op --
        if (dirty & DIRTY_BLEND) {
            if (rs[27]) {   // D3DRS_ALPHABLENDENABLE
                glEnable(GL_BLEND);
                glBlendFunc(d3dblend_to_gl(rs[19]), d3dblend_to_gl(rs[20]));
                // D3DRS_BLENDOP=171: 1=ADD, 2=SUBTRACT, 3=REVSUBTRACT
                switch (rs[171]) {
                case 2: glBlendEquation(GL_FUNC_SUBTRACT);         break;
                case 3: glBlendEquation(GL_FUNC_REVERSE_SUBTRACT); break;
                default: glBlendEquation(GL_FUNC_ADD);             break;
                }
            } else {
                glDisable(GL_BLEND);
            }
        }

        // -- Depth test + func + write --
        if (dirty & DIRTY_DEPTH) {
            if (rs[7]) {    // D3DRS_ZENABLE
                glEnable(GL_DEPTH_TEST);
                glDepthFunc(d3dcmp_to_gl(rs[23]));  // D3DRS_ZFUNC=23
            } else {
                glDisable(GL_DEPTH_TEST);
            }
            glDepthMask(rs[14] ? GL_TRUE : GL_FALSE);  // D3DRS_ZWRITEENABLE
        }

        // -- Cull mode -- DIAGNOSTIC: disable all culling to test geometry
        glDisable(GL_CULL_FACE);

        // -- Alpha test --
        if (dirty & DIRTY_ALPHA_TEST) {
            if (rs[15]) {   // D3DRS_ALPHATESTENABLE
                glEnable(GL_ALPHA_TEST);
                glAlphaFunc(d3dcmp_to_gl(rs[25] ? rs[25] : 8),        // D3DRS_ALPHAFUNC=25
                            (float)(rs[24] & 0xFF) / 255.0f);          // D3DRS_ALPHAREF=24
            } else {
                glDisable(GL_ALPHA_TEST);
            }
        }

        // -- Fill mode (D3DRS_FILLMODE=8): 1=POINT, 2=WIRE, 3=SOLID --
        // -- Shade mode (D3DRS_SHADEMODE=9): 1=FLAT, 2=GOURAUD --
        if (dirty & DIRTY_FILL_SHADE) {
            switch (rs[8]) {
            case 1: glPolygonMode(GL_FRONT_AND_BACK, GL_POINT); break;
            case 2: glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);  break;
            default: glPolygonMode(GL_FRONT_AND_BACK, GL_FILL); break;
            }
            glShadeModel((rs[9] == 1) ? GL_FLAT : GL_SMOOTH);
        }

        // -- Color write mask (D3DRS_COLORWRITEENABLE=168): bits R=1,G=2,B=4,A=8 --
        if (dirty & DIRTY_COLOR_WRITE) {
            DWORD cw = rs[168];
            glColorMask((cw & 1) ? GL_TRUE : GL_FALSE,
                        (cw & 2) ? GL_TRUE : GL_FALSE,
                        (cw & 4) ? GL_TRUE : GL_FALSE,
                        (cw & 8) ? GL_TRUE : GL_FALSE);
        }

        // -- Stencil (D3DRS_STENCILENABLE=52) --
        if (dirty & DIRTY_STENCIL) {
            if (rs[52]) {
                glEnable(GL_STENCIL_TEST);
                glStencilFunc(d3dcmp_to_gl(rs[56] ? rs[56] : 8),
                              (GLint)rs[57], rs[58]);              // FUNC, REF, MASK
                glStencilOp(d3dstencilop_to_gl(rs[53]),            // D3DRS_STENCILFAIL
                            d3dstencilop_to_gl(rs[54]),            // D3DRS_STENCILZFAIL
                            d3dstencilop_to_gl(rs[55]));           // D3DRS_STENCILPASS
                glStencilMask(rs[59]);                             // D3DRS_STENCILWRITEMASK
            } else {
                glDisable(GL_STENCIL_TEST);
            }
        }

        // -- Fog (D3DRS_FOGENABLE=28) --
        if (dirty & DIRTY_FOG) {
            if (rs[28]) {
                glEnable(GL_FOG);
                D3DCOLOR fc = (D3DCOLOR)rs[34];  // D3DRS_FOGCOLOR
                GLfloat fogC[4] = { ColorComponent(fc,16), ColorComponent(fc,8), ColorComponent(fc,0), 1.0f };
                glFogfv(GL_FOG_COLOR, fogC);
                // Float values are stored as DWORD bits in rs[]
                float fogStart   = *reinterpret_cast<const float*>(&rs[36]);
                float fogEnd     = *reinterpret_cast<const float*>(&rs[37]);
                float fogDensity = *reinterpret_cast<const float*>(&rs[38]);
                DWORD fogMode    = rs[35] ? rs[35] : rs[140];  // table fog overrides vertex fog
                switch (fogMode) {
                case 1: glFogi(GL_FOG_MODE, GL_EXP);    glFogf(GL_FOG_DENSITY, fogDensity); break;
                case 2: glFogi(GL_FOG_MODE, GL_EXP2);   glFogf(GL_FOG_DENSITY, fogDensity); break;
                default: glFogi(GL_FOG_MODE, GL_LINEAR); glFogf(GL_FOG_START, fogStart); glFogf(GL_FOG_END, fogEnd); break;
                }
            } else {
                glDisable(GL_FOG);
            }
        }

        // -- Lighting + material --
        if (dirty & DIRTY_LIGHTING) {
            if (rs[137]) {   // D3DRS_LIGHTING
                glEnable(GL_LIGHTING);
                glEnable(GL_NORMALIZE);
                D3DCOLOR ambColor = (D3DCOLOR)rs[139];  // D3DRS_AMBIENT
                GLfloat sceneAmb[4] = { ColorComponent(ambColor,16), ColorComponent(ambColor,8),
                                        ColorComponent(ambColor,0),  1.0f };
                glLightModelfv(GL_LIGHT_MODEL_AMBIENT,     sceneAmb);
                glLightModeli (GL_LIGHT_MODEL_TWO_SIDE,     GL_FALSE);
                glLightModeli (GL_LIGHT_MODEL_LOCAL_VIEWER, GL_FALSE);
                // D3DRS_DIFFUSEMATERIALSOURCE (rs[145]): 0=material.Diffuse, 1/2=vertex COLOR1/COLOR2
                // When COLOR1, GL_COLOR_MATERIAL lets vertex glColor4ub drive GL_DIFFUSE (alpha=1.0)
                // instead of material.Diffuse (which can be ~0.48), fixing EVA logo transparency.
                if (rs[145] == D3DMCS_COLOR1 || rs[145] == D3DMCS_COLOR2) {
                    glColorMaterial(GL_FRONT_AND_BACK, GL_DIFFUSE);
                    glEnable(GL_COLOR_MATERIAL);
                } else {
                    glDisable(GL_COLOR_MATERIAL);
                    GLfloat mdiff[4] = { material.Diffuse.r, material.Diffuse.g, material.Diffuse.b, material.Diffuse.a };
                    glMaterialfv(GL_FRONT_AND_BACK, GL_DIFFUSE, mdiff);
                }
                GLfloat mambi[4] = { material.Ambient.r,  material.Ambient.g,  material.Ambient.b,  material.Ambient.a  };
                GLfloat memis[4] = { material.Emissive.r, material.Emissive.g, material.Emissive.b, material.Emissive.a };
                glMaterialfv(GL_FRONT_AND_BACK, GL_AMBIENT,  mambi);
                glMaterialfv(GL_FRONT_AND_BACK, GL_EMISSION, memis);
                if (material.Power == 0.0f) {
                    GLfloat nospec[4] = {0,0,0,0};
                    glMaterialfv(GL_FRONT_AND_BACK, GL_SPECULAR,  nospec);
                    glMaterialf (GL_FRONT_AND_BACK, GL_SHININESS, 0.0f);
                } else {
                    GLfloat mspec[4] = { material.Specular.r, material.Specular.g, material.Specular.b, material.Specular.a };
                    glMaterialfv(GL_FRONT_AND_BACK, GL_SPECULAR, mspec);
                    float shine = material.Power < 128.0f ? material.Power : 128.0f;
                    glMaterialf (GL_FRONT_AND_BACK, GL_SHININESS, shine);
                }
            } else {
                glDisable(GL_LIGHTING);
                glDisable(GL_NORMALIZE);
                glDisable(GL_COLOR_MATERIAL);
            }
        }

        // -- Texture stages 0..7 --
        if (dirty & DIRTY_TEXTURES) {
        // D3DTSS indices: 1=COLOROP,2=COLORARG1,3=COLORARG2,4=ALPHAOP,5=ALPHAARG1,6=ALPHAARG2
        //   13=ADDRESSU,14=ADDRESSV,16=MAGFILTER,17=MINFILTER,18=MIPFILTER
        // D3DTOP: 1=DISABLE,2=SEL1,3=SEL2,4=MOD,5=MOD2X,6=MOD4X,7=ADD,8=ADDSIGNED,
        //         10=SUBTRACT,12=BLENDDIFFUSEALPHA,13=BLENDTEXTUREALPHA,14=BLENDFACTORALPHA
        // D3DTA:  0=DIFFUSE,1=CURRENT,2=TEXTURE,3=TFACTOR,4=SPECULAR; 0x10=COMPL,0x20=ALPHAREP

        for (int stage = 0; stage < 8; stage++) {
            glActiveTexture(static_cast<GLenum>(GL_TEXTURE0 + stage));
            DWORD colorOp = tss[stage][1];

            // Stop pipeline at DISABLE or unset stage
            if (colorOp == 1 || (stage > 0 && colorOp == 0)) {
                glDisable(GL_TEXTURE_2D);
                glDisable(GL_TEXTURE_GEN_S);
                glDisable(GL_TEXTURE_GEN_T);
                glMatrixMode(GL_TEXTURE); glLoadIdentity(); glMatrixMode(GL_MODELVIEW);
                for (int s = stage + 1; s < 8; s++) {
                    glActiveTexture(GL_TEXTURE0 + (GLenum)s);
                    glDisable(GL_TEXTURE_2D);
                    glDisable(GL_TEXTURE_GEN_S);
                    glDisable(GL_TEXTURE_GEN_T);
                    glMatrixMode(GL_TEXTURE); glLoadIdentity(); glMatrixMode(GL_MODELVIEW);
                }
                break;
            }

            // Bind texture and set addressing/filter
            D3D8Texture_GL* tex = currentTextures[stage];
            if (tex && tex->dirty) tex->Upload_To_GL();
            bool hasTex = tex && tex->glTexId;
            if (hasTex) {
                glEnable(GL_TEXTURE_2D);
                glBindTexture(GL_TEXTURE_2D, tex->glTexId);
                DWORD addrU = tss[stage][13], addrV = tss[stage][14];
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, static_cast<GLint>(d3dtaddress_to_gl(addrU ? addrU : 1)));
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, static_cast<GLint>(d3dtaddress_to_gl(addrV ? addrV : 1)));
                DWORD magF = tss[stage][16], minF = tss[stage][17], mipF = tss[stage][18];
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, (magF==1) ? GL_NEAREST : GL_LINEAR);
                GLenum minFilter;
                if      (mipF <= 1)  minFilter = (minF==1) ? GL_NEAREST              : GL_LINEAR;
                else if (mipF == 2)  minFilter = (minF==1) ? GL_NEAREST_MIPMAP_NEAREST : GL_LINEAR_MIPMAP_NEAREST;
                else                 minFilter = (minF==1) ? GL_NEAREST_MIPMAP_LINEAR  : GL_LINEAR_MIPMAP_LINEAR;
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, static_cast<GLint>(minFilter));
            } else {
                glDisable(GL_TEXTURE_2D);
            }

            glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, static_cast<GLint>(GL_COMBINE));

            // Set env color for D3DTA_TFACTOR (D3DRS_TEXTUREFACTOR=60)
            DWORD ca1=tss[stage][2], ca2=tss[stage][3], aa1=tss[stage][5], aa2=tss[stage][6];
            if ((ca1&0xF)==3||(ca2&0xF)==3||(aa1&0xF)==3||(aa2&0xF)==3) {
                GLfloat envC[4] = { ColorComponent(rs[60],16), ColorComponent(rs[60],8),
                                    ColorComponent(rs[60], 0), ColorComponent(rs[60],24) };
                glTexEnvfv(GL_TEXTURE_ENV, GL_TEXTURE_ENV_COLOR, envC);
            }

            // D3DTA → GL source (stage-aware: CURRENT=GL_PREVIOUS for stage>0)
            auto dta_src = [&](DWORD arg) -> GLenum {
                switch (arg & 0xF) {
                case 2:  return GL_TEXTURE;
                case 3:  return GL_CONSTANT;       // D3DTA_TFACTOR
                case 1:  return (stage==0) ? GL_PRIMARY_COLOR : GL_PREVIOUS;  // D3DTA_CURRENT
                case 4:  return GL_PRIMARY_COLOR;  // D3DTA_SPECULAR (approx)
                default: return GL_PRIMARY_COLOR;  // D3DTA_DIFFUSE
                }
            };
            // D3DTA operand for RGB (handles COMPLEMENT=0x10, ALPHAREPLICATE=0x20)
            auto dta_op_rgb = [](DWORD arg) -> GLenum {
                if (arg & 0x20) return (arg & 0x10) ? GL_ONE_MINUS_SRC_ALPHA : GL_SRC_ALPHA;
                return (arg & 0x10) ? GL_ONE_MINUS_SRC_COLOR : GL_SRC_COLOR;
            };
            // D3DTA operand for alpha
            auto dta_op_a = [](DWORD arg) -> GLenum {
                return (arg & 0x10) ? GL_ONE_MINUS_SRC_ALPHA : GL_SRC_ALPHA;
            };

            // Color op
            GLfloat rgbScale = 1.0f;
            switch (colorOp) {
            case 2:
                glTexEnvi(GL_TEXTURE_ENV, GL_COMBINE_RGB, static_cast<GLint>(GL_REPLACE));
                glTexEnvi(GL_TEXTURE_ENV, GL_SOURCE0_RGB, static_cast<GLint>(dta_src(ca1)));
                glTexEnvi(GL_TEXTURE_ENV, GL_OPERAND0_RGB, static_cast<GLint>(dta_op_rgb(ca1)));
                break;
            case 3:
                glTexEnvi(GL_TEXTURE_ENV, GL_COMBINE_RGB, static_cast<GLint>(GL_REPLACE));
                glTexEnvi(GL_TEXTURE_ENV, GL_SOURCE0_RGB, static_cast<GLint>(dta_src(ca2)));
                glTexEnvi(GL_TEXTURE_ENV, GL_OPERAND0_RGB, static_cast<GLint>(dta_op_rgb(ca2)));
                break;
            case 5: case 6:
                rgbScale = (colorOp == 5) ? 2.0f : 4.0f;
                // fall through
            case 4: default:
                glTexEnvi(GL_TEXTURE_ENV, GL_COMBINE_RGB, static_cast<GLint>(GL_MODULATE));
                glTexEnvi(GL_TEXTURE_ENV, GL_SOURCE0_RGB, static_cast<GLint>(dta_src(ca1)));
                glTexEnvi(GL_TEXTURE_ENV, GL_OPERAND0_RGB, static_cast<GLint>(dta_op_rgb(ca1)));
                glTexEnvi(GL_TEXTURE_ENV, GL_SOURCE1_RGB, static_cast<GLint>(dta_src(ca2)));
                glTexEnvi(GL_TEXTURE_ENV, GL_OPERAND1_RGB, static_cast<GLint>(dta_op_rgb(ca2)));
                break;
            case 7:
                glTexEnvi(GL_TEXTURE_ENV, GL_COMBINE_RGB, static_cast<GLint>(GL_ADD));
                glTexEnvi(GL_TEXTURE_ENV, GL_SOURCE0_RGB, static_cast<GLint>(dta_src(ca1)));
                glTexEnvi(GL_TEXTURE_ENV, GL_OPERAND0_RGB, static_cast<GLint>(dta_op_rgb(ca1)));
                glTexEnvi(GL_TEXTURE_ENV, GL_SOURCE1_RGB, static_cast<GLint>(dta_src(ca2)));
                glTexEnvi(GL_TEXTURE_ENV, GL_OPERAND1_RGB, static_cast<GLint>(dta_op_rgb(ca2)));
                break;
            case 8:
                glTexEnvi(GL_TEXTURE_ENV, GL_COMBINE_RGB, static_cast<GLint>(GL_ADD_SIGNED));
                glTexEnvi(GL_TEXTURE_ENV, GL_SOURCE0_RGB, static_cast<GLint>(dta_src(ca1)));
                glTexEnvi(GL_TEXTURE_ENV, GL_OPERAND0_RGB, static_cast<GLint>(dta_op_rgb(ca1)));
                glTexEnvi(GL_TEXTURE_ENV, GL_SOURCE1_RGB, static_cast<GLint>(dta_src(ca2)));
                glTexEnvi(GL_TEXTURE_ENV, GL_OPERAND1_RGB, static_cast<GLint>(dta_op_rgb(ca2)));
                break;
            case 10:
                glTexEnvi(GL_TEXTURE_ENV, GL_COMBINE_RGB, static_cast<GLint>(GL_SUBTRACT));
                glTexEnvi(GL_TEXTURE_ENV, GL_SOURCE0_RGB, static_cast<GLint>(dta_src(ca1)));
                glTexEnvi(GL_TEXTURE_ENV, GL_OPERAND0_RGB, static_cast<GLint>(dta_op_rgb(ca1)));
                glTexEnvi(GL_TEXTURE_ENV, GL_SOURCE1_RGB, static_cast<GLint>(dta_src(ca2)));
                glTexEnvi(GL_TEXTURE_ENV, GL_OPERAND1_RGB, static_cast<GLint>(dta_op_rgb(ca2)));
                break;
            case 12: case 13: case 14: {  // BLEND*ALPHA: lerp(arg2, arg1, blendSrc.a)
                glTexEnvi(GL_TEXTURE_ENV, GL_COMBINE_RGB, static_cast<GLint>(GL_INTERPOLATE));
                glTexEnvi(GL_TEXTURE_ENV, GL_SOURCE0_RGB, static_cast<GLint>(dta_src(ca1)));
                glTexEnvi(GL_TEXTURE_ENV, GL_OPERAND0_RGB, static_cast<GLint>(dta_op_rgb(ca1)));
                glTexEnvi(GL_TEXTURE_ENV, GL_SOURCE1_RGB, static_cast<GLint>(dta_src(ca2)));
                glTexEnvi(GL_TEXTURE_ENV, GL_OPERAND1_RGB, static_cast<GLint>(dta_op_rgb(ca2)));
                GLenum blendSrc = (colorOp==12) ? GL_PRIMARY_COLOR
                                : (colorOp==13) ? GL_TEXTURE : GL_CONSTANT;
                glTexEnvi(GL_TEXTURE_ENV, GL_SOURCE2_RGB, static_cast<GLint>(blendSrc));
                glTexEnvi(GL_TEXTURE_ENV, GL_OPERAND2_RGB, static_cast<GLint>(GL_SRC_ALPHA));
                break;
            }
            }
            glTexEnvf(GL_TEXTURE_ENV, GL_RGB_SCALE, rgbScale);

            // Alpha op
            DWORD alphaOp = tss[stage][4];
            GLfloat aScale = 1.0f;
            switch (alphaOp) {
            case 2:
                glTexEnvi(GL_TEXTURE_ENV, GL_COMBINE_ALPHA, static_cast<GLint>(GL_REPLACE));
                glTexEnvi(GL_TEXTURE_ENV, GL_SOURCE0_ALPHA, static_cast<GLint>(dta_src(aa1)));
                glTexEnvi(GL_TEXTURE_ENV, GL_OPERAND0_ALPHA, static_cast<GLint>(dta_op_a(aa1)));
                break;
            case 3:
                glTexEnvi(GL_TEXTURE_ENV, GL_COMBINE_ALPHA, static_cast<GLint>(GL_REPLACE));
                glTexEnvi(GL_TEXTURE_ENV, GL_SOURCE0_ALPHA, static_cast<GLint>(dta_src(aa2)));
                glTexEnvi(GL_TEXTURE_ENV, GL_OPERAND0_ALPHA, static_cast<GLint>(dta_op_a(aa2)));
                break;
            case 5: case 6:
                aScale = (alphaOp == 5) ? 2.0f : 4.0f;
                // fall through
            case 4: default:
                glTexEnvi(GL_TEXTURE_ENV, GL_COMBINE_ALPHA, static_cast<GLint>(GL_MODULATE));
                glTexEnvi(GL_TEXTURE_ENV, GL_SOURCE0_ALPHA, static_cast<GLint>(dta_src(aa1)));
                glTexEnvi(GL_TEXTURE_ENV, GL_OPERAND0_ALPHA, static_cast<GLint>(dta_op_a(aa1)));
                glTexEnvi(GL_TEXTURE_ENV, GL_SOURCE1_ALPHA, static_cast<GLint>(dta_src(aa2)));
                glTexEnvi(GL_TEXTURE_ENV, GL_OPERAND1_ALPHA, static_cast<GLint>(dta_op_a(aa2)));
                break;
            case 7:
                glTexEnvi(GL_TEXTURE_ENV, GL_COMBINE_ALPHA, static_cast<GLint>(GL_ADD));
                glTexEnvi(GL_TEXTURE_ENV, GL_SOURCE0_ALPHA, static_cast<GLint>(dta_src(aa1)));
                glTexEnvi(GL_TEXTURE_ENV, GL_OPERAND0_ALPHA, static_cast<GLint>(dta_op_a(aa1)));
                glTexEnvi(GL_TEXTURE_ENV, GL_SOURCE1_ALPHA, static_cast<GLint>(dta_src(aa2)));
                glTexEnvi(GL_TEXTURE_ENV, GL_OPERAND1_ALPHA, static_cast<GLint>(dta_op_a(aa2)));
                break;
            case 8:
                glTexEnvi(GL_TEXTURE_ENV, GL_COMBINE_ALPHA, static_cast<GLint>(GL_ADD_SIGNED));
                glTexEnvi(GL_TEXTURE_ENV, GL_SOURCE0_ALPHA, static_cast<GLint>(dta_src(aa1)));
                glTexEnvi(GL_TEXTURE_ENV, GL_OPERAND0_ALPHA, static_cast<GLint>(dta_op_a(aa1)));
                glTexEnvi(GL_TEXTURE_ENV, GL_SOURCE1_ALPHA, static_cast<GLint>(dta_src(aa2)));
                glTexEnvi(GL_TEXTURE_ENV, GL_OPERAND1_ALPHA, static_cast<GLint>(dta_op_a(aa2)));
                break;
            case 10:
                glTexEnvi(GL_TEXTURE_ENV, GL_COMBINE_ALPHA, static_cast<GLint>(GL_SUBTRACT));
                glTexEnvi(GL_TEXTURE_ENV, GL_SOURCE0_ALPHA, static_cast<GLint>(dta_src(aa1)));
                glTexEnvi(GL_TEXTURE_ENV, GL_OPERAND0_ALPHA, static_cast<GLint>(dta_op_a(aa1)));
                glTexEnvi(GL_TEXTURE_ENV, GL_SOURCE1_ALPHA, static_cast<GLint>(dta_src(aa2)));
                glTexEnvi(GL_TEXTURE_ENV, GL_OPERAND1_ALPHA, static_cast<GLint>(dta_op_a(aa2)));
                break;
            case 1:  // DISABLE — pass through previous alpha
                glTexEnvi(GL_TEXTURE_ENV, GL_COMBINE_ALPHA, static_cast<GLint>(GL_REPLACE));
                glTexEnvi(GL_TEXTURE_ENV, GL_SOURCE0_ALPHA, static_cast<GLint>(GL_PREVIOUS));
                glTexEnvi(GL_TEXTURE_ENV, GL_OPERAND0_ALPHA, static_cast<GLint>(GL_SRC_ALPHA));
                break;
            }
            glTexEnvf(GL_TEXTURE_ENV, GL_ALPHA_SCALE, aScale);

            // -- Texture coordinate generation (D3DTSS_TEXCOORDINDEX = tss[stage][11]) --
            // D3DTSS_TCI_CAMERASPACENORMAL (0x00010000): generate UV from eye-space normals.
            // This is used by ClassicEnvironmentMapperClass for environment-mapped meshes
            // that have no UV coords in their vertex buffer (e.g. IF_EVAGIZMO.IF_EVALOGO).
            {
                DWORD tci_mode = tss[stage][11] & 0xFFFF0000u;
                if (tci_mode == D3DTSS_TCI_CAMERASPACENORMAL) {
                    glEnable(GL_TEXTURE_GEN_S);
                    glEnable(GL_TEXTURE_GEN_T);
                    glTexGeni(GL_S, GL_TEXTURE_GEN_MODE, GL_NORMAL_MAP);
                    glTexGeni(GL_T, GL_TEXTURE_GEN_MODE, GL_NORMAL_MAP);
                } else {
                    glDisable(GL_TEXTURE_GEN_S);
                    glDisable(GL_TEXTURE_GEN_T);
                }
                // -- Texture transform matrix (D3DTSS_TEXTURETRANSFORMFLAGS = tss[stage][24]) --
                // When non-zero, apply the per-stage texture matrix (set via D3DTS_TEXTURE0+stage).
                // ClassicEnvironmentMapperClass sets this to scale+bias the generated normal UVs
                // from [-1,1] to [0,1]: u = Nx*0.5 + 0.5, v = Ny*0.5 + 0.5.
                glMatrixMode(GL_TEXTURE);
                if (tss[stage][24] != 0) {  // D3DTTFF_DISABLE = 0
                    float tmat[16];
                    d3d_to_gl_matrix(&texMatrix[stage], tmat);
                    glLoadMatrixf(tmat);
                } else {
                    glLoadIdentity();
                }
                glMatrixMode(GL_MODELVIEW);
            }
        }
        glActiveTexture(GL_TEXTURE0);  // restore default active unit
        }  // dirty & DIRTY_TEXTURES

        // Clear all state flags except DIRTY_TRANSFORMS and DIRTY_LIGHTS — those
        // are handled by Apply_GL_Transforms(), which runs after this function.
        // If we cleared them here, Apply_GL_Transforms() would always see dirty=0
        // and early-return, so 3D world/view/proj matrices would never be uploaded.
        dirty &= (uint32_t)(DIRTY_TRANSFORMS | DIRTY_LIGHTS);
    }

    // Apply D3D world/view/proj matrices to GL
    void Apply_GL_Transforms() {
        // Skip if neither transforms nor lights changed — but XYZRHW always
        // needs the ortho setup in case it follows a 3D draw.
        if (!(dirty & (DIRTY_TRANSFORMS | DIRTY_LIGHTS))) {
            if (!(currentFVF & D3DFVF_XYZRHW)) return;
        }

        float mat[16];

        if (currentFVF & D3DFVF_XYZRHW) {
            // Pre-transformed (screen-space) vertices: ortho projection, identity
            // modelview.  (0,0) = top-left, Y increases downward — matches D3D.
            glMatrixMode(GL_PROJECTION);
            glLoadIdentity();
            glOrtho(0.0, (double)width, (double)height, 0.0, 0.0, 1.0);
            glMatrixMode(GL_MODELVIEW);
            glLoadIdentity();
            return;  // No 3D matrices or lighting for pre-transformed verts
        }

        glMatrixMode(GL_PROJECTION);
        d3d_to_gl_matrix(&projMatrix, mat);
        glLoadMatrixf(mat);

        glMatrixMode(GL_MODELVIEW);
        d3d_to_gl_matrix(&viewMatrix, mat);
        glLoadMatrixf(mat);

        // Apply light positions/directions with the view matrix only (no world matrix),
        // so GL transforms them from world space into eye space correctly.
        if (rs[137]) {  // D3DRS_LIGHTING
            for (int li = 0; li < 8; li++) {
                if (lightEnabled[li]) {
                    const D3DLIGHT8& L = lights[li];
                    GLfloat ldiff[4] = { L.Diffuse.r,  L.Diffuse.g,  L.Diffuse.b,  L.Diffuse.a  };
                    GLfloat lspec[4] = { L.Specular.r, L.Specular.g, L.Specular.b, L.Specular.a };
                    GLfloat lambi[4] = { L.Ambient.r,  L.Ambient.g,  L.Ambient.b,  L.Ambient.a  };
                    GLenum glLight = static_cast<GLenum>(GL_LIGHT0 + li);
                    glLightfv(glLight, GL_DIFFUSE,  ldiff);
                    glLightfv(glLight, GL_SPECULAR, lspec);
                    glLightfv(glLight, GL_AMBIENT,  lambi);

                    if (L.Type == 1 /* D3DLIGHT_POINT */ || L.Type == 2 /* D3DLIGHT_SPOT */) {
                        GLfloat pos[4] = { L.Position.x, L.Position.y, L.Position.z, 1.0f };
                        glLightfv(glLight, GL_POSITION, pos);
                        glLightf (glLight, GL_CONSTANT_ATTENUATION,  L.Attenuation0);
                        glLightf (glLight, GL_LINEAR_ATTENUATION,    L.Attenuation1);
                        glLightf (glLight, GL_QUADRATIC_ATTENUATION, L.Attenuation2);
                    } else {
                        // Directional (D3DLIGHT_DIRECTIONAL=3): D3D Direction points scene→source,
                        // GL position w=0 points source→origin; negate to convert.
                        GLfloat pos[4] = { -L.Direction.x, -L.Direction.y, -L.Direction.z, 0.0f };
                        glLightfv(glLight, GL_POSITION, pos);
                    }

                    if (L.Type == 2 /* D3DLIGHT_SPOT */) {
                        GLfloat spotDir[3] = { L.Direction.x, L.Direction.y, L.Direction.z };
                        glLightfv(glLight, GL_SPOT_DIRECTION, spotDir);
                        // D3D Phi = outer cone full angle (radians) → GL half-angle in degrees
                        glLightf (glLight, GL_SPOT_CUTOFF,   L.Phi * 57.29578f * 0.5f);
                        glLightf (glLight, GL_SPOT_EXPONENT, L.Falloff);
                    } else {
                        glLightf(glLight, GL_SPOT_CUTOFF, 180.0f);
                    }
                    glEnable(glLight);
                } else {
                    glDisable(static_cast<GLenum>(GL_LIGHT0 + li));
                }
            }
        }

        // Multiply modelview by world (in GL: Modelview = View_gl * World_gl)
        float wmat[16];
        d3d_to_gl_matrix(&worldMatrix, wmat);
        glMultMatrixf(wmat);

        dirty &= ~(uint32_t)(DIRTY_TRANSFORMS | DIRTY_LIGHTS);
    }

    // IUnknown
    uint32_t AddRef()  override { return ++refCount; }
    uint32_t Release() override {
        if (--refCount == 0) { delete this; return 0; }
        return refCount;
    }
    HRESULT QueryInterface(REFIID, void**) override { return E_NOINTERFACE; }

    // -----------------------------------------------------------------------
    // Core rendering methods
    // -----------------------------------------------------------------------
    HRESULT TestCooperativeLevel() override { return S_OK; }
    UINT    GetAvailableTextureMem() override { return 256 * 1024 * 1024; }
    HRESULT ResourceManagerDiscardBytes(DWORD) override { return S_OK; }

    HRESULT GetDeviceCaps(D3DCAPS8* caps) override {
        if (!caps) return E_POINTER;
        memset(caps, 0, sizeof(*caps));
        caps->DeviceType            = D3DDEVTYPE_HAL;
        caps->AdapterOrdinal        = 0;
        caps->Caps                  = 0;
        caps->Caps2                 = D3DCAPS2_FULLSCREENGAMMA;
        caps->Caps3                 = 0;
        caps->PresentationIntervals = D3DPRESENT_INTERVAL_DEFAULT | D3DPRESENT_INTERVAL_IMMEDIATE;
        caps->CursorCaps            = 0;
        caps->DevCaps               = D3DDEVCAPS_HWTRANSFORMANDLIGHT | D3DDEVCAPS_HWRASTERIZATION;
        caps->PrimitiveMiscCaps     = 0x0000008FL; // cullnone|cullcw|cullccw|colorwriteenable
        caps->RasterCaps            = D3DPRASTERCAPS_MIPMAPLODBIAS | D3DPRASTERCAPS_ZBIAS |
                                      D3DPRASTERCAPS_FOGVERTEX | D3DPRASTERCAPS_FOGTABLE |
                                      D3DPRASTERCAPS_ZFOG | D3DPRASTERCAPS_WFOG;
        caps->ZCmpCaps              = 0xFF;
        caps->SrcBlendCaps          = 0x3FFF;
        caps->DestBlendCaps         = 0x3FFF;
        caps->AlphaCmpCaps          = 0xFF;
        caps->ShadeCaps             = 0x000020E2L; // gouraud RGB, alpha blend, fog
        caps->TextureCaps           = D3DPTEXTURECAPS_ALPHA | D3DPTEXTURECAPS_MIPMAP |
                                      D3DPTEXTURECAPS_CUBEMAP | D3DPTEXTURECAPS_PERSPECTIVE |
                                      0x00000008L; // ALPHAPALETTE
        caps->TextureFilterCaps     = D3DPTFILTERCAPS_MINFLINEAR | D3DPTFILTERCAPS_MAGFLINEAR |
                                      D3DPTFILTERCAPS_MIPFLINEAR | D3DPTFILTERCAPS_MINFPOINT |
                                      D3DPTFILTERCAPS_MAGFPOINT  | D3DPTFILTERCAPS_MIPFPOINT;
        caps->CubeTextureFilterCaps   = caps->TextureFilterCaps;
        caps->VolumeTextureFilterCaps = caps->TextureFilterCaps;
        caps->TextureAddressCaps      = 0x0000001FL; // wrap|mirror|clamp|border|indep
        caps->VolumeTextureAddressCaps = caps->TextureAddressCaps;
        caps->LineCaps              = 0x0000001FL; // texture|ztest|blend|alphacmp|fog
        caps->MaxTextureWidth       = 4096;
        caps->MaxTextureHeight      = 4096;
        caps->MaxVolumeExtent       = 256;
        caps->MaxTextureRepeat      = 32768;
        caps->MaxTextureAspectRatio = 4096;
        caps->MaxAnisotropy         = 16;
        caps->MaxUserClipPlanes     = 6;
        caps->MaxActiveLights       = 8;
        caps->MaxVertexBlendMatrices    = 4;
        caps->MaxVertexBlendMatrixIndex = 8;
        caps->MaxPointSize          = 64.0f;
        caps->MaxPrimitiveCount     = 0xFFFF;
        caps->MaxVertexIndex        = 0xFFFF;
        caps->MaxStreams             = 1;
        caps->MaxStreamStride       = 256;
        caps->VertexShaderVersion   = 0xFFFE0101; // VS 1.1
        caps->MaxVertexShaderConst  = 96;
        caps->PixelShaderVersion    = 0xFFFF0104; // PS 1.4
        caps->MaxPixelShaderValue   = 8.0f;
        return S_OK;
    }

    HRESULT GetDisplayMode(D3DDISPLAYMODE* pMode) override {
        if (!pMode) return E_POINTER;
        SDL_DisplayMode mode;
        SDL_GetCurrentDisplayMode(0, &mode);
        pMode->Width       = static_cast<UINT>(mode.w);
        pMode->Height      = static_cast<UINT>(mode.h);
        pMode->RefreshRate = static_cast<UINT>(mode.refresh_rate);
        pMode->Format      = D3DFMT_X8R8G8B8;
        return S_OK;
    }

    HRESULT GetCreationParameters(void* params) override { return S_OK; }

    HRESULT Reset(D3DPRESENT_PARAMETERS* pp) override {
        if (pp) {
            if (pp->BackBufferWidth > 0)  width  = static_cast<int>(pp->BackBufferWidth);
            if (pp->BackBufferHeight > 0) height = static_cast<int>(pp->BackBufferHeight);
            SDL_Window* win = (SDL_Window*)SDL2_Platform_GetWindow();
            if (win) SDL_SetWindowSize(win, width, height);
            glViewport(0, 0, width, height);
        }
        return S_OK;
    }

    HRESULT Present(const RECT*, const RECT*, HWND, const void*) override {
        {
            std::vector<GLuint> toDelete;
            {
                std::lock_guard<std::mutex> lock(g_texDeleteMutex);
                toDelete.swap(g_pendingTexDeletes);
            }
            if (!toDelete.empty()) {
                glDeleteTextures(static_cast<GLsizei>(toDelete.size()), toDelete.data());
            }
        }
        SDL2_Platform_SwapWindow();
        return S_OK;
    }

    HRESULT BeginScene() override { return S_OK; }
    HRESULT EndScene()   override { return S_OK; }

    HRESULT Clear(DWORD count, const D3DRECT* pRects, DWORD flags, D3DCOLOR color, float z, DWORD stencil) override {
        float r = ColorComponent(color, 16);
        float g = ColorComponent(color,  8);
        float b = ColorComponent(color,  0);
        float a = ColorComponent(color, 24);
        glClearColor(r, g, b, a);
        glClearDepth(static_cast<GLclampd>(z));
        glClearStencil(static_cast<GLint>(stencil));
        GLbitfield mask = 0;
        if (flags & D3DCLEAR_TARGET)  mask |= GL_COLOR_BUFFER_BIT;
        if (flags & D3DCLEAR_ZBUFFER) mask |= GL_DEPTH_BUFFER_BIT;
        if (flags & D3DCLEAR_STENCIL) mask |= GL_STENCIL_BUFFER_BIT;
        if (mask) {
            // D3D8 Clear() ignores write-enable render states; GL's glClear() respects
            // glDepthMask/glColorMask. Force-enable write masks so the clear always works.
            if (flags & D3DCLEAR_TARGET)  glColorMask(GL_TRUE, GL_TRUE, GL_TRUE, GL_TRUE);
            if (flags & D3DCLEAR_ZBUFFER) glDepthMask(GL_TRUE);
            if (flags & D3DCLEAR_STENCIL) glStencilMask(0xFF);
            glClear(mask);
            // Restore GL write-mask state to match the tracked D3D render state.
            if (flags & D3DCLEAR_ZBUFFER) glDepthMask(rs[14] ? GL_TRUE : GL_FALSE);
            if (flags & D3DCLEAR_TARGET) {
                uint32_t cw = rs[168];  // D3DRS_COLORWRITEENABLE
                glColorMask((cw & 1) ? GL_TRUE : GL_FALSE,
                            (cw & 2) ? GL_TRUE : GL_FALSE,
                            (cw & 4) ? GL_TRUE : GL_FALSE,
                            (cw & 8) ? GL_TRUE : GL_FALSE);
            }
        }
        return S_OK;
    }

    HRESULT SetViewport(const D3DVIEWPORT8* vp) override {
        if (!vp) return E_POINTER;
        // D3D viewport Y is top-down; GL is bottom-up — flip Y
        int glY = height - static_cast<int>(vp->Y + vp->Height);
        glViewport(static_cast<GLint>(vp->X), glY, static_cast<GLsizei>(vp->Width), static_cast<GLsizei>(vp->Height));
        glDepthRange(static_cast<GLclampd>(vp->MinZ), static_cast<GLclampd>(vp->MaxZ));
        return S_OK;
    }
    HRESULT GetViewport(D3DVIEWPORT8* vp) override {
        if (!vp) return E_POINTER;
        vp->X = 0; vp->Y = 0;
        vp->Width = static_cast<DWORD>(width); vp->Height = static_cast<DWORD>(height);
        vp->MinZ = 0.0f; vp->MaxZ = 1.0f;
        return S_OK;
    }

    // State store — track for Apply_GL_State()
    HRESULT SetRenderState(D3DRENDERSTATETYPE state, DWORD value) override {
        if ((uint32_t)state < 256) {
            if (rs[state] == value) return S_OK;  // content dedup
            rs[state] = value;
            dirty |= rs_dirty_flag((uint32_t)state);
        }
        return S_OK;
    }
    HRESULT GetRenderState(D3DRENDERSTATETYPE state, DWORD* pVal) override {
        if (pVal) *pVal = ((unsigned)state < 256) ? rs[state] : 0;
        return S_OK;
    }
    HRESULT SetTransform(D3DTRANSFORMSTATETYPE type, const D3DMATRIX* m) override {
        if (!m) return S_OK;
        if (type >= D3DTS_TEXTURE0 && type <= D3DTS_TEXTURE7) {
            texMatrix[type - D3DTS_TEXTURE0] = *m;
            dirty |= DIRTY_TEXTURES;
            return S_OK;
        }
        switch (type) {
        case D3DTS_WORLD:      worldMatrix = *m; break;
        case D3DTS_VIEW:       viewMatrix  = *m; break;
        case D3DTS_PROJECTION: projMatrix  = *m; break;
        default: break;
        }
        dirty |= DIRTY_TRANSFORMS;
        return S_OK;
    }
    HRESULT GetTransform(D3DTRANSFORMSTATETYPE type, D3DMATRIX* m) override {
        if (!m) return S_OK;
        switch (type) {
        case D3DTS_WORLD:      *m = worldMatrix; break;
        case D3DTS_VIEW:       *m = viewMatrix;  break;
        case D3DTS_PROJECTION: *m = projMatrix;  break;
        default: memset(m, 0, sizeof(*m)); break;
        }
        return S_OK;
    }
    HRESULT MultiplyTransform(D3DTRANSFORMSTATETYPE, const D3DMATRIX*) override { return S_OK; }
    HRESULT SetMaterial(const D3DMATERIAL8* mat) override {
        if (mat) {
            if (memcmp(&material, mat, sizeof(D3DMATERIAL8)) == 0) {
                return S_OK;
            }
            material = *mat;
            dirty |= DIRTY_LIGHTING;
        }
        return S_OK;
    }
    HRESULT GetMaterial(D3DMATERIAL8* m) override {
        if (m) *m = material;
        return S_OK;
    }
    HRESULT SetLight(DWORD index, const D3DLIGHT8* light) override {
        if (index < 8 && light) {
            lights[index] = *light;
            dirty |= DIRTY_LIGHTS;
        }
        return S_OK;
    }
    HRESULT GetLight(DWORD index, D3DLIGHT8* l) override {
        if (l && index < 8) *l = lights[index];
        return S_OK;
    }
    HRESULT LightEnable(DWORD index, BOOL enable) override {
        if (index < 8) {
            lightEnabled[index] = enable;
            dirty |= DIRTY_LIGHTS;
        }
        return S_OK;
    }
    HRESULT GetLightEnable(DWORD index, BOOL* e) override {
        if (e) *e = (index < 8) ? lightEnabled[index] : FALSE;
        return S_OK;
    }
    HRESULT SetClipPlane(DWORD, const float*) override { return S_OK; }
    HRESULT GetClipPlane(DWORD, float* p)     override { if(p) memset(p,0,16); return S_OK; }
    HRESULT SetTexture(DWORD stage, IDirect3DBaseTexture8* tex) override {
        D3D8Texture_GL* t = static_cast<D3D8Texture_GL*>(tex);
        if (stage < 8) {
            if (currentTextures[stage] == t) return S_OK;  // content dedup
            currentTextures[stage] = t;
            dirty |= DIRTY_TEXTURES;
        }
        return S_OK;
    }
    HRESULT GetTexture(DWORD stage, IDirect3DBaseTexture8** pp) override {
        if (pp) *pp = (stage < 8) ? currentTextures[stage] : nullptr;
        return S_OK;
    }
    HRESULT SetTextureStageState(DWORD stage, D3DTEXTURESTAGESTATETYPE type, DWORD value) override {
        if (stage < 8 && (uint32_t)type < 32) {
            if (tss[stage][type] == value) return S_OK;  // content dedup
            tss[stage][type] = value;
            dirty |= DIRTY_TEXTURES;
        }
        return S_OK;
    }
    HRESULT GetTextureStageState(DWORD stage, D3DTEXTURESTAGESTATETYPE type, DWORD* v) override {
        if (v) *v = (stage < 8 && (unsigned)type < 32) ? tss[stage][type] : 0;
        return S_OK;
    }
    HRESULT ValidateDevice(DWORD* passes) override { if(passes)*passes=1; return S_OK; }
    HRESULT GetInfo(DWORD, void*, DWORD) override { return S_OK; }
    HRESULT SetStreamSource(UINT stream, IDirect3DVertexBuffer8* vb, UINT stride) override {
        if (stream == 0) {
            currentVB       = static_cast<D3D8VertexBuffer_GL*>(vb);
            currentVBStride = stride;
        }
        return S_OK;
    }
    HRESULT GetStreamSource(UINT, IDirect3DVertexBuffer8** pp, UINT* s) override {
        if (pp) *pp = currentVB; if (s) *s = currentVBStride; return S_OK;
    }
    HRESULT SetIndices(IDirect3DIndexBuffer8* ib, UINT baseVertex) override {
        currentIB           = static_cast<D3D8IndexBuffer_GL*>(ib);
        currentIBBaseVertex = baseVertex;
        return S_OK;
    }
    HRESULT GetIndices(IDirect3DIndexBuffer8** pp, UINT* b) override {
        if (pp) *pp = currentIB; if (b) *b = currentIBBaseVertex; return S_OK;
    }
    HRESULT SetVertexShader(DWORD fvf) override { currentFVF = fvf; return S_OK; }
    HRESULT GetVertexShader(DWORD* h) override { if (h) *h = currentFVF; return S_OK; }
    HRESULT SetPixelShader(DWORD) override { return S_OK; }
    HRESULT GetPixelShader(DWORD* h) override { if(h)*h=0; return S_OK; }

    // State blocks — Phase A: no-ops
    HRESULT BeginStateBlock() override { return S_OK; }
    HRESULT EndStateBlock(DWORD* t) override { if(t)*t=0; return S_OK; }
    HRESULT ApplyStateBlock(DWORD) override { return S_OK; }
    HRESULT CaptureStateBlock(DWORD) override { return S_OK; }
    HRESULT DeleteStateBlock(DWORD) override { return S_OK; }
    HRESULT CreateStateBlock(DWORD, DWORD* t) override { if(t)*t=0; return S_OK; }
    HRESULT SetClipStatus(const D3DCLIPSTATUS8*) override { return S_OK; }
    HRESULT GetClipStatus(D3DCLIPSTATUS8* c) override { if(c) memset(c,0,sizeof(*c)); return S_OK; }

    // Gamma
    void SetGammaRamp(DWORD, const void*) override {
        // SDL_SetWindowGammaRamp could go here
    }
    void GetGammaRamp(void* r) override { if(r) memset(r, 0, sizeof(WORD)*256*3); }

    // Palette
    HRESULT SetPaletteEntries(UINT, const PALETTEENTRY*) override { return S_OK; }
    HRESULT GetPaletteEntries(UINT, PALETTEENTRY*) override { return S_OK; }
    HRESULT SetCurrentTexturePalette(UINT) override { return S_OK; }
    HRESULT GetCurrentTexturePalette(UINT* n) override { if(n)*n=0; return S_OK; }

    // Cursor
    HRESULT SetCursorProperties(UINT, UINT, IDirect3DSurface8*) override { return S_OK; }
    void    SetCursorPosition(int, int, DWORD) override {}
    BOOL    ShowCursor(BOOL) override { return FALSE; }

    // Misc device queries
    HRESULT GetDirect3D(IDirect3D8** pp) override { if(pp)*pp=NULL; return S_OK; }
    HRESULT GetRasterStatus(void*) override { return S_OK; }
    HRESULT GetFrontBuffer(IDirect3DSurface8* p) override { return S_OK; }
    HRESULT SetRenderTarget(IDirect3DSurface8*, IDirect3DSurface8*) override { return S_OK; }
    HRESULT GetRenderTarget(IDirect3DSurface8** pp) override {
        if(pp) *pp = new D3D8Surface_GL();
        return S_OK;
    }
    HRESULT GetDepthStencilSurface(IDirect3DSurface8** pp) override {
        if(pp) *pp = new D3D8Surface_GL();
        return S_OK;
    }
    HRESULT GetBackBuffer(UINT, DWORD, IDirect3DSurface8** pp) override {
        if(pp) *pp = new D3D8Surface_GL();
        return S_OK;
    }

    // Resource creation
    HRESULT CreateTexture(UINT w, UINT h, UINT levels, DWORD usage, D3DFORMAT fmt, D3DPOOL pool, IDirect3DTexture8** pp) override {
        if (!pp) return E_POINTER;
        auto* newtex = new D3D8Texture_GL(w, h, fmt, levels);
        *pp = newtex;
        return S_OK;
    }
    HRESULT CreateVolumeTexture(UINT,UINT,UINT,UINT,DWORD,D3DFORMAT,D3DPOOL,IDirect3DVolumeTexture8** pp) override {
        if(pp)*pp=NULL; return S_OK;
    }
    HRESULT CreateCubeTexture(UINT,UINT,DWORD,D3DFORMAT,D3DPOOL,IDirect3DCubeTexture8** pp) override {
        if(pp)*pp=NULL; return S_OK;
    }
    HRESULT CreateVertexBuffer(UINT length, DWORD, DWORD, D3DPOOL, IDirect3DVertexBuffer8** pp) override {
        if (!pp) return E_POINTER;
        *pp = new D3D8VertexBuffer_GL(length);
        return S_OK;
    }
    HRESULT CreateIndexBuffer(UINT length, DWORD, D3DFORMAT, D3DPOOL, IDirect3DIndexBuffer8** pp) override {
        if (!pp) return E_POINTER;
        *pp = new D3D8IndexBuffer_GL(length);
        return S_OK;
    }
    HRESULT CreateRenderTarget(UINT w, UINT h, D3DFORMAT fmt, D3DMULTISAMPLE_TYPE, BOOL, IDirect3DSurface8** pp) override {
        if(pp) *pp = new D3D8Surface_GL(w, h, fmt);
        return S_OK;
    }
    HRESULT CreateDepthStencilSurface(UINT w, UINT h, D3DFORMAT fmt, D3DMULTISAMPLE_TYPE, IDirect3DSurface8** pp) override {
        if(pp) *pp = new D3D8Surface_GL(w, h, fmt);
        return S_OK;
    }
    HRESULT CreateImageSurface(UINT w, UINT h, D3DFORMAT fmt, IDirect3DSurface8** pp) override {
        if(pp) *pp = new D3D8Surface_GL(w, h, fmt);
        return S_OK;
    }
    HRESULT CreateAdditionalSwapChain(D3DPRESENT_PARAMETERS*, IDirect3DSwapChain8** pp) override {
        if(pp)*pp=NULL; return S_OK;
    }

    HRESULT CopyRects(IDirect3DSurface8* src, const RECT* srcRects, UINT numRects,
                      IDirect3DSurface8* dst, const POINT* dstPoints) override {
        auto* s = static_cast<D3D8Surface_GL*>(src);
        auto* d = static_cast<D3D8Surface_GL*>(dst);
        if (s && d && s->pixels && d->pixels && s->fmt == d->fmt) {
            if (numRects == 0) {
                UINT src_bytes = D3D8Surface_GL::total_surface_bytes(s->width, s->height, s->fmt);
                UINT dst_bytes = D3D8Surface_GL::total_surface_bytes(d->width, d->height, d->fmt);
                UINT bytes = src_bytes < dst_bytes ? src_bytes : dst_bytes;
                if (s->pixels != d->pixels)
                    memmove(d->pixels, s->pixels, bytes);
            } else {
                UINT bpp = D3D8Surface_GL::bytes_per_pixel(s->fmt);
                for (UINT i = 0; i < numRects; i++) {
                    int sx = srcRects ? static_cast<int32_t>(srcRects[i].left) : 0;
                    int sy = srcRects ? static_cast<int32_t>(srcRects[i].top)  : 0;
                    int sw = srcRects ? static_cast<int32_t>(srcRects[i].right - srcRects[i].left) : (int)s->width;
                    int sh = srcRects ? static_cast<int32_t>(srcRects[i].bottom - srcRects[i].top) : (int)s->height;
                    int dx = dstPoints ? static_cast<int32_t>(dstPoints[i].x) : 0;
                    int dy = dstPoints ? static_cast<int32_t>(dstPoints[i].y) : 0;
                    for (int row = 0; row < sh; row++) {
                        memcpy(d->pixels + ((dy + row) * (int)d->width + dx) * (int)bpp,
                               s->pixels + ((sy + row) * (int)s->width + sx) * (int)bpp,
                               (size_t)(sw * (int)bpp));
                    }
                }
            }
            if (d->parentTexture) {
                d->parentTexture->levelHasData[d->mipLevel] = true;
                d->parentTexture->dirty = true;
            }
        }
        return S_OK;
    }
    HRESULT UpdateTexture(IDirect3DBaseTexture8* src, IDirect3DBaseTexture8* dst) override {
        auto* s = static_cast<D3D8Texture_GL*>(src);
        auto* d = static_cast<D3D8Texture_GL*>(dst);
        if (s && d) {
            UINT levels = s->numLevels < d->numLevels ? s->numLevels : d->numLevels;
            for (UINT i = 0; i < levels && i < 16; i++) {
                if (s->levelPixels[i] && d->levelPixels[i]) {
                    UINT mw = D3D8Texture_GL::mip_dim(s->width, i);
                    UINT mh = D3D8Texture_GL::mip_dim(s->height, i);
                    UINT bytes = D3D8Surface_GL::total_surface_bytes(mw, mh, s->fmt);
                    memmove(d->levelPixels[i], s->levelPixels[i], bytes);
                    d->levelHasData[i] = s->levelHasData[i];
                }
            }
            d->dirty = true;
            d->Upload_To_GL();
        }
        return S_OK;
    }

    // Shader no-ops
    HRESULT CreateVertexShader(const DWORD*, const DWORD*, DWORD* h, DWORD) override { if(h)*h=1; return S_OK; }
    HRESULT DeleteVertexShader(DWORD) override { return S_OK; }
    HRESULT SetVertexShaderConstant(DWORD, const void*, DWORD) override { return S_OK; }
    HRESULT GetVertexShaderConstant(DWORD, void*, DWORD) override { return S_OK; }
    HRESULT GetVertexShaderDeclaration(DWORD, void*, DWORD*) override { return S_OK; }
    HRESULT GetVertexShaderFunction(DWORD, void*, DWORD*) override { return S_OK; }
    HRESULT CreatePixelShader(const DWORD*, DWORD* h) override { if(h)*h=1; return S_OK; }
    HRESULT DeletePixelShader(DWORD) override { return S_OK; }
    HRESULT SetPixelShaderConstant(DWORD, const void*, DWORD) override { return S_OK; }
    HRESULT GetPixelShaderConstant(DWORD, void*, DWORD) override { return S_OK; }
    HRESULT GetPixelShaderFunction(DWORD, void*, DWORD*) override { return S_OK; }

    // -----------------------------------------------------------------------
    // Draw calls — Phase B: actual OpenGL rendering
    // -----------------------------------------------------------------------

    // Compute byte offsets for each FVF field given the current FVF mask.
    // Returns false if the FVF doesn't contain position (D3DFVF_XYZ).
    // All output offsets are set to -1 if that field is absent.
    struct FVFLayout {
        int off_xyz;      // D3DFVF_XYZ  (12 bytes)
        int off_normal;   // D3DFVF_NORMAL (12 bytes)
        int off_diffuse;  // D3DFVF_DIFFUSE (4 bytes)
        int off_specular; // D3DFVF_SPECULAR (4 bytes)
        int off_uv[8];    // up to 8 UV sets (8 bytes each)
        int uv_count;
    };

    FVFLayout Compute_FVF_Layout(DWORD fvf) const {
        FVFLayout L;
        L.off_xyz = -1; L.off_normal = -1; L.off_diffuse = -1; L.off_specular = -1;
        L.uv_count = 0;
        for (int i = 0; i < 8; i++) L.off_uv[i] = -1;

        int off = 0;

        // Position
        if (fvf & D3DFVF_XYZ) {
            L.off_xyz = off; off += 12;
        } else if (fvf & D3DFVF_XYZRHW) {
            L.off_xyz = off; off += 16;  // XYZRHW is 16 bytes (x,y,z,rhw)
        }

        // Blend weights: only present for D3DFVF_XYZBn variants.
        // This game uses D3DFVF_XYZ (no blending), so no skip needed here.

        // Normal
        if (fvf & D3DFVF_NORMAL) {
            L.off_normal = off; off += 12;
        }

        // Point size (D3DFVF_PSIZE, 4 bytes) — rarely used
        if (fvf & D3DFVF_PSIZE) { off += 4; }

        // Diffuse
        if (fvf & D3DFVF_DIFFUSE) {
            L.off_diffuse = off; off += 4;
        }

        // Specular
        if (fvf & D3DFVF_SPECULAR) {
            L.off_specular = off; off += 4;
        }

        // Texture coordinates (8 bytes each for 2D UVs).
        // D3DFVF_TEX1=0x100, D3DFVF_TEX2=0x200, ...: tex count = bits[11:8] of FVF
        int tex_cnt = (int)((fvf >> 8) & 0xF);
        if (tex_cnt > 8) tex_cnt = 8;
        L.uv_count = tex_cnt;
        for (int t = 0; t < tex_cnt; t++) {
            // All UV sets in this game are 2D (8 bytes: float u, float v).
            // The full D3D spec allows bits[16+t*2..17+t*2] to encode 1D/2D/3D/4D,
            // but those bits are always zero here (2D), so we always add 8 bytes.
            L.off_uv[t] = off;
            off += 8;
        }
        return L;
    }

    // Submit one vertex to GL using the parsed FVF layout.
    // Default color is full white+opaque if no diffuse field.
    void GL_Submit_Vertex(const BYTE* vp, const FVFLayout& L) {
        float x = 0, y = 0, z = 0;
        if (L.off_xyz >= 0) {
            x = *(const float*)(vp + L.off_xyz + 0);
            y = *(const float*)(vp + L.off_xyz + 4);
            z = *(const float*)(vp + L.off_xyz + 8);
        }

        BYTE r = 255, g = 255, b = 255, a = 255;
        if (L.off_diffuse >= 0) {
            DWORD d = *(const DWORD*)(vp + L.off_diffuse);
            a = (d >> 24) & 0xFF;
            r = (d >> 16) & 0xFF;
            g = (d >>  8) & 0xFF;
            b = (d >>  0) & 0xFF;
        }

        if (L.off_normal >= 0) {
            float nx = *(const float*)(vp + L.off_normal + 0);
            float ny = *(const float*)(vp + L.off_normal + 4);
            float nz = *(const float*)(vp + L.off_normal + 8);
            glNormal3f(nx, ny, nz);
        }
        glColor4ub(r, g, b, a);
        if (L.uv_count == 0) {
            // D3D8 default: when FVF has no texture coordinates, D3D8 uses (0,0) for all
            // texture stages.  OpenGL would otherwise reuse the last submitted coordinate
            // from the previous draw call, producing garbage sampling.
            glMultiTexCoord2f(GL_TEXTURE0, 0.0f, 0.0f);
        } else {
            for (int t = 0; t < L.uv_count; t++) {
                if (L.off_uv[t] >= 0) {
                    float tu = *(const float*)(vp + L.off_uv[t] + 0);
                    float tv = *(const float*)(vp + L.off_uv[t] + 4);
                    glMultiTexCoord2f(static_cast<GLenum>(GL_TEXTURE0 + t), tu, tv);
                }
            }
        }
        glVertex3f(x, y, z);
    }

    // Helper: draw indexed primitives of any D3DPRIMITIVETYPE.
    // Uses currentFVF to parse vertex components correctly.
    void GL_Draw_Prim_Indexed(D3DPRIMITIVETYPE type, const BYTE* vertBase, UINT stride,
                              const unsigned short* indices, UINT primCount, UINT baseVertex)
    {
        FVFLayout L = Compute_FVF_Layout(currentFVF);
        Apply_GL_State();
        Apply_GL_Transforms();
        UINT indexCount = prim_vertex_count(type, primCount);
        glBegin(d3dprim_to_gl(type));
        for (UINT i = 0; i < indexCount; i++) {
            UINT vtxIdx = (UINT)indices[i] + baseVertex;
            const BYTE* vp = vertBase + vtxIdx * stride;
            GL_Submit_Vertex(vp, L);
        }
        glEnd();
    }

    HRESULT DrawPrimitive(D3DPRIMITIVETYPE type, UINT startVertex, UINT primCount) override {
        if (!currentVB || !currentVB->data) return S_OK;
        UINT vertCount = prim_vertex_count(type, primCount);
        FVFLayout L = Compute_FVF_Layout(currentFVF);
        Apply_GL_State();
        Apply_GL_Transforms();
        glBegin(d3dprim_to_gl(type));
        for (UINT i = 0; i < vertCount; i++) {
            UINT vtxIdx = startVertex + i;
            const BYTE* vp = currentVB->data + vtxIdx * currentVBStride;
            GL_Submit_Vertex(vp, L);
        }
        glEnd();
        return S_OK;
    }

    HRESULT DrawIndexedPrimitive(D3DPRIMITIVETYPE type, UINT minVtxIdx, UINT vtxCount,
                                  UINT startIdx, UINT primCount) override {
        if (!currentVB || !currentVB->data || !currentIB || !currentIB->data) return S_OK;
        const unsigned short* indices = (const unsigned short*)currentIB->data + startIdx;
        GL_Draw_Prim_Indexed(type, currentVB->data, currentVBStride,
                             indices, primCount, currentIBBaseVertex);
        return S_OK;
    }

    HRESULT DrawPrimitiveUP(D3DPRIMITIVETYPE type, UINT primCount,
                             const void* pVtx, UINT stride) override {
        if (!pVtx) return S_OK;
        UINT vertCount = prim_vertex_count(type, primCount);
        FVFLayout L = Compute_FVF_Layout(currentFVF);
        Apply_GL_State();
        Apply_GL_Transforms();
        glBegin(d3dprim_to_gl(type));
        for (UINT i = 0; i < vertCount; i++) {
            const BYTE* vp = (const BYTE*)pVtx + i * stride;
            GL_Submit_Vertex(vp, L);
        }
        glEnd();
        return S_OK;
    }

    HRESULT DrawIndexedPrimitiveUP(D3DPRIMITIVETYPE type, UINT minVtxIdx, UINT vtxCount,
                                    UINT primCount, const void* pIdxData, D3DFORMAT idxFmt,
                                    const void* pVtxData, UINT stride) override {
        if (!pIdxData || !pVtxData) return S_OK;
        const unsigned short* indices = (const unsigned short*)pIdxData;
        // minVtxIdx is the minimum index value (a range hint), not a vertex offset.
        // Vertex indices in pIdxData are absolute, so baseVertex = 0.
        GL_Draw_Prim_Indexed(type, (const BYTE*)pVtxData, stride,
                             indices, primCount, 0);
        return S_OK;
    }

    HRESULT ProcessVertices(UINT, UINT, UINT, IDirect3DVertexBuffer8*, DWORD) override { return S_OK; }

    // Patch draw calls
    HRESULT DrawRectPatch(UINT, const float*, const void*) override { return S_OK; }
    HRESULT DrawTriPatch(UINT, const float*, const void*) override { return S_OK; }
    HRESULT DeletePatch(UINT) override { return S_OK; }
};

// ---------------------------------------------------------------------------
// IDirect3D8_GL
// ---------------------------------------------------------------------------
struct IDirect3D8_GL : public IDirect3D8 {
    uint32_t refCount = 1;

    // IUnknown
    uint32_t AddRef()  override { return ++refCount; }
    uint32_t Release() override {
        if (--refCount == 0) { delete this; return 0; }
        return refCount;
    }
    HRESULT QueryInterface(REFIID, void**) override { return E_NOINTERFACE; }

    // IDirect3D8
    HRESULT RegisterSoftwareDevice(void*) override { return S_OK; }

    UINT GetAdapterCount() override {
        return (UINT)SDL_GetNumVideoDisplays();
    }

    HRESULT GetAdapterIdentifier(UINT adapter, DWORD flags, void* pIdent) override {
        // pIdent points to D3DADAPTER_IDENTIFIER8 — zero it out with a friendly name
        if (pIdent) memset(pIdent, 0, sizeof(D3DADAPTER_IDENTIFIER8));
        return S_OK;
    }

    UINT GetAdapterModeCount(UINT adapter) override {
        return (UINT)SDL_GetNumDisplayModes((int)adapter);
    }

    HRESULT EnumAdapterModes(UINT adapter, UINT mode, D3DDISPLAYMODE* pMode) override {
        if (!pMode) return E_POINTER;
        SDL_DisplayMode sdlMode;
        if (SDL_GetDisplayMode((int)adapter, (int)mode, &sdlMode) != 0) return D3DERR_INVALIDCALL;
        pMode->Width       = static_cast<UINT>(sdlMode.w);
        pMode->Height      = static_cast<UINT>(sdlMode.h);
        pMode->RefreshRate = static_cast<UINT>(sdlMode.refresh_rate);
        pMode->Format      = D3DFMT_X8R8G8B8;
        return S_OK;
    }

    HRESULT GetAdapterDisplayMode(UINT adapter, D3DDISPLAYMODE* pMode) override {
        if (!pMode) return E_POINTER;
        SDL_DisplayMode sdlMode;
        if (SDL_GetCurrentDisplayMode((int)adapter, &sdlMode) != 0) return D3DERR_INVALIDCALL;
        pMode->Width       = static_cast<UINT>(sdlMode.w);
        pMode->Height      = static_cast<UINT>(sdlMode.h);
        pMode->RefreshRate = static_cast<UINT>(sdlMode.refresh_rate);
        pMode->Format      = D3DFMT_X8R8G8B8;
        return S_OK;
    }

    HRESULT CheckDeviceType(UINT, D3DDEVTYPE, D3DFORMAT, D3DFORMAT, BOOL) override { return S_OK; }
    HRESULT CheckDeviceFormat(UINT, D3DDEVTYPE, D3DFORMAT, DWORD, D3DRESOURCETYPE, D3DFORMAT) override { return S_OK; }
    HRESULT CheckDeviceMultiSampleType(UINT, D3DDEVTYPE, D3DFORMAT, BOOL, D3DMULTISAMPLE_TYPE) override { return S_OK; }
    HRESULT CheckDepthStencilMatch(UINT, D3DDEVTYPE, D3DFORMAT, D3DFORMAT, D3DFORMAT) override { return S_OK; }

    HRESULT GetDeviceCaps(UINT adapter, D3DDEVTYPE devType, D3DCAPS8* pCaps) override {
        // Create a temporary device to fill caps
        IDirect3DDevice8_GL tmp;
        return tmp.GetDeviceCaps(pCaps);
    }

    HMONITOR GetAdapterMonitor(UINT) override { return NULL; }

    HRESULT CreateDevice(UINT adapter, D3DDEVTYPE devType, HWND hFocusWnd,
                         DWORD behaviorFlags, D3DPRESENT_PARAMETERS* pPP,
                         IDirect3DDevice8** ppDevice) override
    {
        if (!ppDevice) return E_POINTER;

        // SDL2 window + GL context were already created by CreateWindowEx.
        // If somehow not yet (e.g. dedicated server path), init now.
        if (!SDL2_Platform_GetWindow()) {
            int w = pPP ? static_cast<int>(pPP->BackBufferWidth)  : 800;
            int h = pPP ? static_cast<int>(pPP->BackBufferHeight) : 600;
            if (w <= 0) w = 800;
            if (h <= 0) h = 600;
            if (SDL2_Platform_Init("Renegade", w, h) != 0) return D3DERR_INVALIDCALL;
        }

        IDirect3DDevice8_GL* dev = new IDirect3DDevice8_GL();
        if (pPP) {
            if (pPP->BackBufferWidth  > 0) dev->width  = static_cast<int>(pPP->BackBufferWidth);
            if (pPP->BackBufferHeight > 0) dev->height = static_cast<int>(pPP->BackBufferHeight);
        }
        glViewport(0, 0, dev->width, dev->height);
        *ppDevice = dev;
        fprintf(stderr, "[D3D8_GL] IDirect3DDevice8 created (%dx%d)\n", dev->width, dev->height);
        return S_OK;
    }
};

// ---------------------------------------------------------------------------
// Entry point — replaces the old inline stub
// ---------------------------------------------------------------------------
IDirect3D8* Direct3DCreate8(UINT sdk_version)
{
    fprintf(stderr, "[D3D8_GL] Direct3DCreate8(%u) -> IDirect3D8_GL\n", sdk_version);
    return new IDirect3D8_GL();
}

// ---------------------------------------------------------------------------
// D3D8_GL_Reset_Device_State — reset internal state shadow to D3D8 defaults
//
// Called by DX8Wrapper::Invalidate_Cached_Render_States() when returning from
// game to menu on macOS.  On Windows, Device::Reset() does this automatically;
// on macOS there is no device reset, so stale rs[]/tss[][]/dirty values from
// the game would persist into the menu render.
//
// Mirrors the IDirect3DDevice8_GL constructor exactly, then forces dirty=DIRTY_ALL
// so the next Apply_GL_State() re-applies every state group to OpenGL.
// ---------------------------------------------------------------------------
void D3D8_GL_Reset_Device_State(IDirect3DDevice8* dev)
{
    IDirect3DDevice8_GL* d = static_cast<IDirect3DDevice8_GL*>(dev);
    if (!d) return;

    // Reset rs[] to D3D8 defaults (mirrors IDirect3DDevice8_GL constructor)
    memset(d->rs, 0, sizeof(d->rs));
    d->rs[7]   = 1;    // D3DRS_ZENABLE          = TRUE
    d->rs[14]  = 1;    // D3DRS_ZWRITEENABLE     = TRUE
    d->rs[19]  = 2;    // D3DRS_SRCBLEND         = D3DBLEND_ONE
    d->rs[20]  = 1;    // D3DRS_DESTBLEND        = D3DBLEND_ZERO
    d->rs[8]   = 3;    // D3DRS_FILLMODE         = D3DFILL_SOLID
    d->rs[9]   = 2;    // D3DRS_SHADEMODE        = D3DSHADE_GOURAUD
    d->rs[22]  = 3;    // D3DRS_CULLMODE         = D3DCULL_CCW
    d->rs[23]  = 4;    // D3DRS_ZFUNC            = D3DCMP_LESSEQUAL
    d->rs[25]  = 8;    // D3DRS_ALPHAFUNC        = D3DCMP_ALWAYS
    d->rs[137] = 1;    // D3DRS_LIGHTING         = TRUE
    d->rs[168] = 0xF;  // D3DRS_COLORWRITEENABLE = all channels (R|G|B|A)
    d->rs[171] = 1;    // D3DRS_BLENDOP          = D3DBLENDOP_ADD

    // Reset texture stage state (stage 0 defaults; stages 1-7 stay zero = disabled)
    memset(d->tss, 0, sizeof(d->tss));
    d->tss[0][1] = 4;  // COLOROP  = D3DTOP_MODULATE
    d->tss[0][2] = 2;  // COLORARG1 = D3DTA_TEXTURE
    d->tss[0][3] = 0;  // COLORARG2 = D3DTA_DIFFUSE
    d->tss[0][4] = 2;  // ALPHAOP  = D3DTOP_SELECTARG1
    d->tss[0][5] = 2;  // ALPHAARG1 = D3DTA_TEXTURE
    d->tss[0][6] = 0;  // ALPHAARG2 = D3DTA_DIFFUSE

    // Clear bound textures — currentTextures[] are non-owning pointers (SetTexture
    // does not AddRef), so just zero without Release.
    memset(d->currentTextures, 0, sizeof(d->currentTextures));

    // Mark everything dirty — next Apply_GL_State() will re-apply all state to GL
    d->dirty = DIRTY_ALL;

    // Immediately restore the GL write masks so that the next glClear() works correctly.
    // Apply_GL_State() is only called during draw calls, but glClear() can be called
    // before any draw (e.g., the backdrop clears the screen before rendering).
    // If the game left glDepthMask=GL_FALSE (typical for 2D/UI passes that disable
    // depth writes) or glColorMask=(0,0,0,0), glClear would silently skip clearing
    // those buffers — leaving stale game depth values that cause backdrop geometry to
    // fail the depth test, producing a black screen.
    //
    // These calls mirror the shadow values set above (rs[14]=1, rs[168]=0xF), keeping
    // the shadow and actual GL state in sync.
    glDepthMask(GL_TRUE);
    glColorMask(GL_TRUE, GL_TRUE, GL_TRUE, GL_TRUE);
    glStencilMask(0xFFFFFFFF);
}
