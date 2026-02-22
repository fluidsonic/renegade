# GDI Font Compatibility Layer

## Overview

`original/compat/wingdi_coretext.cpp` implements the GDI text API subset used by `FontCharsClass` (`render2dsentence.cpp`), backed by CoreText/CoreGraphics on macOS.

## Font Loading: AddFontResource

`AddFontResourceA(filename)` is called from `stylemgr.cpp` for each entry in `stylemgr.ini` `[Font File List]`. It:

1. Tries the filename as a loose file (bare, then `data/` prefix)
2. Computes `CRC_Stringi(toupper(filename))` and binary-searches all MIX archives in `data/`
3. Writes extracted bytes to `<exe_dir>/fonts/<filename>` and registers with CoreText via `CTFontManagerRegisterFontsForURL`

## Fonts in always.dat

| MIX filename     | Actual font             | CRC         |
|------------------|-------------------------|-------------|
| `54251___.TTF`   | Regatta Condensed LET   | 0xD61D626C  |
| `frangcmi.ttf`   | FranklinGotCdMdITCTT    | 0x39DFA19F  |

`54251` is the Windows font ID for Regatta Condensed LET. Westwood stored TTF files under their Windows font IDs.

## ARI_____.TTF: Not In the MIX

`stylemgr.ini` lists `File02=ARI_____.TTF`, but this file is **not present in any MIX archive**. This is intentional: Arial MT (`ARI_____.TTF`) was a standard Windows system font in 2002 and was never shipped in the game data.

- CRC of `ARI_____.TTF` = 0x4BB54340 — absent from `always.dat` index
- `AddFontResourceA` logs "expected for system fonts like Arial" and returns 0
- `CreateFontA("Arial MT", ...)` maps to macOS system "Arial" via the explicit mapping in `wingdi_coretext.cpp`

## stylemgr.ini Font Configuration

Extracted from `always.dat` (1148 bytes):

```ini
[Font File List]
File01=54251___.TTF      ; Regatta Condensed LET
File02=ARI_____.TTF      ; Arial MT — NOT in MIX, uses macOS system Arial

[Font Names]
FONT_TITLE=Regatta Condensed LET, 52, 0
FONT_LG_CONTROLS=Arial MT, 12, 1
FONT_CONTROLS=Arial MT, 8, 1
FONT_LISTS=Arial MT, 8, 0
FONT_TOOLTIPS=Arial MT, 8, 0
FONT_MENU=Regatta Condensed LET, 32, 0
FONT_SM_MENU=Regatta Condensed LET, 20, 0
FONT_HEADER=Arial MT, 9, 1
FONT_BIG_HEADER=Arial MT, 12, 1
FONT_CREDITS=Arial MT, 10, 0
FONT_CREDITS_BOLD=Arial MT, 10, 1
FONT_INGAME_TXT=Arial MT, 8, 0
FONT_INGAME_BIG_TXT=Arial MT, 16, 0
FONT_INGAME_SUBTITLE_TXT=Arial MT, 14, 0
FONT_INGAME_HEADER_TXT=Arial MT, 9, 1
```

## CRC Algorithm

The MIX index is keyed by CRC32 of the uppercased filename:

```c
uint32_t mix_crc(const char* name) {
    uint32_t crc = 0xFFFFFFFF;
    while (*name) {
        unsigned char c = toupper((unsigned char)*name++);
        crc = table[(crc ^ c) & 0xFF] ^ (crc >> 8);
    }
    return crc ^ 0xFFFFFFFF;
}
```

This is standard CRC32 (IEEE 802.3 polynomial 0xEDB88320 reflected) with 0xFFFFFFFF init and final XOR — identical to `zlib.crc32` / Python `binascii.crc32`.

## MIX File Format (always.dat)

```
[0-3]   "MIX1"
[4-7]   int32 header_offset  (absolute, points to index section)
[8-11]  int32 names_offset   (absolute, points to names section)
[12-15] int32 unused

At header_offset:
  int32 file_count
  file_count × { uint32 crc, uint32 offset, uint32 size }  (sorted by CRC)

At names_offset:
  int32 name_count
  name_count × { uint8 name_len, char[name_len] name }     (NUL-terminated within name_len)
```

`always.dat` contains 15,161 files. Entry offsets are absolute file positions.
