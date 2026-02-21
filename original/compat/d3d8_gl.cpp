// d3d8_gl.cpp — D3D8 → OpenGL 2.1 translation layer (Phase A)
// Implements IDirect3D8 and IDirect3DDevice8 backed by SDL2+OpenGL.
// Phase A: enough to pass WW3D::Init() and run the game loop with glClear.

#define SDL_MAIN_HANDLED
#include <SDL2/SDL.h>
#include <OpenGL/gl.h>
#include <OpenGL/glu.h>
#include <stdio.h>
#include <string.h>
#include <new>

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
// Stub resource objects (Phase A: just enough to not crash)
// ---------------------------------------------------------------------------

// Forward declaration so D3D8Surface_GL::UnlockRect can notify the parent texture
struct D3D8Texture_GL;

struct D3D8Surface_GL : public IDirect3DSurface8 {
    ULONG           refCount      = 1;
    UINT            width         = 1;
    UINT            height        = 1;
    D3DFORMAT       fmt           = D3DFMT_A8R8G8B8;
    BYTE*           pixels        = NULL;
    bool            ownsPixels    = true;
    D3D8Texture_GL* parentTexture = nullptr;  // non-null if this is a texture surface level

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

    // Default: small owned placeholder (for stub RenderTarget/DepthStencil)
    D3D8Surface_GL()
        : width(1), height(1), fmt(D3DFMT_A8R8G8B8), ownsPixels(true)
    { pixels = new BYTE[4](); }

    // Owned pixel storage (CreateImageSurface, CreateRenderTarget, etc.)
    D3D8Surface_GL(UINT w, UINT h, D3DFORMAT f)
        : width(w ? w : 1), height(h ? h : 1), fmt(f), ownsPixels(true)
    { pixels = new BYTE[width * height * bytes_per_pixel(f)](); }

    // Non-owned: shares pixel buffer from parent (e.g., texture surface level)
    D3D8Surface_GL(UINT w, UINT h, D3DFORMAT f, BYTE* external_pixels)
        : width(w ? w : 1), height(h ? h : 1), fmt(f),
          pixels(external_pixels), ownsPixels(false)
    {}

    ~D3D8Surface_GL() { if (ownsPixels) delete[] pixels; }

    // IUnknown
    ULONG AddRef()  override { return ++refCount; }
    ULONG Release() override { if (--refCount == 0) { delete this; return 0; } return refCount; }
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
    HRESULT LockRect(D3DLOCKED_RECT* lr, const RECT*, DWORD) override {
        if (lr) {
            lr->Pitch = (int)(width * bytes_per_pixel(fmt));
            lr->pBits = pixels;
        }
        return S_OK;
    }
    HRESULT UnlockRect() override;  // defined after D3D8Texture_GL (needs parentTexture->Upload_To_GL)
};

struct D3D8Texture_GL : public IDirect3DTexture8 {
    ULONG     refCount = 1;
    UINT      width    = 1;
    UINT      height   = 1;
    D3DFORMAT fmt      = D3DFMT_A8R8G8B8;
    BYTE*     pixels   = NULL;   // CPU-side pixel store for level 0
    GLuint    glTexId  = 0;      // GL texture object (0 = not yet uploaded)
    bool      dirty    = false;  // pixels changed since last upload

    D3D8Texture_GL(UINT w, UINT h, D3DFORMAT f = D3DFMT_A8R8G8B8)
        : width(w ? w : 1), height(h ? h : 1), fmt(f)
    {
        pixels = new BYTE[width * height * D3D8Surface_GL::bytes_per_pixel(f)]();
    }
    ~D3D8Texture_GL() {
        delete[] pixels;
        if (glTexId) glDeleteTextures(1, &glTexId);
    }

