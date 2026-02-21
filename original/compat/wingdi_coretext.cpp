// wingdi_coretext.cpp — GDI text rendering via CoreText/CoreGraphics for macOS
// Implements the subset of GDI used by FontCharsClass (render2dsentence.cpp):
//   CreateFont / CreateDIBSection / CreateCompatibleDC / SelectObject / DeleteObject
//   DeleteDC / SetBkColor / SetTextColor / GetTextMetrics / GetTextExtentPoint32W
//   ExtTextOutW / GetDC / ReleaseDC
// Also implements AddFontResource / RemoveFontResource which load font bytes
// from MIX archives via _TheFileFactory and register them with CoreText.

#include <CoreText/CoreText.h>
#include <CoreGraphics/CoreGraphics.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include <math.h>
#include <stdio.h>
#include <sys/stat.h>
#include <mach-o/dyld.h>
#include <vector>
#include <string>

// Pull in compat types
#include "windef.h"
#include "wingdi.h"    // BITMAPINFO, TEXTMETRIC, HGDIOBJ, etc.
#include "winuser.h"   // HWND, SIZE, RECT

// MIX file factory — needed for AddFontResource implementation
#include "ffactory.h"  // FileFactoryClass, _TheFileFactory
#include "wwfile.h"    // FileClass

// ---- Type tags to distinguish handles ----
enum FakeGDITag : int {
    TAG_FONT   = 0x464F4E54,  // 'FONT'
    TAG_BITMAP = 0x42494D50,  // 'BIMP'
    TAG_DC     = 0x48444300,  // 'HDC\0'
};

// ---- Font object ----
struct FakeFont {
    FakeGDITag tag;      // must be first field = TAG_FONT
    CTFontRef  ctfont;
    int        height;
};

// ---- Bitmap object (24-bit BGR buffer for game, 32-bit BGRA for CG) ----
struct FakeBitmap {
    FakeGDITag   tag;        // must be first field = TAG_BITMAP
    CGContextRef ctx;
    uint8_t*     cg_buf;     // 32-bit BGRA, w*h*4 bytes
    uint8_t*     gdi_buf;    // 24-bit BGR, stride DWORD-aligned  ← GDIBitmapBits
    int          width;
    int          height;
    int          gdi_stride;
};

// ---- Device context ----
struct FakeDC {
    FakeGDITag  tag;         // must be first field = TAG_DC
    FakeFont*   font;
    FakeBitmap* bitmap;
    COLORREF    text_color;
    COLORREF    bk_color;
};

// A dummy sentinel returned as "old object" when DC has no previously selected obj
static struct { FakeGDITag tag; } s_dummy = { TAG_FONT };

// ---- Copy CG (32-bit BGRA) → GDI (24-bit BGR), row-flipped for top-down DIB ----
static void sync_gdi_buf(FakeBitmap* bmp) {
    for (int row = 0; row < bmp->height; row++) {
        // CG origin is bottom-left; GDI (top-down DIB) origin is top-left → flip rows
        const uint8_t* src = bmp->cg_buf + (size_t)(bmp->height - 1 - row) * bmp->width * 4;
        uint8_t*       dst = bmp->gdi_buf + (size_t)row * bmp->gdi_stride;
        for (int col = 0; col < bmp->width; col++) {
            // kCGBitmapByteOrder32Little | kCGImageAlphaNoneSkipFirst → BGRA memory order
            dst[0] = src[0];  // B
            dst[1] = src[1];  // G
            dst[2] = src[2];  // R
            src += 4;
            dst += 3;
        }
    }
}

// ---- GetDC / ReleaseDC ----
// The game only uses the screen DC as a parameter to CreateDIBSection; return a
// sentinel that is never dereferenced.
static FakeDC s_screen_dc = { TAG_DC, nullptr, nullptr, 0x00FFFFFF, 0x00000000 };

HDC GetDC(HWND /*wnd*/) { return (HDC)&s_screen_dc; }
int ReleaseDC(HWND /*wnd*/, HDC /*dc*/) { return 1; }

// ---- CreateCompatibleDC / DeleteDC ----
HDC CreateCompatibleDC(HDC /*src*/) {
    FakeDC* dc = (FakeDC*)calloc(1, sizeof(FakeDC));
    dc->tag        = TAG_DC;
    dc->text_color = 0x00FFFFFF;  // white
    dc->bk_color   = 0x00000000;  // black
    return (HDC)dc;
}

