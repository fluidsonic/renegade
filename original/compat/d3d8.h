#pragma once

#include "global.h"

// d3d8.h compat shim for macOS

#include "d3d8types.h"
#include "d3d8caps.h"
#include "wingdi.h"  // for PALETTEENTRY

// Forward declarations
struct IDirect3D8;
struct IDirect3DSwapChain8;
struct IDirect3DDevice8;
struct IDirect3DResource8;
struct IDirect3DBaseTexture8;
struct IDirect3DTexture8;
struct IDirect3DCubeTexture8;
struct IDirect3DVolumeTexture8;
struct IDirect3DSurface8;
struct IDirect3DVolume8;
struct IDirect3DVertexBuffer8;
struct IDirect3DIndexBuffer8;

struct IDirect3DResource8 : public IUnknown {
    virtual HRESULT GetDevice(IDirect3DDevice8** ppDevice) { return E_NOTIMPL; }
    virtual HRESULT SetPrivateData(REFGUID guid, const void* data, DWORD size, DWORD flags) { return E_NOTIMPL; }
    virtual HRESULT GetPrivateData(REFGUID guid, void* data, DWORD* size) { return E_NOTIMPL; }
    virtual HRESULT FreePrivateData(REFGUID guid) { return E_NOTIMPL; }
    virtual DWORD   SetPriority(DWORD priority) { return 0; }
    virtual DWORD   GetPriority() { return 0; }
    virtual void    PreLoad() {}
    virtual D3DRESOURCETYPE GetType() { return D3DRTYPE_SURFACE; }
};

struct IDirect3DBaseTexture8 : public IDirect3DResource8 {
    virtual DWORD SetLOD(DWORD lod) { return 0; }
    virtual DWORD GetLOD() { return 0; }
    virtual DWORD GetLevelCount() { return 0; }
};

struct IDirect3DTexture8 : public IDirect3DBaseTexture8 {
    virtual HRESULT GetLevelDesc(UINT level, D3DSURFACE_DESC* desc) { return E_NOTIMPL; }
    virtual HRESULT GetSurfaceLevel(UINT level, IDirect3DSurface8** ppSurface) { return E_NOTIMPL; }
    virtual HRESULT LockRect(UINT level, D3DLOCKED_RECT* pLockedRect, const RECT* pRect, DWORD flags) { return E_NOTIMPL; }
    virtual HRESULT UnlockRect(UINT level) { return E_NOTIMPL; }
    virtual HRESULT AddDirtyRect(const RECT* pRect) { return E_NOTIMPL; }
};

struct IDirect3DCubeTexture8 : public IDirect3DBaseTexture8 {
    virtual HRESULT GetLevelDesc(UINT level, D3DSURFACE_DESC* desc) { return E_NOTIMPL; }
    virtual HRESULT GetCubeMapSurface(DWORD face, UINT level, IDirect3DSurface8** ppSurface) { return E_NOTIMPL; }
    virtual HRESULT LockRect(DWORD face, UINT level, D3DLOCKED_RECT* pLockedRect, const RECT* pRect, DWORD flags) { return E_NOTIMPL; }
    virtual HRESULT UnlockRect(DWORD face, UINT level) { return E_NOTIMPL; }
    virtual HRESULT AddDirtyRect(DWORD face, const RECT* pRect) { return E_NOTIMPL; }
};

struct IDirect3DVolumeTexture8 : public IDirect3DBaseTexture8 {
    virtual HRESULT GetLevelDesc(UINT level, D3DVOLUME_DESC* desc) { return E_NOTIMPL; }
    virtual HRESULT GetVolumeLevel(UINT level, IDirect3DVolume8** ppVolume) { return E_NOTIMPL; }
    virtual HRESULT LockBox(UINT level, D3DLOCKED_BOX* pLockedBox, const D3DBOX* pBox, DWORD flags) { return E_NOTIMPL; }
    virtual HRESULT UnlockBox(UINT level) { return E_NOTIMPL; }
    virtual HRESULT AddDirtyBox(const D3DBOX* pBox) { return E_NOTIMPL; }
};