    // Upload CPU pixels to GL. Must be called with GL context current.
    //
    // D3D convention: UV V=0 = texture top; row 0 of the pixel buffer = top row.
    // GL convention:  UV V=0 = texture bottom; glTexImage2D row 0 = bottom row.
    // The draw calls use the formula  gl_v = 1.0 - d3d_v  to map D3D UVs to GL UVs,
    // so GL V=1.0 must correspond to D3D row 0 (the top).  We achieve this by
    // uploading the rows in REVERSE order (D3D row 0 last = GL top = GL V=1.0).
    void Upload_To_GL() {
        if (!pixels) return;
        if (!glTexId) glGenTextures(1, &glTexId);
        glBindTexture(GL_TEXTURE_2D, glTexId);

        // Map D3D format → GL internal format, external format, data type
        GLenum gl_internal = GL_RGBA8;
        GLenum gl_format   = GL_BGRA_EXT;
        GLenum gl_type     = GL_UNSIGNED_BYTE;

        switch (fmt) {
        case D3DFMT_A8R8G8B8:
            gl_internal = GL_RGBA8;  gl_format = GL_BGRA_EXT; gl_type = GL_UNSIGNED_BYTE; break;
        case D3DFMT_X8R8G8B8:
            gl_internal = GL_RGB8;   gl_format = GL_BGRA_EXT; gl_type = GL_UNSIGNED_BYTE; break;
        case D3DFMT_A4R4G4B4:
            // Memory layout (little-endian 16-bit): bits[15:12]=A,[11:8]=R,[7:4]=G,[3:0]=B
            // GL_BGRA + GL_UNSIGNED_SHORT_4_4_4_4_REV: reads bits[15:12]=A,[11:8]=R,[7:4]=G,[3:0]=B ✓
            gl_internal = GL_RGBA4;  gl_format = GL_BGRA_EXT; gl_type = GL_UNSIGNED_SHORT_4_4_4_4_REV; break;
        case D3DFMT_A1R5G5B5:
            gl_internal = GL_RGB5_A1; gl_format = GL_BGRA_EXT; gl_type = GL_UNSIGNED_SHORT_1_5_5_5_REV; break;
        case D3DFMT_R5G6B5:
            gl_internal = GL_RGB;    gl_format = GL_RGB;       gl_type = GL_UNSIGNED_SHORT_5_6_5_REV; break;
        case D3DFMT_A8:
        case D3DFMT_L8:
            gl_internal = GL_ALPHA8; gl_format = GL_ALPHA;     gl_type = GL_UNSIGNED_BYTE; break;
        default:
            gl_internal = GL_RGBA8;  gl_format = GL_BGRA_EXT;  gl_type = GL_UNSIGNED_BYTE; break;
        }

        // Build a temporary row-reversed copy of the pixel data so that D3D row 0
        // (top) is uploaded last and lands at GL V=1.0 (GL top).  This pairs with
        // the  gl_v = 1 - d3d_v  UV transform applied in every draw call.
        UINT bpp      = D3D8Surface_GL::bytes_per_pixel(fmt);
        UINT row_bytes = width * bpp;
        BYTE* flipped = new BYTE[height * row_bytes];
        for (UINT r = 0; r < height; r++) {
            memcpy(flipped + r * row_bytes,
                   pixels  + (height - 1 - r) * row_bytes,
                   row_bytes);
        }

        glTexImage2D(GL_TEXTURE_2D, 0, gl_internal, (GLsizei)width, (GLsizei)height,
                     0, gl_format, gl_type, flipped);
        delete[] flipped;

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        GLenum err = glGetError();
        if (err) fprintf(stderr, "[d3d8] Upload_To_GL(%ux%u fmt=%u): GL error 0x%x\n",
                         width, height, (unsigned)fmt, err);
        dirty = false;
    }

    // IUnknown
    ULONG AddRef()  override { return ++refCount; }
    ULONG Release() override { if (--refCount == 0) { delete this; return 0; } return refCount; }
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
    DWORD GetLevelCount() override { return 1; }

    // IDirect3DTexture8
    HRESULT GetLevelDesc(UINT, D3DSURFACE_DESC* d) override {
        if (d) {
            memset(d, 0, sizeof(*d));
            d->Width  = width;
            d->Height = height;
            d->Format = fmt;
        }
        return S_OK;
    }
    HRESULT GetSurfaceLevel(UINT, IDirect3DSurface8** pp) override {
        if (pp) {
            auto* surf = new D3D8Surface_GL(width, height, fmt, pixels);
            surf->parentTexture = this;
            *pp = surf;
        }
        return S_OK;
    }
    HRESULT LockRect(UINT level, D3DLOCKED_RECT* lr, const RECT*, DWORD) override {
        static unsigned s_lock_count = 0;
        if (++s_lock_count <= 10) {
            fprintf(stderr, "[Texture::LockRect] #%u: %ux%u fmt=%u level=%u\n",
                    s_lock_count, width, height, (unsigned)fmt, level);
        }
        if (lr) {
            lr->Pitch = (int)(width * D3D8Surface_GL::bytes_per_pixel(fmt));
            lr->pBits = pixels;
        }
        return S_OK;
    }
    HRESULT UnlockRect(UINT level) override {
        static unsigned s_unlock_count = 0;
        if (++s_unlock_count <= 10) {
            fprintf(stderr, "[Texture::UnlockRect] #%u: %ux%u fmt=%u level=%u → uploading\n",
                    s_unlock_count, width, height, (unsigned)fmt, level);
        }
        if (level == 0) {
            dirty = true;
            Upload_To_GL();
        }
        return S_OK;
    }
    HRESULT AddDirtyRect(const RECT*) override { dirty = true; return S_OK; }
};

// Out-of-line definition: notify parent texture to upload when a surface level is unlocked
HRESULT D3D8Surface_GL::UnlockRect() {
    if (parentTexture) {
        parentTexture->dirty = true;
        parentTexture->Upload_To_GL();
    }
    return S_OK;
}

struct D3D8VertexBuffer_GL : public IDirect3DVertexBuffer8 {
    BYTE* data = NULL;
    UINT  size = 0;

    D3D8VertexBuffer_GL(UINT sz) : size(sz) { data = new BYTE[sz]; memset(data, 0, sz); }
    ~D3D8VertexBuffer_GL() { delete[] data; }

    ULONG AddRef()  override { return 1; }
    ULONG Release() override { delete this; return 0; }
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

