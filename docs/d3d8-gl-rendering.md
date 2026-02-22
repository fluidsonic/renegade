# D3D8 to OpenGL Rendering Layer

## Overview

`original/compat/d3d8_gl.cpp` implements the D3D8 API (`IDirect3D8`, `IDirect3DDevice8`) backed by OpenGL 2.1 via SDL2. This lets the unmodified C++ game source (which calls D3D8) render through macOS OpenGL.

## Matrix Convention: D3D vs GL

### The game's internal convention

`Matrix4` (in `Code/WWMath/matrix4.h`) stores rows as `Vector4 Row[4]` — row-major storage — and applies matrices as column-vectors: `M * v`. This is the standard OpenGL convention.

### DX8Wrapper transpose

`DX8Wrapper::Set_Transform(D3DTS_PROJECTION, m)` and similar calls always transpose the `Matrix4` before passing to `IDirect3DDevice8::SetTransform`:

```cpp
DX8CALL(SetTransform(D3DTS_PROJECTION, (D3DMATRIX*)&m.Transpose()));
```

The D3D device therefore receives the matrix in D3D's row-vector convention (`v * M`). The `D3DMATRIX` stores elements as `_ij` = row i, col j.

### d3d_to_gl_matrix

When `Apply_GL_Transforms()` loads the stored D3D matrices into GL, it uses `d3d_to_gl_matrix`. The correct conversion is:

```
DX8Wrapper stores:   M_game  (GL convention, M*v)
SetTransform gets:   M_game^T  (D3D convention, v*M)
d3d_to_gl_matrix:   needs to recover M_game for GL

M_game = (M_game^T)^T = just copy the D3DMATRIX bytes directly
```

`glLoadMatrixf(float m[16])` reads column-major: `m[col*4+row]`. The `D3DMATRIX` stores row-major: `_ij` is element (row=i, col=j) at memory offset `(i-1)*4 + (j-1)`.

The correct formula:
```c
out[col*4 + row] = m->_(col+1)(row+1)
```

Which is equivalent to a direct memory copy of the 16 floats. The final `d3d_to_gl_matrix` implementation:

```c
out[0]  = m->_11;  out[1]  = m->_12;  out[2]  = m->_13;  out[3]  = m->_14;
out[4]  = m->_21;  out[5]  = m->_22;  out[6]  = m->_23;  out[7]  = m->_24;
out[8]  = m->_31;  out[9]  = m->_32;  out[10] = m->_33;  out[11] = m->_34;
out[12] = m->_41;  out[13] = m->_42;  out[14] = m->_43;  out[15] = m->_44;
```

**Common mistake**: the previous version swapped row/col indices, effectively double-transposing and producing wrong 3D transforms. For symmetric matrices (e.g., identity) this mistake is invisible; it only manifests with actual camera/projection matrices.

## 2D Rendering

`Render2DClass` (in `Code/ww3d2/render2d.cpp`) handles all 2D UI rendering:

1. Sets projection, world, view all to identity.
2. Converts pixel coordinates to D3D NDC via `Convert_Vert`:
   - `NDC_x = pixel_x * (2/W) - 1` (range -1 to +1, left to right)
   - `NDC_y = pixel_y * (-2/H) + 1` (range +1 to -1, top to bottom)
3. Uses FVF `D3DFVF_XYZ|D3DFVF_NORMAL|D3DFVF_TEX2|D3DFVF_DIFFUSE` (stride 44 bytes).
4. Vertex layout: XYZ@0, Normal@12, Diffuse@24, UV1@28, UV2@36.

## Texture Upload and UV Convention

`Upload_To_GL()` handles the D3D/GL texture coordinate mismatch:

- **D3D**: V=0 = top of texture, rows stored top-to-bottom in memory.
- **GL**: V=0 = bottom of texture, rows stored bottom-to-top for `glTexImage2D`.

Fix: upload rows in reverse order (flip vertically). Pair this with the UV transform `gl_v = 1.0f - d3d_v` in every draw call.

## Font Loading (`ARI_____.TTF`)

`stylemgr.ini` lists `ARI_____.TTF` (Arial MT) in the font file list. This file does NOT exist in any MIX archive. However:

- `AddFontResourceA` returns 0 when not found (expected, logs a message).
- `CreateFontA` maps "Arial MT" -> "Arial" which is a macOS system font.
- CoreText resolves "Arial" natively — no MIX extraction needed for this font.

`54251___.TTF` (Regatta Condensed LET) IS found in `data/always.dat` and is registered with CoreText for use as FONT_TITLE and FONT_MENU.

## Coordinate Systems Summary

| System | X | Y | Origin |
|--------|---|---|--------|
| D3D NDC | -1 (left) to +1 (right) | +1 (top) to -1 (bottom) | center |
| GL NDC | -1 (left) to +1 (right) | -1 (bottom) to +1 (top) | center |
| GL Viewport | 0 (left) to W (right) | 0 (bottom) to H (top) | bottom-left |
| D3D Viewport | 0 (left) to W (right) | 0 (top) to H (bottom) | top-left |
| SDL2 Window | 0 (left) to W (right) | 0 (top) to H (bottom) | top-left |

`SetViewport` compensates for the D3D vs GL Y-flip: `gl_y = height - (vp.Y + vp.Height)`.
