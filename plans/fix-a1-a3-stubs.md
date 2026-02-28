# Plan: Fix A1-A3 Stub Implementations

## Context

From the stub inventory, three items were flagged as potentially impactful:
- **A1 (GDI Font Rendering)**: Already fully implemented via CoreText in `wingdi_coretext.cpp` — no work needed
- **A2 (D3DX Texture Loading)**: 3 D3DX functions are called but stub to `E_NOTIMPL`
- **A3 (Audio Reverb/Filter)**: Tinny effect is completely stubbed in `mss_impl.cpp`

## A2: D3DX Texture/Surface Operations

### Problem
Three D3DX functions are called and return `E_NOTIMPL`:

1. **`D3DXCreateTextureFromFileExA`** — 1 call site (`dx8wrapper.cpp:1931`). Legacy path that loads textures from loose files. Falls back to missing texture on failure. The main loading pipeline (`DDSFileClass` → `TextureLoadTaskClass`) works fine without it.

2. **`D3DXLoadSurfaceFromSurface`** — 4 call sites:
   - `dx8wrapper.cpp:1981` — texture-from-surface creation
   - `surfaceclass.cpp:446` — `SurfaceClass::Copy()` fallback when format/size mismatch
   - `surfaceclass.cpp:487` — `SurfaceClass::Stretch_Copy()` for resizing
   - `missingtexture.cpp:91` — mipmap generation for the procedural missing texture

3. **`D3DXFilterTexture`** — 1 call site (`dx8wrapper.cpp:1986`). Auto-generates mipmaps after surface-to-texture copy.

### Implementation

Implement `D3DXLoadSurfaceFromSurface` as a software surface blitter:
- Lock both surfaces via `LockRect()`
- Use `BitmapHandlerClass::Copy_Image()` (already exists in `original/Code/ww3d2/bitmaphandler.cpp`) for format conversion
- Handle different source/dest sizes with nearest-neighbor or box filter downsampling
- Palette args are always NULL, color key is always 0 — no need to handle those

Implement `D3DXFilterTexture` as iterative mipmap generation:
- For each level N (1..max): lock level N-1 and level N, box-filter downsample, unlock
- Use the existing `BitmapHandlerClass` pixel format conversion utilities

Implement `D3DXCreateTextureFromFileExA`:
- Parse the file extension (.tga, .dds, .bmp)
- For DDS: reuse `DDSFileClass` to load and copy levels
- For TGA: reuse `Targa` class (already in `Code/wwlib/targa.h`)
- Create the texture via `pDevice->CreateTexture()`, lock surfaces, copy data, unlock
- Handle `D3DX_DEFAULT` width/height (use image dimensions)

### Files to modify
- `original/compat/D3dx8core.h` — replace stub implementations
- New: `original/compat/d3dx8_impl.cpp` — implementation file for D3DX functions
- `original/CMakeLists.txt` — add new .cpp to build

### Files to reference
- `original/Code/ww3d2/bitmaphandler.cpp` — `BitmapHandlerClass::Copy_Image()` for format conversion
- `original/Code/ww3d2/ddsfile.cpp` — DDS loading
- `original/Code/ww3d2/surfaceclass.cpp` — surface lock/unlock patterns
- `original/compat/d3d8_gl.cpp` — GL texture/surface implementation (`D3D8Texture_GL`, `D3D8Surface_GL`)

---

## A3: Audio Reverb/Filter (Tinny Effect)

### Problem
`FilteredSoundClass` applies a "tinny" reverb to 2D samples for radio/speaker sounds. The filter system is completely stubbed:
- `AIL_enumerate_filters()` returns 0 → `m_ReverbFilter = INVALID_MILES_HANDLE`
- `AIL_set_sample_processor()` is a no-op
- `AIL_set_filter_sample_preference()` is a no-op
- `AIL_set_3D_sample_effects_level()` is a no-op

### Original Behavior
`FilteredSoundClass::Initialize_Miles_Handle()` sets 3 reverb parameters:
- `"Reverb level"` = 0.3 (wet/dry mix)
- `"Reverb reflect time"` = 0.01 (10ms early reflections)
- `"Reverb decay time"` = 0.535 (535ms decay tail)

### Implementation

Use miniaudio's `ma_delay_node` + `ma_hpf_node` to create the tinny effect:

1. **`AIL_enumerate_filters()`** — return 1, provide a dummy HPROVIDER handle and name "Reverb"

2. **Add filter state to `Sample2D`**:
   ```
   struct SampleFilter {
       bool active;
       ma_delay_node delay;
       ma_hpf_node hpf;
       float reverb_level;     // wet mix (default 0.3)
       float reflect_time;     // delay in seconds (default 0.01)
       float decay_time;       // feedback decay (default 0.535)
   };
   ```

3. **`AIL_set_sample_processor()`** — when `proc_type == DP_FILTER`:
   - Initialize `ma_delay_node` with default parameters
   - Initialize `ma_hpf_node` with cutoff ~2000 Hz for tinny quality
   - Rewire the node graph: sound → hpf → delay → endpoint
   - Mark the filter as active on this sample

4. **`AIL_set_filter_sample_preference()`** — map preference strings:
   - `"Reverb level"` → `ma_delay_node_set_wet(node, value)` + `ma_delay_node_set_dry(node, 1.0 - value)`
   - `"Reverb reflect time"` → update delay frames: `value * sampleRate`
   - `"Reverb decay time"` → `ma_delay_node_set_decay(node, value)`

5. **Cleanup** — when sample is released (`AIL_release_sample_handle`), uninit any active filter nodes

6. **`AIL_set_3D_sample_effects_level()`** — optional/low priority since it was EAX-only and the game sets `m_EffectsLevel = 0.0` for non-EAX drivers

### Files to modify
- `original/compat/mss_impl.cpp` — implement the 4 filter functions + add `SampleFilter` struct
- `original/compat/mss.h` — no changes needed (declarations already exist)

### Files to reference
- `original/compat/miniaudio.h` — `ma_delay_node`, `ma_hpf_node` APIs
- `original-untouched/Code/WWAudio/FilteredSound.cpp` — original call pattern

---

## Verification

1. **Build**: `cmake --build original/build --target Commando -j8`
2. **Run**: `MallocNanoZone=0 original/build/Code/Commando/Commando.app/Contents/MacOS/Commando`
3. **A2 verification**: Check that the missing texture has proper mipmaps (no white/black mip levels at distance). Test any code path that loads textures from loose files.
4. **A3 verification**: Listen for filtered/tinny sounds in-game (radio communications, speaker announcements). Compare with the flat non-reverb sound.