    ULONG AddRef()  override { return 1; }
    ULONG Release() override { delete this; return 0; }
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
// Per-frame render stats — printed once per second
// ---------------------------------------------------------------------------
struct FrameStats {
    unsigned drawCalls      = 0;  // DrawIndexedPrimitive + DrawPrimitive
    unsigned drawUPCalls    = 0;  // DrawIndexedPrimitiveUP + DrawPrimitiveUP
    unsigned setTexture     = 0;  // SetTexture
    unsigned setTransform   = 0;  // SetTransform
    unsigned setRenderState = 0;  // SetRenderState
    unsigned createTexture  = 0;  // CreateTexture (lifetime, not per-frame)
    unsigned frames         = 0;
};
static FrameStats s_stats;
static FrameStats s_reported;       // stats as of last report
static unsigned   s_report_frame = 0;

static void report_frame_stats(void)
{
    s_stats.frames++;
    s_report_frame++;
    // Print once per 300 frames (~5s) — but only if something changed
    if (s_report_frame % 300 == 0) {
        fprintf(stderr,
            "[d3d8] frame %u: draw=%u drawUP=%u setTex=%u setXform=%u setRS=%u (createTex=%u total)\n",
            s_stats.frames,
            s_stats.drawCalls, s_stats.drawUPCalls,
            s_stats.setTexture, s_stats.setTransform, s_stats.setRenderState,
            s_stats.createTexture);
        // Reset per-frame counters; keep createTexture (lifetime)
        unsigned saved_create = s_stats.createTexture;
        s_stats = FrameStats{};
        s_stats.createTexture = saved_create;
    }
}

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

// Transpose a D3D row-major matrix to a GL column-major float[16]
static void d3d_to_gl_matrix(const D3DMATRIX* m, float out[16]) {
    out[0]  = m->_11;  out[1]  = m->_21;  out[2]  = m->_31;  out[3]  = m->_41;
    out[4]  = m->_12;  out[5]  = m->_22;  out[6]  = m->_32;  out[7]  = m->_42;
    out[8]  = m->_13;  out[9]  = m->_23;  out[10] = m->_33;  out[11] = m->_43;
    out[12] = m->_14;  out[13] = m->_24;  out[14] = m->_34;  out[15] = m->_44;
}

// ---------------------------------------------------------------------------
// IDirect3DDevice8_GL — Phase A/B implementation
// ---------------------------------------------------------------------------
struct IDirect3DDevice8_GL : public IDirect3DDevice8 {
    ULONG   refCount = 1;
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