struct IDirect3DSurface8 : public IUnknown {
    virtual HRESULT GetDevice(IDirect3DDevice8** ppDevice) { return E_NOTIMPL; }
    virtual HRESULT SetPrivateData(REFGUID guid, const void* data, DWORD size, DWORD flags) { return E_NOTIMPL; }
    virtual HRESULT GetPrivateData(REFGUID guid, void* data, DWORD* size) { return E_NOTIMPL; }
    virtual HRESULT FreePrivateData(REFGUID guid) { return E_NOTIMPL; }
    virtual HRESULT GetContainer(REFIID riid, void** ppContainer) { return E_NOTIMPL; }
    virtual HRESULT GetDesc(D3DSURFACE_DESC* desc) { return E_NOTIMPL; }
    virtual HRESULT LockRect(D3DLOCKED_RECT* pLockedRect, const RECT* pRect, DWORD flags) { return E_NOTIMPL; }
    virtual HRESULT UnlockRect() { return E_NOTIMPL; }
};

struct IDirect3DVertexBuffer8 : public IDirect3DResource8 {
    virtual HRESULT Lock(UINT offset, UINT size, BYTE** ppbData, DWORD flags) { return E_NOTIMPL; }
    virtual HRESULT Unlock() { return E_NOTIMPL; }
    virtual HRESULT GetDesc(void* desc) { return E_NOTIMPL; }
};

struct IDirect3DIndexBuffer8 : public IDirect3DResource8 {
    virtual HRESULT Lock(UINT offset, UINT size, BYTE** ppbData, DWORD flags) { return E_NOTIMPL; }
    virtual HRESULT Unlock() { return E_NOTIMPL; }
    virtual HRESULT GetDesc(void* desc) { return E_NOTIMPL; }
};