BOOL DeleteDC(HDC hdc) {
    if (!hdc) return TRUE;
    FakeDC* dc = (FakeDC*)hdc;
    if (dc == &s_screen_dc) return TRUE;  // don't free static screen DC
    free(dc);
    return TRUE;
}

// ---- CreateFont ----
HFONT CreateFontA(int cHeight, int /*cWidth*/, int /*cEsc*/, int /*cOri*/,
                  int cWeight, DWORD bItalic, DWORD /*bUnder*/, DWORD /*bStrike*/,
                  DWORD /*iCharSet*/, DWORD /*iOutPrec*/, DWORD /*iClipPrec*/,
                  DWORD /*iQuality*/, DWORD /*iPitchFam*/, LPCSTR pszFaceName)
{
    FakeFont* f = (FakeFont*)calloc(1, sizeof(FakeFont));
    f->tag    = TAG_FONT;
    f->height = abs(cHeight);

    // Build a font descriptor matching the requested face + weight
    const char* name = (pszFaceName && pszFaceName[0]) ? pszFaceName : "Helvetica";

    // Map Windows face names to macOS equivalents
    const char* mapped = name;
    if (strcmp(name, "Arial MT") == 0 || strcmp(name, "Arial") == 0)
        mapped = "Arial";
    else if (strcmp(name, "MS Sans Serif") == 0 || strcmp(name, "Tahoma") == 0)
        mapped = "Helvetica";
    // "Regatta Condensed LET" is used for C&C Renegade menu titles (FONT_TITLE etc.).
    // If AddFontResource successfully registered the TTF from the MIX archive, CoreText
    // will find it by its PostScript/family name directly.  If registration failed or the
    // font was never registered, fall back to "Impact" as the closest condensed display
    // font available on macOS.
    // We try the requested name first; CTFontCreateWithName falls back to a system font
    // if the name is not found, so we can't easily detect failure here.  The Impact
    // fallback is kept only if needed — leave as direct pass-through.
    // (No explicit remapping needed: if the font registered successfully the name works.)

    CFStringRef cf_name = CFStringCreateWithCString(kCFAllocatorDefault, mapped,
                                                    kCFStringEncodingASCII);
    CGFloat pt_size = f->height > 0 ? (CGFloat)f->height : 12.0;

    // Build symbolic traits for bold/italic
    CTFontSymbolicTraits traits = 0;
    if (cWeight >= 700) traits |= kCTFontTraitBold;
    if (bItalic)         traits |= kCTFontTraitItalic;

    if (traits) {
        CFNumberRef trait_num = CFNumberCreate(kCFAllocatorDefault, kCFNumberIntType, &traits);
        CFStringRef trait_key = kCTFontSymbolicTrait;
        CFDictionaryRef trait_dict = CFDictionaryCreate(kCFAllocatorDefault,
            (const void**)&trait_key, (const void**)&trait_num, 1,
            &kCFTypeDictionaryKeyCallBacks, &kCFTypeDictionaryValueCallBacks);
        CFRelease(trait_num);

        CFStringRef desc_keys[] = { kCTFontFamilyNameAttribute, kCTFontTraitsAttribute };
        CFTypeRef   desc_vals[] = { cf_name, trait_dict };
        CFDictionaryRef desc_dict = CFDictionaryCreate(kCFAllocatorDefault,
            (const void**)desc_keys, (const void**)desc_vals, 2,
            &kCFTypeDictionaryKeyCallBacks, &kCFTypeDictionaryValueCallBacks);
        CFRelease(trait_dict);

        CTFontDescriptorRef desc = CTFontDescriptorCreateWithAttributes(desc_dict);
        CFRelease(desc_dict);
        f->ctfont = CTFontCreateWithFontDescriptor(desc, pt_size, nullptr);
        CFRelease(desc);
    } else {
        f->ctfont = CTFontCreateWithName(cf_name, pt_size, nullptr);
    }
    CFRelease(cf_name);

    return (HFONT)f;
}

