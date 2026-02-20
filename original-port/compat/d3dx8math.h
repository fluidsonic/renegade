// d3dx8math.h compat shim for macOS
#pragma once
#ifndef D3DX8MATH_H_COMPAT
#define D3DX8MATH_H_COMPAT

#include "d3d8types.h"
#include <math.h>
#include <string.h>

typedef struct D3DXVECTOR2 {
    float x, y;
    D3DXVECTOR2() : x(0), y(0) {}
    D3DXVECTOR2(float x, float y) : x(x), y(y) {}
} D3DXVECTOR2, *LPD3DXVECTOR2;

typedef struct D3DXVECTOR3 {
    float x, y, z;
    D3DXVECTOR3() : x(0), y(0), z(0) {}
    D3DXVECTOR3(float x, float y, float z) : x(x), y(y), z(z) {}
} D3DXVECTOR3, *LPD3DXVECTOR3;

typedef struct D3DXVECTOR4 {
    float x, y, z, w;
    D3DXVECTOR4() : x(0), y(0), z(0), w(0) {}
    D3DXVECTOR4(float x, float y, float z, float w) : x(x), y(y), z(z), w(w) {}
} D3DXVECTOR4, *LPD3DXVECTOR4;

typedef struct D3DXMATRIX : public D3DMATRIX {
    D3DXMATRIX() {}
    float& operator()(int r, int c) { return m[r][c]; }
    const float& operator()(int r, int c) const { return m[r][c]; }
} D3DXMATRIX, *LPD3DXMATRIX;

typedef struct D3DXQUATERNION {
    float x, y, z, w;
    D3DXQUATERNION() : x(0), y(0), z(0), w(1) {}
    D3DXQUATERNION(float x, float y, float z, float w) : x(x), y(y), z(z), w(w) {}
} D3DXQUATERNION, *LPD3DXQUATERNION;

typedef struct D3DXPLANE {
    float a, b, c, d;
    D3DXPLANE() : a(0), b(0), c(0), d(0) {}
    D3DXPLANE(float a, float b, float c, float d) : a(a), b(b), c(c), d(d) {}
} D3DXPLANE, *LPD3DXPLANE;

inline D3DXMATRIX* D3DXMatrixIdentity(D3DXMATRIX* pOut) {
    if (!pOut) return NULL;
    memset(pOut, 0, sizeof(D3DXMATRIX));
    pOut->m[0][0] = pOut->m[1][1] = pOut->m[2][2] = pOut->m[3][3] = 1.0f;
    return pOut;
}

inline D3DXMATRIX* D3DXMatrixMultiply(D3DXMATRIX* pOut, const D3DXMATRIX* pM1, const D3DXMATRIX* pM2) {
    if (!pOut || !pM1 || !pM2) return NULL;
    D3DXMATRIX tmp;
    for (int r=0; r<4; r++)
        for (int c=0; c<4; c++) {
            tmp.m[r][c] = 0;
            for (int k=0; k<4; k++) tmp.m[r][c] += pM1->m[r][k] * pM2->m[k][c];
        }
    *pOut = tmp;
    return pOut;
}

inline D3DXMATRIX* D3DXMatrixTranspose(D3DXMATRIX* pOut, const D3DXMATRIX* pM) {
    if (!pOut || !pM) return NULL;
    for (int r=0; r<4; r++) for (int c=0; c<4; c++) pOut->m[r][c] = pM->m[c][r];
    return pOut;
}

inline float D3DXVec3Dot(const D3DXVECTOR3* pV1, const D3DXVECTOR3* pV2) {
    return pV1->x*pV2->x + pV1->y*pV2->y + pV1->z*pV2->z;
}
inline float D3DXVec3Length(const D3DXVECTOR3* pV) {
    return sqrtf(pV->x*pV->x + pV->y*pV->y + pV->z*pV->z);
}
inline D3DXVECTOR3* D3DXVec3Normalize(D3DXVECTOR3* pOut, const D3DXVECTOR3* pV) {
    float len = D3DXVec3Length(pV);
    if (len > 0.0f) { pOut->x=pV->x/len; pOut->y=pV->y/len; pOut->z=pV->z/len; }
    return pOut;
}
inline D3DXVECTOR3* D3DXVec3Cross(D3DXVECTOR3* pOut, const D3DXVECTOR3* pV1, const D3DXVECTOR3* pV2) {
    D3DXVECTOR3 tmp;
    tmp.x = pV1->y*pV2->z - pV1->z*pV2->y;
    tmp.y = pV1->z*pV2->x - pV1->x*pV2->z;
    tmp.z = pV1->x*pV2->y - pV1->y*pV2->x;
    *pOut = tmp; return pOut;
}

#endif // D3DX8MATH_H_COMPAT