struct IDirect3DDevice8 : public IUnknown {
    virtual HRESULT TestCooperativeLevel() { return E_NOTIMPL; }
    virtual UINT    GetAvailableTextureMem() { return 0; }
    virtual HRESULT ResourceManagerDiscardBytes(DWORD bytes) { return E_NOTIMPL; }
    virtual HRESULT GetDirect3D(IDirect3D8** ppD3D8) { return E_NOTIMPL; }
    virtual HRESULT GetDeviceCaps(D3DCAPS8* caps) { return E_NOTIMPL; }
    virtual HRESULT GetDisplayMode(D3DDISPLAYMODE* pMode) { return E_NOTIMPL; }
    virtual HRESULT GetCreationParameters(void* params) { return E_NOTIMPL; }
    virtual HRESULT SetCursorProperties(UINT x, UINT y, IDirect3DSurface8* pBitmap) { return E_NOTIMPL; }
    virtual void    SetCursorPosition(int x, int y, DWORD flags) {}
    virtual BOOL    ShowCursor(BOOL show) { return FALSE; }
    virtual HRESULT CreateAdditionalSwapChain(D3DPRESENT_PARAMETERS* pp, IDirect3DSwapChain8** sc) { return E_NOTIMPL; }
    virtual HRESULT Reset(D3DPRESENT_PARAMETERS* pp) { return E_NOTIMPL; }
    virtual HRESULT Present(const RECT* src, const RECT* dst, HWND wnd, const void* region) { return E_NOTIMPL; }
    virtual HRESULT GetBackBuffer(UINT index, DWORD type, IDirect3DSurface8** ppBack) { return E_NOTIMPL; }
    virtual HRESULT GetRasterStatus(void* pStatus) { return E_NOTIMPL; }
    virtual void    SetGammaRamp(DWORD flags, const void* pRamp) {}
    virtual void    GetGammaRamp(void* pRamp) {}
    virtual HRESULT CreateTexture(UINT w, UINT h, UINT levels, DWORD usage, D3DFORMAT fmt, D3DPOOL pool, IDirect3DTexture8** ppTex) { return E_NOTIMPL; }
    virtual HRESULT CreateVolumeTexture(UINT w, UINT h, UINT d, UINT levels, DWORD usage, D3DFORMAT fmt, D3DPOOL pool, IDirect3DVolumeTexture8** ppVolTex) { return E_NOTIMPL; }
    virtual HRESULT CreateCubeTexture(UINT size, UINT levels, DWORD usage, D3DFORMAT fmt, D3DPOOL pool, IDirect3DCubeTexture8** ppCubeTex) { return E_NOTIMPL; }
    virtual HRESULT CreateVertexBuffer(UINT length, DWORD usage, DWORD fvf, D3DPOOL pool, IDirect3DVertexBuffer8** ppVB) { return E_NOTIMPL; }
    virtual HRESULT CreateIndexBuffer(UINT length, DWORD usage, D3DFORMAT fmt, D3DPOOL pool, IDirect3DIndexBuffer8** ppIB) { return E_NOTIMPL; }
    virtual HRESULT CreateRenderTarget(UINT w, UINT h, D3DFORMAT fmt, D3DMULTISAMPLE_TYPE ms, BOOL lockable, IDirect3DSurface8** ppSurf) { return E_NOTIMPL; }
    virtual HRESULT CreateDepthStencilSurface(UINT w, UINT h, D3DFORMAT fmt, D3DMULTISAMPLE_TYPE ms, IDirect3DSurface8** ppSurf) { return E_NOTIMPL; }
    virtual HRESULT CreateImageSurface(UINT w, UINT h, D3DFORMAT fmt, IDirect3DSurface8** ppSurf) { return E_NOTIMPL; }
    virtual HRESULT CopyRects(IDirect3DSurface8* src, const RECT* srcRects, UINT num, IDirect3DSurface8* dst, const POINT* dstPts) { return E_NOTIMPL; }
    virtual HRESULT UpdateTexture(IDirect3DBaseTexture8* src, IDirect3DBaseTexture8* dst) { return E_NOTIMPL; }
    virtual HRESULT GetFrontBuffer(IDirect3DSurface8* pDst) { return E_NOTIMPL; }
    virtual HRESULT SetRenderTarget(IDirect3DSurface8* pRT, IDirect3DSurface8* pZS) { return E_NOTIMPL; }
    virtual HRESULT GetRenderTarget(IDirect3DSurface8** ppRT) { return E_NOTIMPL; }
    virtual HRESULT GetDepthStencilSurface(IDirect3DSurface8** ppZS) { return E_NOTIMPL; }
    virtual HRESULT BeginScene() { return E_NOTIMPL; }
    virtual HRESULT EndScene() { return E_NOTIMPL; }
    virtual HRESULT Clear(DWORD count, const D3DRECT* pRects, DWORD flags, D3DCOLOR color, float z, DWORD stencil) { return E_NOTIMPL; }
    virtual HRESULT SetTransform(D3DTRANSFORMSTATETYPE state, const D3DMATRIX* pMatrix) { return E_NOTIMPL; }
    virtual HRESULT GetTransform(D3DTRANSFORMSTATETYPE state, D3DMATRIX* pMatrix) { return E_NOTIMPL; }
    virtual HRESULT MultiplyTransform(D3DTRANSFORMSTATETYPE state, const D3DMATRIX* pMatrix) { return E_NOTIMPL; }
    virtual HRESULT SetViewport(const D3DVIEWPORT8* pViewport) { return E_NOTIMPL; }
    virtual HRESULT GetViewport(D3DVIEWPORT8* pViewport) { return E_NOTIMPL; }
    virtual HRESULT SetMaterial(const D3DMATERIAL8* pMaterial) { return E_NOTIMPL; }
    virtual HRESULT GetMaterial(D3DMATERIAL8* pMaterial) { return E_NOTIMPL; }
    virtual HRESULT SetLight(DWORD index, const D3DLIGHT8* pLight) { return E_NOTIMPL; }
    virtual HRESULT GetLight(DWORD index, D3DLIGHT8* pLight) { return E_NOTIMPL; }
    virtual HRESULT LightEnable(DWORD index, BOOL enable) { return E_NOTIMPL; }
    virtual HRESULT GetLightEnable(DWORD index, BOOL* pEnable) { return E_NOTIMPL; }
    virtual HRESULT SetClipPlane(DWORD index, const float* pPlane) { return E_NOTIMPL; }
    virtual HRESULT GetClipPlane(DWORD index, float* pPlane) { return E_NOTIMPL; }
    virtual HRESULT SetRenderState(D3DRENDERSTATETYPE state, DWORD value) { return E_NOTIMPL; }
    virtual HRESULT GetRenderState(D3DRENDERSTATETYPE state, DWORD* pValue) { return E_NOTIMPL; }
    virtual HRESULT BeginStateBlock() { return E_NOTIMPL; }
    virtual HRESULT EndStateBlock(DWORD* pToken) { return E_NOTIMPL; }
    virtual HRESULT ApplyStateBlock(DWORD token) { return E_NOTIMPL; }
    virtual HRESULT CaptureStateBlock(DWORD token) { return E_NOTIMPL; }
    virtual HRESULT DeleteStateBlock(DWORD token) { return E_NOTIMPL; }
    virtual HRESULT CreateStateBlock(DWORD type, DWORD* pToken) { return E_NOTIMPL; }
    virtual HRESULT SetClipStatus(const D3DCLIPSTATUS8* pCS) { return E_NOTIMPL; }
    virtual HRESULT GetClipStatus(D3DCLIPSTATUS8* pCS) { return E_NOTIMPL; }
    virtual HRESULT GetTexture(DWORD stage, IDirect3DBaseTexture8** ppTex) { return E_NOTIMPL; }
    virtual HRESULT SetTexture(DWORD stage, IDirect3DBaseTexture8* pTex) { return E_NOTIMPL; }
    virtual HRESULT GetTextureStageState(DWORD stage, D3DTEXTURESTAGESTATETYPE state, DWORD* pValue) { return E_NOTIMPL; }
    virtual HRESULT SetTextureStageState(DWORD stage, D3DTEXTURESTAGESTATETYPE state, DWORD value) { return E_NOTIMPL; }
    virtual HRESULT ValidateDevice(DWORD* pNumPasses) { return E_NOTIMPL; }
    virtual HRESULT GetInfo(DWORD id, void* pStruct, DWORD size) { return E_NOTIMPL; }
    virtual HRESULT SetPaletteEntries(UINT num, const PALETTEENTRY* pEntries) { return E_NOTIMPL; }
    virtual HRESULT GetPaletteEntries(UINT num, PALETTEENTRY* pEntries) { return E_NOTIMPL; }
    virtual HRESULT SetCurrentTexturePalette(UINT num) { return E_NOTIMPL; }
    virtual HRESULT GetCurrentTexturePalette(UINT* pNum) { return E_NOTIMPL; }
    virtual HRESULT DrawPrimitive(D3DPRIMITIVETYPE type, UINT startVertex, UINT primCount) { return E_NOTIMPL; }
    virtual HRESULT DrawIndexedPrimitive(D3DPRIMITIVETYPE type, UINT minVtx, UINT numVtx, UINT startIdx, UINT primCount) { return E_NOTIMPL; }
    virtual HRESULT DrawPrimitiveUP(D3DPRIMITIVETYPE type, UINT primCount, const void* pVtx, UINT stride) { return E_NOTIMPL; }
    virtual HRESULT DrawIndexedPrimitiveUP(D3DPRIMITIVETYPE type, UINT minVtx, UINT numVtx, UINT primCount, const void* pIdx, D3DFORMAT idxFmt, const void* pVtx, UINT stride) { return E_NOTIMPL; }
    virtual HRESULT ProcessVertices(UINT srcStart, UINT dstIdx, UINT vtxCount, IDirect3DVertexBuffer8* pDstBuf, DWORD flags) { return E_NOTIMPL; }
    virtual HRESULT CreateVertexShader(const DWORD* pDecl, const DWORD* pFunc, DWORD* pHandle, DWORD usage) { return E_NOTIMPL; }
    virtual HRESULT SetVertexShader(DWORD handle) { return E_NOTIMPL; }
    virtual HRESULT GetVertexShader(DWORD* pHandle) { return E_NOTIMPL; }
    virtual HRESULT DeleteVertexShader(DWORD handle) { return E_NOTIMPL; }
    virtual HRESULT SetVertexShaderConstant(DWORD reg, const void* pData, DWORD count) { return E_NOTIMPL; }
    virtual HRESULT GetVertexShaderConstant(DWORD reg, void* pData, DWORD count) { return E_NOTIMPL; }
    virtual HRESULT GetVertexShaderDeclaration(DWORD handle, void* pData, DWORD* pSize) { return E_NOTIMPL; }
    virtual HRESULT GetVertexShaderFunction(DWORD handle, void* pData, DWORD* pSize) { return E_NOTIMPL; }
    virtual HRESULT SetStreamSource(UINT num, IDirect3DVertexBuffer8* pVB, UINT stride) { return E_NOTIMPL; }
    virtual HRESULT GetStreamSource(UINT num, IDirect3DVertexBuffer8** ppVB, UINT* pStride) { return E_NOTIMPL; }
    virtual HRESULT SetIndices(IDirect3DIndexBuffer8* pIB, UINT baseVtxIdx) { return E_NOTIMPL; }
    virtual HRESULT GetIndices(IDirect3DIndexBuffer8** ppIB, UINT* pBaseVtxIdx) { return E_NOTIMPL; }
    virtual HRESULT CreatePixelShader(const DWORD* pFunc, DWORD* pHandle) { return E_NOTIMPL; }
    virtual HRESULT SetPixelShader(DWORD handle) { return E_NOTIMPL; }
    virtual HRESULT GetPixelShader(DWORD* pHandle) { return E_NOTIMPL; }
    virtual HRESULT DeletePixelShader(DWORD handle) { return E_NOTIMPL; }
    virtual HRESULT SetPixelShaderConstant(DWORD reg, const void* pData, DWORD count) { return E_NOTIMPL; }
    virtual HRESULT GetPixelShaderConstant(DWORD reg, void* pData, DWORD count) { return E_NOTIMPL; }
    virtual HRESULT GetPixelShaderFunction(DWORD handle, void* pData, DWORD* pSize) { return E_NOTIMPL; }
    virtual HRESULT DrawRectPatch(UINT handle, const float* pSegs, const void* pInfo) { return E_NOTIMPL; }
    virtual HRESULT DrawTriPatch(UINT handle, const float* pSegs, const void* pInfo) { return E_NOTIMPL; }
    virtual HRESULT DeletePatch(UINT handle) { return E_NOTIMPL; }
};