// ---- CreateDIBSection ----
// The game requests a 24-bit top-down DIB (biHeight < 0) and reads GDIBitmapBits directly.
HBITMAP CreateDIBSection(HDC /*dc*/, const BITMAPINFO* bmi, UINT /*usage*/,
                          void** ppvBits, HANDLE /*hSection*/, DWORD /*offset*/)
{
    int w = (int)bmi->bmiHeader.biWidth;
    int h = (int)bmi->bmiHeader.biHeight;
    if (h < 0) h = -h;  // top-down DIB uses negative height

    FakeBitmap* bmp = (FakeBitmap*)calloc(1, sizeof(FakeBitmap));
    bmp->tag    = TAG_BITMAP;
    bmp->width  = w;
    bmp->height = h;

    // 32-bit BGRA CG context for rendering
    CGColorSpaceRef cs = CGColorSpaceCreateDeviceRGB();
    bmp->cg_buf = (uint8_t*)calloc((size_t)w * h, 4);
    bmp->ctx = CGBitmapContextCreate(bmp->cg_buf, (size_t)w, (size_t)h, 8,
                                     (size_t)w * 4, cs,
                                     kCGImageAlphaNoneSkipFirst | kCGBitmapByteOrder32Little);
    CGColorSpaceRelease(cs);

    // 24-bit BGR GDI-compatible buffer exposed to the game
    bmp->gdi_stride = ((w * 3) + 3) & ~3;
    bmp->gdi_buf    = (uint8_t*)calloc((size_t)h, (size_t)bmp->gdi_stride);

    if (ppvBits) *ppvBits = bmp->gdi_buf;
    return (HBITMAP)bmp;
}

// ---- SelectObject ----
HGDIOBJ SelectObject(HDC hdc, HGDIOBJ obj) {
    if (!hdc || !obj) return (HGDIOBJ)&s_dummy;
    FakeDC* dc = (FakeDC*)hdc;
    if (dc == &s_screen_dc) return (HGDIOBJ)&s_dummy;

    FakeGDITag tag = *(FakeGDITag*)obj;
    if (tag == TAG_FONT) {
        HGDIOBJ old = dc->font ? (HGDIOBJ)dc->font : (HGDIOBJ)&s_dummy;
        dc->font = (FakeFont*)obj;
        return old;
    }
    if (tag == TAG_BITMAP) {
        HGDIOBJ old = dc->bitmap ? (HGDIOBJ)dc->bitmap : (HGDIOBJ)&s_dummy;
        dc->bitmap = (FakeBitmap*)obj;
        return old;
    }
    return (HGDIOBJ)&s_dummy;
}

// ---- DeleteObject ----
BOOL DeleteObject(HGDIOBJ obj) {
    if (!obj || obj == (HGDIOBJ)&s_dummy) return TRUE;
    FakeGDITag tag = *(FakeGDITag*)obj;
    if (tag == TAG_FONT) {
        FakeFont* f = (FakeFont*)obj;
        if (f->ctfont) CFRelease(f->ctfont);
        free(f);
        return TRUE;
    }
    if (tag == TAG_BITMAP) {
        FakeBitmap* b = (FakeBitmap*)obj;
        if (b->ctx) CGContextRelease(b->ctx);
        free(b->cg_buf);
        free(b->gdi_buf);
        free(b);
        return TRUE;
    }
    return FALSE;
}

// ---- SetBkColor / SetTextColor ----
COLORREF SetBkColor(HDC hdc, COLORREF c) {
    if (!hdc) return 0;
    FakeDC* dc = (FakeDC*)hdc;
    COLORREF old = dc->bk_color;
    dc->bk_color = c;
    return old;
}
COLORREF SetTextColor(HDC hdc, COLORREF c) {
    if (!hdc) return 0;
    FakeDC* dc = (FakeDC*)hdc;
    COLORREF old = dc->text_color;
    dc->text_color = c;
    return old;
}

// ---- GetTextMetrics ----
BOOL GetTextMetricsA(HDC hdc, TEXTMETRIC* tm) {
    if (!tm) return FALSE;
    memset(tm, 0, sizeof(*tm));

    FakeDC* dc = hdc ? (FakeDC*)hdc : nullptr;
    CTFontRef font = (dc && dc->font) ? dc->font->ctfont : nullptr;

    if (font) {
        CGFloat ascent  = CTFontGetAscent(font);
        CGFloat descent = CTFontGetDescent(font);
        CGFloat leading = CTFontGetLeading(font);
        tm->tmAscent  = (LONG)ceil(ascent);
        tm->tmDescent = (LONG)ceil(descent);
        tm->tmHeight  = (LONG)(ceil(ascent) + ceil(descent) + (LONG)ceil(leading));
        CGFloat cap_h = CTFontGetCapHeight(font);
        tm->tmAveCharWidth = (LONG)(cap_h * 0.5);
        tm->tmMaxCharWidth = (LONG)cap_h;
    } else {
        tm->tmHeight  = 16;
        tm->tmAscent  = 13;
        tm->tmDescent = 3;
    }
    return TRUE;
}

