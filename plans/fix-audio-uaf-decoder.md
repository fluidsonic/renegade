# Fix: heap-use-after-free in audio — miniaudio decoder reads freed SoundBufferClass data

## Context

ASan crash: CoreAudio thread (T9) reads from WAV buffer via `ma_dr_wav__on_read_memory` after the delayed-release thread (T11) freed the `SoundBufferClass::m_Buffer` via `Free_Buffer()`.

The race happens because `AIL_end_sample` / `AIL_end_3D_sample` only call `ma_sound_stop()` (which is async — the audio thread may still be mid-read) but leave the `ma_decoder` initialized with a raw pointer to the buffer data. The delayed-release thread eventually frees the buffer, but the decoder still holds a dangling pointer.

## Root Cause

In `mss_impl.cpp`:
- `AIL_end_sample` (line 476) and `AIL_end_3D_sample` (line 769) called `ma_sound_stop` + `ma_sound_seek_to_pcm_frame` but did NOT call `uninit_sample2d`/`uninit_sample3d`
- `uninit_sample2d`/`uninit_sample3d` (lines 241-248) properly tear down the decoder: `ma_sound_uninit` + `ma_decoder_uninit`
- The buffer was freed ~2s later by the delayed-release thread, but the decoder still held a pointer to it

## Fix Applied

**File: `original/compat/mss_impl.cpp`**

1. `AIL_end_sample`: replaced `ma_sound_stop` + `ma_sound_seek_to_pcm_frame` with `uninit_sample2d(smp)` — calls `ma_sound_uninit` (synchronously removes node from engine graph) + `ma_decoder_uninit` (releases decoder's reference to buffer)

2. `AIL_end_3D_sample`: same — replaced with `uninit_sample3d(smp)`

`ma_sound_uninit` is synchronous: it removes the sound node from the engine's node graph, guaranteeing the audio callback thread will not access the decoder after it returns. The guard is changed from `!smp->sound_initialized` to just `!smp` since `uninit_*` are idempotent.

## Verification

1. Build: `cmake --build original/build --target Commando -j8` — clean
2. Run: `MallocNanoZone=0 original/build/Code/Commando/Commando.app/Contents/MacOS/Commando`
3. Load a level with sounds (menu → single player)
4. Confirm no ASan heap-use-after-free crash
5. Confirm sounds still play correctly in-game
