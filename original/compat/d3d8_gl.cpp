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

struct D3D8Surface_GL : public IDirect3DSurface8 {
    // IUnknown
    ULONG AddRef()  override { return 1; }
    ULONG Release() override { return 0; }
    HRESULT QueryInterface(REFIID, void**) override { return E_NOINTERFACE; }

    HRESULT GetDesc(D3DSURFACE_DESC* desc) override {
        if (desc) { memset(desc, 0, sizeof(*desc)); desc->Format = D3DFMT_A8R8G8B8; }
        return S_OK;
    }
    HRESULT LockRect(D3DLOCKED_RECT* lr, const RECT*, DWORD) override {
        if (lr) { lr->Pitch = 0; lr->pBits = NULL; }
        return S_OK;
    }
    HRESULT UnlockRect() override { return S_OK; }
};

struct D3D8Texture_GL : public IDirect3DTexture8 {
    // IUnknown
    ULONG AddRef()  override { return 1; }
    ULONG Release() override { return 0; }
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
        if (d) memset(d, 0, sizeof(*d));
        return S_OK;
    }
    HRESULT GetSurfaceLevel(UINT, IDirect3DSurface8** pp) override {
        if (pp) *pp = new D3D8Surface_GL();
        return S_OK;
    }
    HRESULT LockRect(UINT, D3DLOCKED_RECT* lr, const RECT*, DWORD) override {
        if (lr) { lr->Pitch = 0; lr->pBits = NULL; }
        return S_OK;
    }
    HRESULT UnlockRect(UINT) override { return S_OK; }
    HRESULT AddDirtyRect(const RECT*) override { return S_OK; }
};

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
// IDirect3DDevice8_GL — Phase A implementation
// ---------------------------------------------------------------------------
struct IDirect3DDevice8_GL : public IDirect3DDevice8 {
    ULONG   refCount = 1;
    int     width    = 800;
    int     height   = 600;

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
    HRESULT EndScene()   override { return S_OK; }

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

    // State store — all no-ops for Phase A
    HRESULT SetRenderState(D3DRENDERSTATETYPE, DWORD)        override { return S_OK; }
    HRESULT GetRenderState(D3DRENDERSTATETYPE, DWORD* pVal)  override { if(pVal)*pVal=0; return S_OK; }
    HRESULT SetTransform(D3DTRANSFORMSTATETYPE, const D3DMATRIX*) override { return S_OK; }
    HRESULT GetTransform(D3DTRANSFORMSTATETYPE, D3DMATRIX* m) override {
        if (m) memset(m, 0, sizeof(*m));
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
    HRESULT SetTexture(DWORD, IDirect3DBaseTexture8*)      override { return S_OK; }
    HRESULT GetTexture(DWORD, IDirect3DBaseTexture8** pp)  override { if(pp)*pp=NULL; return S_OK; }
    HRESULT SetTextureStageState(DWORD, D3DTEXTURESTAGESTATETYPE, DWORD) override { return S_OK; }
    HRESULT GetTextureStageState(DWORD, D3DTEXTURESTAGESTATETYPE, DWORD* v) override { if(v)*v=0; return S_OK; }
    HRESULT ValidateDevice(DWORD* passes) override { if(passes)*passes=1; return S_OK; }
    HRESULT GetInfo(DWORD, void*, DWORD) override { return S_OK; }
    HRESULT SetStreamSource(UINT, IDirect3DVertexBuffer8*, UINT) override { return S_OK; }
    HRESULT GetStreamSource(UINT, IDirect3DVertexBuffer8** pp, UINT* s) override {
        if(pp)*pp=NULL; if(s)*s=0; return S_OK;
    }
    HRESULT SetIndices(IDirect3DIndexBuffer8*, UINT) override { return S_OK; }
    HRESULT GetIndices(IDirect3DIndexBuffer8** pp, UINT* b) override { if(pp)*pp=NULL; if(b)*b=0; return S_OK; }
    HRESULT SetVertexShader(DWORD) override { return S_OK; }
    HRESULT GetVertexShader(DWORD* h) override { if(h)*h=0; return S_OK; }
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
        *pp = new D3D8Texture_GL();
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
    HRESULT CreateRenderTarget(UINT w, UINT h, D3DFORMAT, D3DMULTISAMPLE_TYPE, BOOL, IDirect3DSurface8** pp) override {
        if(pp) *pp = new D3D8Surface_GL();
        return S_OK;
    }
    HRESULT CreateDepthStencilSurface(UINT,UINT,D3DFORMAT,D3DMULTISAMPLE_TYPE,IDirect3DSurface8** pp) override {
        if(pp) *pp = new D3D8Surface_GL();
        return S_OK;
    }
    HRESULT CreateImageSurface(UINT,UINT,D3DFORMAT,IDirect3DSurface8** pp) override {
        if(pp) *pp = new D3D8Surface_GL();
        return S_OK;
    }
    HRESULT CreateAdditionalSwapChain(D3DPRESENT_PARAMETERS*, IDirect3DSwapChain8** pp) override {
        if(pp)*pp=NULL; return S_OK;
    }

    HRESULT CopyRects(IDirect3DSurface8*,const RECT*,UINT,IDirect3DSurface8*,const POINT*) override { return S_OK; }
    HRESULT UpdateTexture(IDirect3DBaseTexture8*,IDirect3DBaseTexture8*) override { return S_OK; }

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

    // Draw calls — Phase A: no-ops (no geometry output yet)
    HRESULT DrawPrimitive(D3DPRIMITIVETYPE, UINT, UINT) override { return S_OK; }
    HRESULT DrawIndexedPrimitive(D3DPRIMITIVETYPE, UINT, UINT, UINT, UINT) override { return S_OK; }
    HRESULT DrawPrimitiveUP(D3DPRIMITIVETYPE, UINT, const void*, UINT) override { return S_OK; }
    HRESULT DrawIndexedPrimitiveUP(D3DPRIMITIVETYPE, UINT, UINT, UINT, const void*, D3DFORMAT, const void*, UINT) override { return S_OK; }
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