// ---- GetTextExtentPoint32W ----
BOOL GetTextExtentPoint32W(HDC hdc, const WCHAR* str, int c, SIZE* size) {
    if (!size) return FALSE;
    size->cx = 0;
    size->cy = 16;

    FakeDC* dc = hdc ? (FakeDC*)hdc : nullptr;
    CTFontRef font = (dc && dc->font) ? dc->font->ctfont : nullptr;
    if (!font || c <= 0 || !str) {
        size->cx = c * 8;
        return TRUE;
    }

    CGFloat total_adv = 0.0;
    CGFloat height = CTFontGetAscent(font) + CTFontGetDescent(font);
    for (int i = 0; i < c; i++) {
        UniChar ch = (UniChar)str[i];
        CGGlyph glyph = 0;
        if (CTFontGetGlyphsForCharacters(font, &ch, &glyph, 1) && glyph) {
            CGSize adv = CGSizeZero;
            CTFontGetAdvancesForGlyphs(font, kCTFontOrientationDefault, &glyph, &adv, 1);
            total_adv += adv.width;
        } else {
            total_adv += height * 0.5;  // fallback
        }
    }
    size->cx = (LONG)ceil(total_adv);
    size->cy = (LONG)ceil(height);
    return TRUE;
}

BOOL GetTextExtentPoint32A(HDC hdc, LPCSTR str, int c, SIZE* size) {
    if (!size) return FALSE;
    if (c <= 0 || !str) { size->cx = 0; size->cy = 16; return TRUE; }
    // Convert ASCII to wide for the W version
    WCHAR wbuf[256];
    if (c > 255) c = 255;
    for (int i = 0; i < c; i++) wbuf[i] = (WCHAR)(unsigned char)str[i];
    return GetTextExtentPoint32W(hdc, wbuf, c, size);
}

