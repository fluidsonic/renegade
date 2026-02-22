# Audio System

## Overview

The audio system is a layered stack. Game code never calls audio hardware APIs directly — all
audio passes through the WWAudio library, which in turn calls Miles Sound System (MSS) `AIL_*`
functions. On the macOS port, those `AIL_*` calls are backed by a miniaudio implementation
(`compat/mss_impl.cpp`) instead of the original Windows MSS DLL.

```
Game code (Combat, Commando, Scripts)
        │
        ▼
  WWAudio library  (Code/WWAudio/)
        │  calls AIL_* functions
        ▼
  compat/mss.h     (extern declarations)
        │
        ▼
  compat/mss_impl.cpp  (miniaudio backend — macOS port)
        │
        ▼
  CoreAudio / AudioToolbox   (OS audio framework)
```

---

## WWAudio Class Hierarchy

```
SoundSceneObjClass
├── AudibleSoundClass          – base for all audible sounds
│   ├── Sound3DClass           – true 3D positional audio (H3DSAMPLE)
│   │   ├── Listener3DClass    – listener object (camera ear)
│   │   └── SoundPseudo3DClass – 2D sound with spatial positioning (HSAMPLE)
│   │       └── FilteredSoundClass – radio / "tinny" DSP effect
│   └── (sound handle wrappers)
└── LogicalSoundClass          – non-audible game trigger (no MSS handle)

WWAudioClass        – singleton manager: init, device selection, cache, playlists
SoundSceneClass     – spatial culling system; owns the listener and all scene sounds
SoundBufferClass    – reference-counted in-memory audio data (cached)
StreamSoundBufferClass – streaming audio (no data copy, reads via file callbacks)
SoundStreamHandleClass – wraps HSTREAM for streaming playback
```

---

## Initialisation Sequence

```
WWAudioClass::Initialize()
  AIL_startup()
  AIL_set_file_callbacks(open, close, seek, read)   ← MIX archive routing
  AIL_open_digital_driver() or AIL_waveOutOpen()    ← 2D driver
  AIL_enumerate_3D_providers() → pick provider
  AIL_open_3D_provider(provider)
  AIL_3D_open_listener(provider)                    ← listener object
  SoundSceneClass created
  Listener3DClass created and added to scene
```

---

## Sound Loading and Caching

1. Caller requests a sound by filename.
2. `WWAudioClass::Get_Sound_Buffer()` checks `m_CachedBuffers[hash]` first.
3. On cache miss, opens file via `FileFactory` (resolves path in MIX archives).
4. `AIL_WAV_info()` parses the WAV header to extract sample rate, channels, bits, data
   pointer, and size.
5. Buffer stored in hash table. Total cache limited to `m_MaxCacheSize` (default 1 MB).
6. `Free_Cache_Space()` purges least-recently-used entries when the cache is full.
7. Buffers are reference-counted; destroyed when refcount drops to 0.

Streaming sounds (`StreamSoundBufferClass`) store only metadata — audio data is never
copied to memory; MSS reads directly through the file callbacks.

---

## 2D Sound Playback

```
AudibleSoundClass::Play()
  AIL_allocate_sample_handle(driver)   → HSAMPLE
  AIL_set_named_sample_file(s, name, data, len, flags)
  AIL_set_sample_volume(s, 0-127)
  AIL_set_sample_pan(s, 0-127)         ← 64 = centre
  AIL_set_sample_loop_count(s, n)      ← 0 = infinite
  AIL_start_sample(s)

Polling per frame:
  AIL_sample_status(s)                 → SMP_PLAYING / SMP_STOPPED / SMP_DONE

Teardown:
  AIL_stop_sample(s)
  AIL_release_sample_handle(s)
```

---

## 3D Positional Audio

### Coordinate Transform

`Sound3DClass::Update_Miles_Transform()` converts game-world positions to the coordinate
system expected by MSS (and miniaudio):

```
Game world:  X = right,   Y = forward,  Z = up   (left-handed)
MSS/miniaudio: X = right, Y = up,       Z = forward (right-handed)

Transform:
  Build  world_to_listener = listener_transform.Inverse()
  Apply  listener_space_tm  = world_to_listener × sound_transform
  Swizzle: miles_pos = ( −tm.Y,  tm.Z,  tm.X )
  Velocity swizzle is identical: ( −vel.Y, vel.Z, vel.X )
```