    IDirect3DDevice8_GL() {
        // Identity matrices
        memset(&worldMatrix, 0, sizeof(worldMatrix));
        memset(&viewMatrix,  0, sizeof(viewMatrix));
        memset(&projMatrix,  0, sizeof(projMatrix));
        worldMatrix._11 = worldMatrix._22 = worldMatrix._33 = worldMatrix._44 = 1.0f;
        viewMatrix._11  = viewMatrix._22  = viewMatrix._33  = viewMatrix._44  = 1.0f;
        projMatrix._11  = projMatrix._22  = projMatrix._33  = projMatrix._44  = 1.0f;
        // D3D render state defaults
        rs[7]  = 1;  // D3DRS_ZENABLE = TRUE
        rs[14] = 1;  // D3DRS_ZWRITEENABLE = TRUE
        rs[19] = 2;  // D3DRS_SRCBLEND = D3DBLEND_ONE
        rs[20] = 1;  // D3DRS_DESTBLEND = D3DBLEND_ZERO
        rs[22] = 3;  // D3DRS_CULLMODE = D3DCULL_CCW
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
        // -- Alpha blending --
        if (rs[27]) {   // D3DRS_ALPHABLENDENABLE
            glEnable(GL_BLEND);
            glBlendFunc(d3dblend_to_gl(rs[19]), d3dblend_to_gl(rs[20]));
        } else {
            glDisable(GL_BLEND);
        }

        // -- Depth test --
        if (rs[7]) {    // D3DRS_ZENABLE
            glEnable(GL_DEPTH_TEST);
        } else {
            glDisable(GL_DEPTH_TEST);
        }
        glDepthMask(rs[14] ? GL_TRUE : GL_FALSE);  // D3DRS_ZWRITEENABLE

        // -- Cull mode --
        switch (rs[22]) {  // D3DRS_CULLMODE
        case 1: glDisable(GL_CULL_FACE); break;                                          // D3DCULL_NONE
        case 2: glEnable(GL_CULL_FACE); glCullFace(GL_BACK);  break;                    // D3DCULL_CW (flipped for GL)
        case 3: glEnable(GL_CULL_FACE); glCullFace(GL_FRONT); break;                    // D3DCULL_CCW (flipped for GL)
        default: glDisable(GL_CULL_FACE); break;
        }

        // -- Alpha test --
        if (rs[15]) {   // D3DRS_ALPHATESTENABLE
            glEnable(GL_ALPHA_TEST);
            // D3DRS_ALPHAREF = 24, D3DRS_ALPHAFUNC = 25 — simplified: just use GREATER
            glAlphaFunc(GL_GREATER, (float)(rs[24] & 0xFF) / 255.0f);
        } else {
            glDisable(GL_ALPHA_TEST);
        }

        // -- Texture stage 0 --
        // Map D3D texture stage states to GL_COMBINE texture environment.
        //
        // D3DTEXTURESTAGESTATETYPE indices:
        //   1=COLOROP, 2=COLORARG1, 3=COLORARG2
        //   4=ALPHAOP,  5=ALPHAARG1, 6=ALPHAARG2
        //
        // D3DTEXTUREOP values:
        //   1=DISABLE, 2=SELECTARG1, 3=SELECTARG2, 4=MODULATE, 7=ADD
        //
        // D3DTA values:
        //   0=DIFFUSE, 1=CURRENT, 2=TEXTURE, 3=TFACTOR, 4=SPECULAR
        //
        // D3DTA_CURRENT and D3DTA_DIFFUSE are both mapped to GL_PRIMARY_COLOR
        // since there is only one texture stage active.
        D3D8Texture_GL* tex = currentTextures[0];
        if (tex) {
            if (tex->dirty) tex->Upload_To_GL();
            if (tex->glTexId) {
                glEnable(GL_TEXTURE_2D);
                glBindTexture(GL_TEXTURE_2D, tex->glTexId);

                // Helper lambda: map D3DTA to GL combine source
                auto d3dta_to_gl = [](DWORD arg) -> GLenum {
                    switch (arg & 0xF) {  // mask off D3DTA_COMPLEMENT etc.
                    case 2: return GL_TEXTURE;        // D3DTA_TEXTURE
                    case 0:                            // D3DTA_DIFFUSE
                    case 1: return GL_PRIMARY_COLOR;  // D3DTA_CURRENT (same as diffuse for stage 0)
                    default: return GL_PRIMARY_COLOR;
                    }
                };

                DWORD colorOp   = tss[0][1];  // D3DTSS_COLOROP
                DWORD colorArg1 = tss[0][2];  // D3DTSS_COLORARG1
                DWORD colorArg2 = tss[0][3];  // D3DTSS_COLORARG2
                DWORD alphaOp   = tss[0][4];  // D3DTSS_ALPHAOP
                DWORD alphaArg1 = tss[0][5];  // D3DTSS_ALPHAARG1
                DWORD alphaArg2 = tss[0][6];  // D3DTSS_ALPHAARG2

                glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_COMBINE);

                // -- Color combine --
                switch (colorOp) {
                default:
                case 4:  // D3DTOP_MODULATE → GL_MODULATE(arg1, arg2)
                    glTexEnvi(GL_TEXTURE_ENV, GL_COMBINE_RGB, GL_MODULATE);
                    glTexEnvi(GL_TEXTURE_ENV, GL_SOURCE0_RGB, d3dta_to_gl(colorArg1));
                    glTexEnvi(GL_TEXTURE_ENV, GL_OPERAND0_RGB, GL_SRC_COLOR);
                    glTexEnvi(GL_TEXTURE_ENV, GL_SOURCE1_RGB, d3dta_to_gl(colorArg2));
                    glTexEnvi(GL_TEXTURE_ENV, GL_OPERAND1_RGB, GL_SRC_COLOR);
                    break;
                case 2:  // D3DTOP_SELECTARG1 → GL_REPLACE with arg1
                    glTexEnvi(GL_TEXTURE_ENV, GL_COMBINE_RGB, GL_REPLACE);
                    glTexEnvi(GL_TEXTURE_ENV, GL_SOURCE0_RGB, d3dta_to_gl(colorArg1));
                    glTexEnvi(GL_TEXTURE_ENV, GL_OPERAND0_RGB, GL_SRC_COLOR);
                    break;
                case 3:  // D3DTOP_SELECTARG2 → GL_REPLACE with arg2
                    glTexEnvi(GL_TEXTURE_ENV, GL_COMBINE_RGB, GL_REPLACE);
                    glTexEnvi(GL_TEXTURE_ENV, GL_SOURCE0_RGB, d3dta_to_gl(colorArg2));
                    glTexEnvi(GL_TEXTURE_ENV, GL_OPERAND0_RGB, GL_SRC_COLOR);
                    break;
                case 7:  // D3DTOP_ADD → GL_ADD(arg1, arg2)
                    glTexEnvi(GL_TEXTURE_ENV, GL_COMBINE_RGB, GL_ADD);
                    glTexEnvi(GL_TEXTURE_ENV, GL_SOURCE0_RGB, d3dta_to_gl(colorArg1));
                    glTexEnvi(GL_TEXTURE_ENV, GL_OPERAND0_RGB, GL_SRC_COLOR);
                    glTexEnvi(GL_TEXTURE_ENV, GL_SOURCE1_RGB, d3dta_to_gl(colorArg2));
                    glTexEnvi(GL_TEXTURE_ENV, GL_OPERAND1_RGB, GL_SRC_COLOR);
                    break;
                case 1:  // D3DTOP_DISABLE — output primary color (diffuse)
                    glTexEnvi(GL_TEXTURE_ENV, GL_COMBINE_RGB, GL_REPLACE);
                    glTexEnvi(GL_TEXTURE_ENV, GL_SOURCE0_RGB, GL_PRIMARY_COLOR);
                    glTexEnvi(GL_TEXTURE_ENV, GL_OPERAND0_RGB, GL_SRC_COLOR);
                    break;
                }

                // -- Alpha combine --
                switch (alphaOp) {
                default:
                case 4:  // D3DTOP_MODULATE → GL_MODULATE(arg1, arg2)
                    glTexEnvi(GL_TEXTURE_ENV, GL_COMBINE_ALPHA, GL_MODULATE);
                    glTexEnvi(GL_TEXTURE_ENV, GL_SOURCE0_ALPHA, d3dta_to_gl(alphaArg1));
                    glTexEnvi(GL_TEXTURE_ENV, GL_OPERAND0_ALPHA, GL_SRC_ALPHA);
                    glTexEnvi(GL_TEXTURE_ENV, GL_SOURCE1_ALPHA, d3dta_to_gl(alphaArg2));
                    glTexEnvi(GL_TEXTURE_ENV, GL_OPERAND1_ALPHA, GL_SRC_ALPHA);
                    break;
                case 2:  // D3DTOP_SELECTARG1 → GL_REPLACE with arg1
                    glTexEnvi(GL_TEXTURE_ENV, GL_COMBINE_ALPHA, GL_REPLACE);
                    glTexEnvi(GL_TEXTURE_ENV, GL_SOURCE0_ALPHA, d3dta_to_gl(alphaArg1));
                    glTexEnvi(GL_TEXTURE_ENV, GL_OPERAND0_ALPHA, GL_SRC_ALPHA);
                    break;
                case 3:  // D3DTOP_SELECTARG2 → GL_REPLACE with arg2
                    glTexEnvi(GL_TEXTURE_ENV, GL_COMBINE_ALPHA, GL_REPLACE);
                    glTexEnvi(GL_TEXTURE_ENV, GL_SOURCE0_ALPHA, d3dta_to_gl(alphaArg2));
                    glTexEnvi(GL_TEXTURE_ENV, GL_OPERAND0_ALPHA, GL_SRC_ALPHA);
                    break;
                case 7:  // D3DTOP_ADD → GL_ADD(arg1, arg2)
                    glTexEnvi(GL_TEXTURE_ENV, GL_COMBINE_ALPHA, GL_ADD);
                    glTexEnvi(GL_TEXTURE_ENV, GL_SOURCE0_ALPHA, d3dta_to_gl(alphaArg1));
                    glTexEnvi(GL_TEXTURE_ENV, GL_OPERAND0_ALPHA, GL_SRC_ALPHA);
                    glTexEnvi(GL_TEXTURE_ENV, GL_SOURCE1_ALPHA, d3dta_to_gl(alphaArg2));
                    glTexEnvi(GL_TEXTURE_ENV, GL_OPERAND1_ALPHA, GL_SRC_ALPHA);
                    break;
                case 1:  // D3DTOP_DISABLE — output primary alpha (diffuse)
                    glTexEnvi(GL_TEXTURE_ENV, GL_COMBINE_ALPHA, GL_REPLACE);
                    glTexEnvi(GL_TEXTURE_ENV, GL_SOURCE0_ALPHA, GL_PRIMARY_COLOR);
                    glTexEnvi(GL_TEXTURE_ENV, GL_OPERAND0_ALPHA, GL_SRC_ALPHA);
                    break;
                }
            } else {
                glDisable(GL_TEXTURE_2D);
            }
        } else {
            glDisable(GL_TEXTURE_2D);
        }
    }