struct IDirect3DSwapChain8 : public IUnknown {
    virtual HRESULT Present(const RECT* src, const RECT* dst, HWND wnd, const void* region) { return E_NOTIMPL; }
    virtual HRESULT GetBackBuffer(UINT index, DWORD type, IDirect3DSurface8** ppBack) { return E_NOTIMPL; }
    virtual HRESULT GetRasterStatus(void* pStatus) { return E_NOTIMPL; }
};

struct IDirect3D8 : public IUnknown {
    virtual HRESULT RegisterSoftwareDevice(void* pInitFn) { return E_NOTIMPL; }
    virtual UINT    GetAdapterCount() { return 1; }
    virtual HRESULT GetAdapterIdentifier(UINT adapter, DWORD flags, void* pIdent) { return E_NOTIMPL; }
    virtual UINT    GetAdapterModeCount(UINT adapter) { return 0; }
    virtual HRESULT EnumAdapterModes(UINT adapter, UINT mode, D3DDISPLAYMODE* pMode) { return E_NOTIMPL; }
    virtual HRESULT GetAdapterDisplayMode(UINT adapter, D3DDISPLAYMODE* pMode) { return E_NOTIMPL; }
    virtual HRESULT CheckDeviceType(UINT adapter, D3DDEVTYPE devType, D3DFORMAT displayFmt, D3DFORMAT backBufFmt, BOOL windowed) { return E_NOTIMPL; }
    virtual HRESULT CheckDeviceFormat(UINT adapter, D3DDEVTYPE devType, D3DFORMAT adapterFmt, DWORD usage, D3DRESOURCETYPE rtype, D3DFORMAT checkFmt) { return E_NOTIMPL; }
    virtual HRESULT CheckDeviceMultiSampleType(UINT adapter, D3DDEVTYPE devType, D3DFORMAT surfFmt, BOOL windowed, D3DMULTISAMPLE_TYPE msType) { return E_NOTIMPL; }
    virtual HRESULT CheckDepthStencilMatch(UINT adapter, D3DDEVTYPE devType, D3DFORMAT adapterFmt, D3DFORMAT rtFmt, D3DFORMAT dsFmt) { return E_NOTIMPL; }
    virtual HRESULT GetDeviceCaps(UINT adapter, D3DDEVTYPE devType, D3DCAPS8* pCaps) { return E_NOTIMPL; }
    virtual HMONITOR GetAdapterMonitor(UINT adapter) { return NULL; }
    virtual HRESULT CreateDevice(UINT adapter, D3DDEVTYPE devType, HWND hFocusWnd, DWORD behaviorFlags, D3DPRESENT_PARAMETERS* pPP, IDirect3DDevice8** ppDevice) { return E_NOTIMPL; }
};

// Implemented in d3d8_gl.cpp — returns a real OpenGL-backed IDirect3D8
IDirect3D8* Direct3DCreate8(UINT sdk_version);

// Convenience typedefs
typedef IDirect3DSurface8*       LPDIRECT3DSURFACE8;
typedef IDirect3DTexture8*       LPDIRECT3DTEXTURE8;
typedef IDirect3DDevice8*        LPDIRECT3DDEVICE8;
typedef IDirect3D8*              LPDIRECT3D8;
typedef IDirect3DVertexBuffer8*  LPDIRECT3DVERTEXBUFFER8;
typedef IDirect3DIndexBuffer8*   LPDIRECT3DINDEXBUFFER8;

#define D3DCLEAR_TARGET    0x00000001L
#define D3DCLEAR_ZBUFFER   0x00000002L
#define D3DCLEAR_STENCIL   0x00000004L
#define D3D_SDK_VERSION    220