This swizzle is baked into the game code and is **not** replicated in `mss_impl.cpp` —
the implementation passes coordinates through as-is.

### 3D Sound Lifecycle

```
AIL_allocate_3D_sample_handle(provider)  → H3DSAMPLE
AIL_set_3D_sample_file(s, data)          ← parses RIFF size from header
AIL_set_3D_sample_distances(s, min, max) ← attenuation radii
AIL_set_3D_sample_cone(s, inner, outer, outer_vol)
AIL_set_3D_sample_volume(s, 0-127)
AIL_set_3D_sample_loop_count(s, n)
AIL_start_3D_sample(s)

Per frame:
  AIL_set_3D_position(sample_handle, x, y, z)
  AIL_set_3D_velocity_vector(sample_handle, vx, vy, vz)
  AIL_3D_sample_status(s)

Teardown:
  AIL_end_3D_sample(s)
  AIL_release_3D_sample_handle(s)
```

### Listener

```
AIL_3D_open_listener(provider)          → H3DPOBJECT (sentinel = LISTENER_HANDLE)
AIL_set_3D_position(listener, x, y, z)
AIL_set_3D_orientation(listener, fx, fy, fz, ux, uy, uz)
AIL_set_3D_velocity(listener, vx, vy, vz, scalar)
```

The listener handle is shared with sound positioning via `H3DPOBJECT` — the implementation
distinguishes the listener from sound objects by comparing against `LISTENER_HANDLE`.

### Distance Attenuation

| Property | MSS function | miniaudio mapping |
|---|---|---|
| Max-volume radius | `AIL_set_3D_sample_distances(min, max)` | `ma_sound_set_min_distance` |
| Drop-off radius | same | `ma_sound_set_max_distance` |
| Directional cone | `AIL_set_3D_sample_cone(inner°, outer°, outer_vol)` | `ma_sound_set_cone` (radians) |
| Obstruction | `AIL_set_3D_sample_obstruction(0.0–1.0)` | volume multiplier: `vol × (1−obs) × (1−occ)` |
| Occlusion | `AIL_set_3D_sample_occlusion(0.0–1.0)` | same combined multiplier |

---

## Streaming Audio

Used for music and long ambient tracks that would exceed the 1 MB cache limit.

```
AIL_open_stream_by_sample(driver, sample, filename, type)  → HSTREAM
AIL_set_stream_volume(s, 0-127)
AIL_set_stream_pan(s, 0-127)
AIL_set_stream_loop_count(s, 0)   ← 0 = loop forever
AIL_start_stream(s)
AIL_pause_stream(s, 1/0)          ← 1 = pause, 0 = resume
AIL_stream_ms_position(s, &len, &pos)
AIL_close_stream(s)
```

The macOS implementation opens streams via a custom `ma_vfs` that routes
`open/read/seek/close` through the game's registered file callbacks, so streams work
directly out of MIX archives without extraction.

---

## MIX Archive File Callbacks

`AIL_set_file_callbacks()` registers four function pointers that MSS uses whenever it
needs to open a file (both for decoding memory-mapped buffers and for streaming):

| Callback | Signature | Implementation |
|---|---|---|
| open | `U32 (const char*, U32* size)` | `FileFactory::Get_File()` → returns `(U32)(uintptr_t)FileClass*` |
| close | `void (U32 handle)` | `FileFactory::Return_File()` |
| seek | `S32 (U32, S32 offset, U32 type)` | `FileClass::Seek()` |
| read | `U32 (U32, void* buf, U32 bytes)` | `FileClass::Read()` |

**64-bit handle caution:** MSS stores file handles as `U32`, but `FileClass*` is 8 bytes
on arm64. The game code already truncates the pointer to `U32`. The miniaudio VFS layer
(`MssVfsFile`) stores the `U32` handle and passes it back verbatim — it does **not**
dereference it as a pointer.

---

## Filtered Sound (Radio Effect)

`FilteredSoundClass` derives from `SoundPseudo3DClass` and adds a DSP filter to simulate
radio or telephone audio:

```
AIL_enumerate_filters(&next, &provider, &name)   ← find filter provider
AIL_open_3D_provider(filter_provider)
AIL_set_sample_processor(sample, DP_FILTER, filter_provider)
AIL_set_filter_sample_preference(sample, param_name, &value)
AIL_set_3D_room_type(provider, ENVIRONMENT_GENERIC)
```