    // Apply D3D world/view/proj matrices to GL
    void Apply_GL_Transforms() {
        float mat[16];
        glMatrixMode(GL_PROJECTION);
        d3d_to_gl_matrix(&projMatrix, mat);
        glLoadMatrixf(mat);

        glMatrixMode(GL_MODELVIEW);
        d3d_to_gl_matrix(&viewMatrix, mat);
        glLoadMatrixf(mat);
        // Multiply modelview by world (in GL: Modelview = View_gl * World_gl)
        float wmat[16];
        d3d_to_gl_matrix(&worldMatrix, wmat);
        glMultMatrixf(wmat);
    }

    // IUnknown
    ULONG AddRef()  override { return ++refCount; }
    ULONG Release() override {
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
        pMode->Width       = mode.w;
        pMode->Height      = mode.h;
        pMode->RefreshRate = mode.refresh_rate;
        pMode->Format      = D3DFMT_X8R8G8B8;
        return S_OK;
    }

    HRESULT GetCreationParameters(void* params) override { return S_OK; }

    HRESULT Reset(D3DPRESENT_PARAMETERS* pp) override {
        if (pp) {
            if (pp->BackBufferWidth > 0)  width  = pp->BackBufferWidth;
            if (pp->BackBufferHeight > 0) height = pp->BackBufferHeight;
            SDL_Window* win = (SDL_Window*)SDL2_Platform_GetWindow();
            if (win) SDL_SetWindowSize(win, width, height);
            glViewport(0, 0, width, height);
        }
        return S_OK;
    }

    HRESULT Present(const RECT*, const RECT*, HWND, const void*) override {
        SDL2_Platform_SwapWindow();
        return S_OK;
    }

    HRESULT BeginScene() override { return S_OK; }
    HRESULT EndScene()   override { report_frame_stats(); return S_OK; }

    HRESULT Clear(DWORD count, const D3DRECT* pRects, DWORD flags, D3DCOLOR color, float z, DWORD stencil) override {
        float r = ColorComponent(color, 16);
        float g = ColorComponent(color,  8);
        float b = ColorComponent(color,  0);
        float a = ColorComponent(color, 24);
        glClearColor(r, g, b, a);
        glClearDepth(z);
        glClearStencil(stencil);
        GLbitfield mask = 0;
        if (flags & D3DCLEAR_TARGET)  mask |= GL_COLOR_BUFFER_BIT;
        if (flags & D3DCLEAR_ZBUFFER) mask |= GL_DEPTH_BUFFER_BIT;
        if (flags & D3DCLEAR_STENCIL) mask |= GL_STENCIL_BUFFER_BIT;
        if (mask) glClear(mask);
        return S_OK;
    }