// ---- ExtTextOutW ----
BOOL ExtTextOutW(HDC hdc, int x, int y, UINT options,
                 const RECT* lprect, const WCHAR* str, UINT c, const INT* /*dx*/)
{
    FakeDC* dc = hdc ? (FakeDC*)hdc : nullptr;
    if (!dc || dc == &s_screen_dc) return TRUE;
    FakeBitmap* bmp = dc->bitmap;
    if (!bmp || !bmp->ctx) return TRUE;

    CGContextRef ctx = bmp->ctx;

    // 1. Fill background (ETO_OPAQUE or just fill the character cell)
    {
        COLORREF bg = dc->bk_color;
        CGFloat r = ((bg >>  0) & 0xFF) / 255.0;
        CGFloat g = ((bg >>  8) & 0xFF) / 255.0;
        CGFloat b = ((bg >> 16) & 0xFF) / 255.0;
        CGContextSetRGBFillColor(ctx, r, g, b, 1.0);

        if (lprect && (options & 0x0002 /*ETO_OPAQUE*/)) {
            // CG origin is bottom-left; transform rect from GDI top-down to CG
            CGFloat cg_y = bmp->height - lprect->bottom;
            CGFloat cg_h = lprect->bottom - lprect->top;
            CGFloat cg_x = lprect->left;
            CGFloat cg_w = lprect->right - lprect->left;
            CGContextFillRect(ctx, CGRectMake(cg_x, cg_y, cg_w, cg_h));
        } else {
            CGContextFillRect(ctx, CGRectMake(0, 0, bmp->width, bmp->height));
        }
    }

    // 2. Draw characters
    if (c > 0 && str && dc->font && dc->font->ctfont) {
        CTFontRef font = dc->font->ctfont;

        COLORREF fg = dc->text_color;
        CGFloat r = ((fg >>  0) & 0xFF) / 255.0;
        CGFloat g = ((fg >>  8) & 0xFF) / 255.0;
        CGFloat b = ((fg >> 16) & 0xFF) / 255.0;

        // Build attributed string
        CFStringRef cf_str = CFStringCreateWithBytes(kCFAllocatorDefault,
                                 (const UInt8*)str, (CFIndex)(c * sizeof(UniChar)),
                                 kCFStringEncodingUTF16LE, false);
        if (cf_str) {
            // Create foreground color for CoreText (CTLineDraw ignores CGContextSetRGBFillColor)
            CGColorSpaceRef color_space = CGColorSpaceCreateDeviceRGB();
            CGFloat fg_comps[] = { r, g, b, 1.0 };
            CGColorRef fg_color = CGColorCreate(color_space, fg_comps);
            CGColorSpaceRelease(color_space);

            CFStringRef attr_keys[] = { kCTFontAttributeName, kCTForegroundColorAttributeName };
            CFTypeRef   attr_vals[] = { font, fg_color };
            CFDictionaryRef attrs = CFDictionaryCreate(kCFAllocatorDefault,
                (const void**)attr_keys, (const void**)attr_vals, 2,
                &kCFTypeDictionaryKeyCallBacks, &kCFTypeDictionaryValueCallBacks);
            CGColorRelease(fg_color);

            CFAttributedStringRef as = CFAttributedStringCreate(kCFAllocatorDefault, cf_str, attrs);
            CFRelease(cf_str);
            CFRelease(attrs);

            CTLineRef line = CTLineCreateWithAttributedString(as);
            CFRelease(as);

            // GDI y=0 is top; CoreText y is from bottom of bitmap.
            // Baseline = (bitmap_height - y) - ascent
            CGFloat ascent = CTFontGetAscent(font);
            CGFloat cg_x   = (CGFloat)x;
            CGFloat cg_y   = (CGFloat)(bmp->height - y) - ascent;

            CGContextSetTextMatrix(ctx, CGAffineTransformIdentity);
            CGContextSetTextPosition(ctx, cg_x, cg_y);
            CTLineDraw(line, ctx);
            CFRelease(line);
        }
    }

    // 3. Sync the 32-bit CG buffer → 24-bit GDI buffer
    sync_gdi_buf(bmp);
    return TRUE;
}

BOOL ExtTextOutA(HDC hdc, int x, int y, UINT options, const RECT* rect,
                 LPCSTR str, UINT c, const INT* dx)
{
    if (!str || c == 0) return ExtTextOutW(hdc, x, y, options, rect, nullptr, 0, dx);
    WCHAR wbuf[256];
    if (c > 255) c = 255;
    for (UINT i = 0; i < c; i++) wbuf[i] = (WCHAR)(unsigned char)str[i];
    return ExtTextOutW(hdc, x, y, options, rect, wbuf, c, dx);
}

// ============================================================
// AddFontResource / RemoveFontResource
// ============================================================
// Extracts font bytes from MIX archives via _TheFileFactory, writes them to
// a temporary file inside <exe_dir>/fonts/, then registers the font with
// CoreText (CTFontManagerRegisterFontsForURL, process scope).
// ============================================================

// Return "<exe_dir>/fonts/" — created on first call.
static std::string get_font_dir() {
    char exe[4096];
    uint32_t sz = sizeof(exe);
    if (_NSGetExecutablePath(exe, &sz) != 0) return "";

    std::string dir(exe);
    size_t slash = dir.rfind('/');
    if (slash == std::string::npos) return "";
    dir = dir.substr(0, slash + 1) + "fonts/";

    // Create the directory if it doesn't exist (ignore errors if already present).
    mkdir(dir.c_str(), 0755);
    return dir;
}