On the macOS port `AIL_set_sample_processor` and `AIL_set_filter_sample_preference` are
currently no-ops (stubs). The sounds play without the filter effect; a proper reverb node
can be added later using miniaudio's node graph API.

---

## Volume and Pan Mapping

| MSS range | Meaning | miniaudio equivalent |
|---|---|---|
| 0–127 | volume (127 = 0 dB) | `vol / 127.0f` → 0.0–1.0 |
| 0–127 | pan (64 = centre) | `(2 × pan / 127) − 1` → −1.0–+1.0 |

---

## Timers

MSS timers drive periodic callbacks (e.g., the `On_Frame_Update` audio tick):

```
HTIMER t = AIL_register_timer(callback_fn);
AIL_set_timer_period(t, period_ms);
AIL_start_timer(t);
...
AIL_stop_timer(t);
AIL_release_timer_handle(t);
```

The macOS implementation runs each timer on a dedicated `pthread` that sleeps for
`period_ms` milliseconds between calls to the registered function.

---

## macOS Backend — mss_impl.cpp

`compat/mss_impl.cpp` (~1100 lines) implements all `AIL_*` functions via miniaudio.

### Pool Sizes

| Pool | Size | Handle range |
|---|---|---|
| 2D samples | 64 | 1–64 |
| 3D samples | 32 | 1–32 |
| Streams | 16 | 1–16 |
| Timers | 16 | 1–16 |

Handles are 1-based (`handle = array_index + 1`). `0` and `(HSAMPLE)-1` are treated as
invalid for 2D samples; `0` is invalid for 3D samples.

### Key Implementation Notes

- **Memory decode**: `AIL_set_named_sample_file` uses `ma_decoder_init_memory`; the caller
  must keep the data buffer alive for the decoder's lifetime (game already does this via
  `SoundBufferClass`).
- **3D file loading**: `AIL_set_3D_sample_file` determines buffer size from the RIFF header
  (`le32(buf+4) + 8`) because the MSS API does not pass a length parameter.
- **Finite loops**: miniaudio supports on/off looping only. Loops > 1 are handled by
  polling `ma_sound_at_end()` inside `AIL_sample_status()` and restarting with
  `ma_sound_seek_to_pcm_frame(0)`.
- **Listener sentinel**: `LISTENER_HANDLE = MAX_3D_SAMPLES + 1 = 33`. `AIL_set_3D_position`
  and friends check for this value to route to `ma_engine_listener_*` instead of
  `ma_sound_*`.
- **Pitch**: `AIL_set_sample_playback_rate(rate)` computes pitch as
  `rate / native_rate` and calls `ma_sound_set_pitch`.
- **Thread safety**: A global `pthread_mutex_t` protects pool allocation;
  `AIL_lock()`/`AIL_unlock()` acquire/release it.

### Build Integration

`Code/Commando/CMakeLists.txt`:
- `mss_impl.cpp` added as an explicit source with `SKIP_PRECOMPILE_HEADERS ON`
  (it defines `MINIAUDIO_IMPLEMENTATION` before including `miniaudio.h`).
- Linked frameworks: `-framework CoreAudio -framework AudioToolbox`.

---

## Threading Model

| Thread | Owner | Purpose |
|---|---|---|
| Main / render thread | Game | Calls `On_Frame_Update`, position updates, culling |
| CoreAudio I/O thread | miniaudio / OS | Pulls audio from miniaudio engine; never touches game state |
| Timer threads (per timer) | mss_impl | Periodic `AIL_*` timer callbacks |
| Delayed-release thread | WWAudioThreadsClass | Deferred object destruction to avoid main-thread stalls |

The CoreAudio thread runs entirely inside miniaudio and is invisible to game code.

---

## Save / Load

Sound state is serialised via the ChunkIO system:

| Class | Chunk ID | Persisted fields |
|---|---|---|
| `AudibleSoundClass` | `CHUNKID_AUDIBLE_SOUND` | volume, pan, loop count, type |
| `Sound3DClass` | `CHUNKID_SOUND3D` | position, velocity, radii, static flag |
| `SoundPseudo3DClass` | `CHUNKID_SOUNDPSEUDO3D` | pan, velocity |
| `LogicalSoundClass` | `CHUNKID_LOGICAL_SOUND` | dropoff radius, type mask |
| `SoundSceneClass` (static sounds) | `CHUNKID_STATIC_SAVELOAD` | all persistent 3D sounds |

Streaming playback position and the audio buffer data itself are **not** serialised;
sounds reload from disk on restore.