    HRESULT SetViewport(const D3DVIEWPORT8* vp) override {
        if (!vp) return E_POINTER;
        // D3D viewport Y is top-down; GL is bottom-up — flip Y
        int glY = height - (vp->Y + vp->Height);
        glViewport(vp->X, glY, vp->Width, vp->Height);
        glDepthRange(vp->MinZ, vp->MaxZ);
        return S_OK;
    }
    HRESULT GetViewport(D3DVIEWPORT8* vp) override {
        if (!vp) return E_POINTER;
        vp->X = 0; vp->Y = 0;
        vp->Width = width; vp->Height = height;
        vp->MinZ = 0.0f; vp->MaxZ = 1.0f;
        return S_OK;
    }

    // State store — track for Apply_GL_State()
    HRESULT SetRenderState(D3DRENDERSTATETYPE state, DWORD value) override {
        s_stats.setRenderState++;
        if ((unsigned)state < 256) rs[state] = value;
        return S_OK;
    }
    HRESULT GetRenderState(D3DRENDERSTATETYPE state, DWORD* pVal) override {
        if (pVal) *pVal = ((unsigned)state < 256) ? rs[state] : 0;
        return S_OK;
    }
    HRESULT SetTransform(D3DTRANSFORMSTATETYPE type, const D3DMATRIX* m) override {
        s_stats.setTransform++;
        if (!m) return S_OK;
        switch (type) {
        case D3DTS_WORLD:      worldMatrix = *m; break;
        case D3DTS_VIEW:       viewMatrix  = *m; break;
        case D3DTS_PROJECTION: projMatrix  = *m; break;
        default: break;
        }
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
    HRESULT SetMaterial(const D3DMATERIAL8*)  override { return S_OK; }
    HRESULT GetMaterial(D3DMATERIAL8* m)      override { if(m) memset(m,0,sizeof(*m)); return S_OK; }
    HRESULT SetLight(DWORD, const D3DLIGHT8*) override { return S_OK; }
    HRESULT GetLight(DWORD, D3DLIGHT8* l)     override { if(l) memset(l,0,sizeof(*l)); return S_OK; }
    HRESULT LightEnable(DWORD, BOOL)          override { return S_OK; }
    HRESULT GetLightEnable(DWORD, BOOL* e)    override { if(e)*e=FALSE; return S_OK; }
    HRESULT SetClipPlane(DWORD, const float*) override { return S_OK; }
    HRESULT GetClipPlane(DWORD, float* p)     override { if(p) memset(p,0,16); return S_OK; }
    HRESULT SetTexture(DWORD stage, IDirect3DBaseTexture8* tex) override {
        s_stats.setTexture++;
        if (stage < 8) currentTextures[stage] = static_cast<D3D8Texture_GL*>(tex);
        return S_OK;
    }
    HRESULT GetTexture(DWORD stage, IDirect3DBaseTexture8** pp) override {
        if (pp) *pp = (stage < 8) ? currentTextures[stage] : nullptr;
        return S_OK;
    }
    HRESULT SetTextureStageState(DWORD stage, D3DTEXTURESTAGESTATETYPE type, DWORD value) override {
        if (stage < 8 && (unsigned)type < 32) tss[stage][type] = value;
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
        *pp = new D3D8Texture_GL(w, h, fmt);
        s_stats.createTexture++;
        if (s_stats.createTexture <= 5) {
            fprintf(stderr, "[d3d8] CreateTexture #%u: %ux%u fmt=%u\n",
                    s_stats.createTexture, w, h, (unsigned)fmt);
        }
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
        // Copy pixel data from src surface to dst surface
        auto* s = static_cast<D3D8Surface_GL*>(src);
        auto* d = static_cast<D3D8Surface_GL*>(dst);
        static unsigned s_copy_count = 0;
        if (++s_copy_count <= 5) {
            fprintf(stderr, "[CopyRects] #%u: src=%p(%ux%u fmt=%u) dst=%p(%ux%u fmt=%u) rects=%u\n",
                s_copy_count,
                (void*)s, s ? s->width : 0, s ? s->height : 0, s ? (unsigned)s->fmt : 0,
                (void*)d, d ? d->width : 0, d ? d->height : 0, d ? (unsigned)d->fmt : 0,
                numRects);
        }
        if (s && d && s->pixels && d->pixels && s->fmt == d->fmt) {
            UINT bpp = D3D8Surface_GL::bytes_per_pixel(s->fmt);
            if (numRects == 0) {
                // Full copy
                UINT w = (s->width  < d->width)  ? s->width  : d->width;
                UINT h = (s->height < d->height) ? s->height : d->height;
                for (UINT row = 0; row < h; row++) {
                    memcpy(d->pixels + row * d->width * bpp,
                           s->pixels + row * s->width * bpp,
                           w * bpp);
                }
            } else {
                for (UINT i = 0; i < numRects; i++) {
                    int sx = srcRects ? srcRects[i].left : 0;
                    int sy = srcRects ? srcRects[i].top  : 0;
                    int sw = srcRects ? (srcRects[i].right - srcRects[i].left) : (int)s->width;
                    int sh = srcRects ? (srcRects[i].bottom - srcRects[i].top) : (int)s->height;
                    int dx = dstPoints ? dstPoints[i].x : 0;
                    int dy = dstPoints ? dstPoints[i].y : 0;
                    for (int row = 0; row < sh; row++) {
                        memcpy(d->pixels + ((dy + row) * d->width + dx) * bpp,
                               s->pixels + ((sy + row) * s->width + sx) * bpp,
                               sw * bpp);
                    }
                }
            }
            // If dst is a texture surface level, notify parent
            if (d->parentTexture) {
                d->parentTexture->dirty = true;
                d->parentTexture->Upload_To_GL();
            }
        }
        return S_OK;
    }
    HRESULT UpdateTexture(IDirect3DBaseTexture8* src, IDirect3DBaseTexture8* dst) override {
        auto* s = static_cast<D3D8Texture_GL*>(src);
        auto* d = static_cast<D3D8Texture_GL*>(dst);
        static unsigned s_upd_count = 0;
        if (++s_upd_count <= 5) {
            fprintf(stderr, "[UpdateTexture] #%u: src=%p(%ux%u fmt=%u) dst=%p(%ux%u fmt=%u)\n",
                s_upd_count,
                (void*)s, s ? s->width : 0, s ? s->height : 0, s ? (unsigned)s->fmt : 0,
                (void*)d, d ? d->width : 0, d ? d->height : 0, d ? (unsigned)d->fmt : 0);
        }
        if (s && d && s->pixels && d->pixels) {
            UINT bpp = D3D8Surface_GL::bytes_per_pixel(s->fmt);
            UINT bytes = s->width * s->height * bpp;
            if (d->width == s->width && d->height == s->height && d->fmt == s->fmt) {
                memcpy(d->pixels, s->pixels, bytes);
                d->dirty = true;
                d->Upload_To_GL();
            }
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

    // Helper: draw one triangle using vertex data at given indices
    void GL_Draw_Triangle_List(const BYTE* vertBase, UINT stride,
                               const unsigned short* indices, UINT indexCount,
                               UINT baseVertex)
    {
        Apply_GL_State();
        Apply_GL_Transforms();
        glBegin(GL_TRIANGLES);
        for (UINT i = 0; i < indexCount; i++) {
            UINT vtxIdx = (UINT)indices[i] + baseVertex;
            const BYTE* vp = vertBase + vtxIdx * stride;
            // Vertex layout: float x,y,z  float nx,ny,nz  uint32 diffuse  float u1,v1  float u2,v2
            float x = *(const float*)(vp +  0);
            float y = *(const float*)(vp +  4);
            float z = *(const float*)(vp +  8);
            DWORD d = *(const DWORD*) (vp + 24);
            float u = *(const float*)(vp + 28);
            float v = *(const float*)(vp + 32);
            BYTE a = (d >> 24) & 0xFF;
            BYTE r = (d >> 16) & 0xFF;
            BYTE g = (d >>  8) & 0xFF;
            BYTE b = (d >>  0) & 0xFF;
            glColor4ub(r, g, b, a);
            glTexCoord2f(u, 1.0f - v);
            glVertex3f(x, y, z);
        }
        glEnd();
    }

    HRESULT DrawPrimitive(D3DPRIMITIVETYPE type, UINT startVertex, UINT primCount) override {
        s_stats.drawCalls++;
        if (!currentVB || !currentVB->data || type != D3DPT_TRIANGLELIST) return S_OK;
        // Build a trivial index list [0,1,2, 3,4,5, ...]
        UINT indexCount = primCount * 3;
        Apply_GL_State();
        Apply_GL_Transforms();
        glBegin(GL_TRIANGLES);
        for (UINT i = 0; i < indexCount; i++) {
            UINT vtxIdx = startVertex + i;
            const BYTE* vp = currentVB->data + vtxIdx * currentVBStride;
            float x = *(const float*)(vp +  0);
            float y = *(const float*)(vp +  4);
            float z = *(const float*)(vp +  8);
            DWORD d = *(const DWORD*) (vp + 24);
            float u = *(const float*)(vp + 28);
            float vt = *(const float*)(vp + 32);
            BYTE a = (d>>24)&0xFF; BYTE r = (d>>16)&0xFF;
            BYTE g = (d>>8)&0xFF;  BYTE bv= (d>>0)&0xFF;
            glColor4ub(r, g, bv, a);
            glTexCoord2f(u, 1.0f - vt);
            glVertex3f(x, y, z);
        }
        glEnd();
        return S_OK;
    }

    HRESULT DrawIndexedPrimitive(D3DPRIMITIVETYPE type, UINT minVtxIdx, UINT vtxCount,
                                  UINT startIdx, UINT primCount) override {
        s_stats.drawCalls++;
        if (!currentVB || !currentVB->data || !currentIB || !currentIB->data) return S_OK;
        if (type != D3DPT_TRIANGLELIST) return S_OK;  // only triangles for now

        // Log first 3 draw calls: vertex positions, color, and GL state
        static unsigned s_dip_count = 0;
        if (++s_dip_count <= 3) {
            const unsigned short* dbg_idx = (const unsigned short*)currentIB->data + startIdx;
            const BYTE* v0 = currentVB->data + ((UINT)dbg_idx[0] + currentIBBaseVertex) * currentVBStride;
            float x = *(const float*)(v0+0), y = *(const float*)(v0+4), z = *(const float*)(v0+8);
            DWORD d = *(const DWORD*)(v0+24);
            float u = *(const float*)(v0+28), vt = *(const float*)(v0+32);
            GLboolean blendOn=0; glGetBooleanv(GL_BLEND, &blendOn);
            GLint texBound=0; glGetIntegerv(GL_TEXTURE_BINDING_2D, &texBound);
            D3D8Texture_GL* tex = currentTextures[0];
            fprintf(stderr, "[DIP] #%u: stride=%u startIdx=%u primCount=%u baseVtx=%u\n"
                            "       v0=(%.3f,%.3f,%.3f) diffuse=0x%08X uv=(%.3f,%.3f)\n"
                            "       blend=%d texBound=%d texPtr=%p glTexId=%u blendSrc=%u blendDst=%u\n",
                s_dip_count, currentVBStride, startIdx, primCount, currentIBBaseVertex,
                x, y, z, (unsigned)d, u, vt,
                (int)blendOn, texBound, (void*)tex, tex ? tex->glTexId : 0,
                rs[19], rs[20]);
            GLenum err = glGetError();
            if (err) fprintf(stderr, "[DIP] GL error before draw: 0x%x\n", err);
        }

        const unsigned short* indices = (const unsigned short*)currentIB->data + startIdx;
        GL_Draw_Triangle_List(currentVB->data, currentVBStride,
                              indices, primCount * 3, currentIBBaseVertex);

        if (s_dip_count <= 3) {
            GLenum err = glGetError();
            if (err) fprintf(stderr, "[DIP] GL error after draw: 0x%x\n", err);
        }
        return S_OK;
    }

    HRESULT DrawPrimitiveUP(D3DPRIMITIVETYPE type, UINT primCount,
                             const void* pVtx, UINT stride) override {
        s_stats.drawUPCalls++;
        if (!pVtx || type != D3DPT_TRIANGLELIST) return S_OK;
        UINT indexCount = primCount * 3;
        Apply_GL_State();
        Apply_GL_Transforms();
        glBegin(GL_TRIANGLES);
        for (UINT i = 0; i < indexCount; i++) {
            const BYTE* vp = (const BYTE*)pVtx + i * stride;
            float x = *(const float*)(vp +  0);
            float y = *(const float*)(vp +  4);
            float z = *(const float*)(vp +  8);
            DWORD d = *(const DWORD*) (vp + 24);
            float u = *(const float*)(vp + 28);
            float v = *(const float*)(vp + 32);
            BYTE a=(d>>24)&0xFF; BYTE r=(d>>16)&0xFF;
            BYTE g=(d>>8)&0xFF;  BYTE b=(d>>0)&0xFF;
            glColor4ub(r,g,b,a); glTexCoord2f(u, 1.0f - v); glVertex3f(x,y,z);
        }
        glEnd();
        return S_OK;
    }

    HRESULT DrawIndexedPrimitiveUP(D3DPRIMITIVETYPE type, UINT minVtxIdx, UINT vtxCount,
                                    UINT primCount, const void* pIdxData, D3DFORMAT idxFmt,
                                    const void* pVtxData, UINT stride) override {
        s_stats.drawUPCalls++;
        if (!pIdxData || !pVtxData || type != D3DPT_TRIANGLELIST) return S_OK;
        const unsigned short* indices = (const unsigned short*)pIdxData;
        GL_Draw_Triangle_List((const BYTE*)pVtxData, stride,
                              indices, primCount * 3, minVtxIdx);
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
    ULONG refCount = 1;

    // IUnknown
    ULONG AddRef()  override { return ++refCount; }
    ULONG Release() override {
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
        pMode->Width       = sdlMode.w;
        pMode->Height      = sdlMode.h;
        pMode->RefreshRate = sdlMode.refresh_rate;
        pMode->Format      = D3DFMT_X8R8G8B8;
        return S_OK;
    }

    HRESULT GetAdapterDisplayMode(UINT adapter, D3DDISPLAYMODE* pMode) override {
        if (!pMode) return E_POINTER;
        SDL_DisplayMode sdlMode;
        if (SDL_GetCurrentDisplayMode((int)adapter, &sdlMode) != 0) return D3DERR_INVALIDCALL;
        pMode->Width       = sdlMode.w;
        pMode->Height      = sdlMode.h;
        pMode->RefreshRate = sdlMode.refresh_rate;
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
            int w = pPP ? pPP->BackBufferWidth  : 800;
            int h = pPP ? pPP->BackBufferHeight : 600;
            if (w <= 0) w = 800;
            if (h <= 0) h = 600;
            if (SDL2_Platform_Init("Renegade", w, h) != 0) return D3DERR_INVALIDCALL;
        }

        IDirect3DDevice8_GL* dev = new IDirect3DDevice8_GL();
        if (pPP) {
            if (pPP->BackBufferWidth  > 0) dev->width  = pPP->BackBufferWidth;
            if (pPP->BackBufferHeight > 0) dev->height = pPP->BackBufferHeight;
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