int AddFontResourceA(LPCSTR lpFilename) {
    if (!lpFilename || !_TheFileFactory) {
        fprintf(stderr, "[AddFontResource] No file factory: %s\n", lpFilename ? lpFilename : "(null)");
        return 0;
    }

    // 1. Get file object from MIX factory.
    FileClass* f = _TheFileFactory->Get_File(lpFilename);
    if (!f) {
        fprintf(stderr, "[AddFontResource] Not found in MIX: %s\n", lpFilename);
        return 0;
    }

    // 2. Open directly — do NOT use Is_Available(). Is_Available() does a raw fopen() on
    //    the bare filename relative to CWD, which fails for entries inside MIX archives.
    //    Open() on a biased RawFileClass uses the already-open MIX file handle and works.
    {
        char cwd[1024]; getcwd(cwd, sizeof(cwd));
        fprintf(stderr, "[AddFontResource] CWD=%s Got file for %s, Is_Available=%d\n", cwd, lpFilename, (int)f->Is_Available());
    }
    if (!f->Open(FileClass::READ)) {
        _TheFileFactory->Return_File(f);
        fprintf(stderr, "[AddFontResource] Cannot open: %s (errno=%d: %s)\n", lpFilename, errno, strerror(errno));
        return 0;
    }

    int size = f->Size();
    if (size <= 0) {
        f->Close();
        _TheFileFactory->Return_File(f);
        fprintf(stderr, "[AddFontResource] Empty file: %s (size=%d)\n", lpFilename, size);
        return 0;
    }

    std::vector<uint8_t> buf((size_t)size);
    int nread = f->Read(buf.data(), size);
    f->Close();
    _TheFileFactory->Return_File(f);

    if (nread != size) {
        fprintf(stderr, "[AddFontResource] Read error: %s (read=%d expected=%d)\n", lpFilename, nread, size);
        return 0;
    }

    // 3. Write bytes to a temp file under <exe_dir>/fonts/.
    std::string font_dir = get_font_dir();
    if (font_dir.empty()) {
        fprintf(stderr, "[AddFontResource] Cannot determine font dir\n");
        return 0;
    }
    std::string font_path = font_dir + lpFilename;

    FILE* tmp = fopen(font_path.c_str(), "wb");
    if (!tmp) {
        fprintf(stderr, "[AddFontResource] Cannot write font file: %s\n", font_path.c_str());
        return 0;
    }
    fwrite(buf.data(), 1, (size_t)size, tmp);
    fclose(tmp);

    // 4. Register the font file with CoreText (process scope).
    CFStringRef path_str = CFStringCreateWithCString(NULL, font_path.c_str(), kCFStringEncodingUTF8);
    CFURLRef url = CFURLCreateWithFileSystemPath(NULL, path_str, kCFURLPOSIXPathStyle, false);
    CFRelease(path_str);

    CFErrorRef err = nullptr;
    bool ok = CTFontManagerRegisterFontsForURL(url, kCTFontManagerScopeProcess, &err);
    CFRelease(url);

    if (!ok || err) {
        if (err) {
            CFStringRef desc = CFErrorCopyDescription(err);
            char dbuf[256] = "";
            CFStringGetCString(desc, dbuf, sizeof(dbuf), kCFStringEncodingUTF8);
            CFRelease(desc);
            CFRelease(err);
            fprintf(stderr, "[AddFontResource] CTFontManager failed for %s: %s\n", lpFilename, dbuf);
        } else {
            fprintf(stderr, "[AddFontResource] CTFontManager failed for %s\n", lpFilename);
        }
        return 0;
    }

    fprintf(stderr, "[AddFontResource] Registered font: %s (%d bytes)\n", lpFilename, size);
    return 1;
}

int AddFontResourceW(LPCWSTR lpFilename) {
    if (!lpFilename) return 0;
    char buf[256];
    size_t i = 0;
    while (lpFilename[i] && i < 255) { buf[i] = (char)lpFilename[i]; i++; }
    buf[i] = '\0';
    return AddFontResourceA(buf);
}

BOOL RemoveFontResourceA(LPCSTR lpFilename) {
    if (!lpFilename) return FALSE;

    std::string font_dir = get_font_dir();
    if (font_dir.empty()) return FALSE;
    std::string tmp_path = font_dir + lpFilename;

    CFStringRef path_str = CFStringCreateWithCString(NULL, tmp_path.c_str(), kCFStringEncodingUTF8);
    CFURLRef url = CFURLCreateWithFileSystemPath(NULL, path_str, kCFURLPOSIXPathStyle, false);
    CFRelease(path_str);

    CFErrorRef err = nullptr;
    CTFontManagerUnregisterFontsForURL(url, kCTFontManagerScopeProcess, &err);
    CFRelease(url);
    if (err) CFRelease(err);
    return TRUE;
}

BOOL RemoveFontResourceW(LPCWSTR lpFilename) {
    if (!lpFilename) return FALSE;
    char buf[256];
    size_t i = 0;
    while (lpFilename[i] && i < 255) { buf[i] = (char)lpFilename[i]; i++; }
    buf[i] = '\0';
    return RemoveFontResourceA(buf);
}
