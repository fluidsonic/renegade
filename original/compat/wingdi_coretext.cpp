// wingdi_coretext.cpp — GDI text rendering via CoreText/CoreGraphics for macOS
// Implements the subset of GDI used by FontCharsClass (render2dsentence.cpp):
//   CreateFont / CreateDIBSection / CreateCompatibleDC / SelectObject / DeleteObject
//   DeleteDC / SetBkColor / SetTextColor / GetTextMetrics / GetTextExtentPoint32W
//   ExtTextOutW / GetDC / ReleaseDC
// Also implements AddFontResource / RemoveFontResource which load font bytes
// from MIX archives and register them with CoreText.

#include <CoreText/CoreText.h>
#include <CoreGraphics/CoreGraphics.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include <math.h>
#include <stdio.h>
#include <sys/stat.h>
#include <mach-o/dyld.h>
#include <dirent.h>
#include <vector>
#include <string>
#include <ctype.h>

// Pull in compat types
#include "windef.h"
#include "wingdi.h"    // BITMAPINFO, TEXTMETRIC, HGDIOBJ, etc.
#include "winuser.h"   // HWND, SIZE, RECT

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

// ---- Copy CG (32-bit BGRA) → GDI (24-bit BGR) ----
// CGBitmapContext memory layout is top-down: cg_buf row 0 = visual top of image.
// The GDI caller requests a top-down DIB (biHeight < 0), so gdi_buf row 0 must
// also be the visual top.  No row reversal is needed — copy rows straight through.
static void sync_gdi_buf(FakeBitmap* bmp) {
    for (int row = 0; row < bmp->height; row++) {
        const uint8_t* src = bmp->cg_buf + (size_t)row * bmp->width * 4;
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

            // CGBitmapContext stores row 0 at the top of the visual image, but
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
// Extracts font bytes by:
//   1. Trying the filename as a loose file (relative to CWD, then data/ subdir).
//   2. Scanning MIX archives in data/ using a standalone MIX parser.
// Writes extracted bytes to <exe_dir>/fonts/<filename> and registers with
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

// CRC32 table for poly 0x04C11DB7 (same table as realcrc.cpp)
static const uint32_t s_crc32_table[256] = {
    0x00000000L, 0x77073096L, 0xEE0E612CL, 0x990951BAL,
    0x076DC419L, 0x706AF48FL, 0xE963A535L, 0x9E6495A3L,
    0x0EDB8832L, 0x79DCB8A4L, 0xE0D5E91EL, 0x97D2D988L,
    0x09B64C2BL, 0x7EB17CBDL, 0xE7B82D07L, 0x90BF1D91L,
    0x1DB71064L, 0x6AB020F2L, 0xF3B97148L, 0x84BE41DEL,
    0x1ADAD47DL, 0x6DDDE4EBL, 0xF4D4B551L, 0x83D385C7L,
    0x136C9856L, 0x646BA8C0L, 0xFD62F97AL, 0x8A65C9ECL,
    0x14015C4FL, 0x63066CD9L, 0xFA0F3D63L, 0x8D080DF5L,
    0x3B6E20C8L, 0x4C69105EL, 0xD56041E4L, 0xA2677172L,
    0x3C03E4D1L, 0x4B04D447L, 0xD20D85FDL, 0xA50AB56BL,
    0x35B5A8FAL, 0x42B2986CL, 0xDBBBC9D6L, 0xACBCF940L,
    0x32D86CE3L, 0x45DF5C75L, 0xDCD60DCFL, 0xABD13D59L,
    0x26D930ACL, 0x51DE003AL, 0xC8D75180L, 0xBFD06116L,
    0x21B4F4B5L, 0x56B3C423L, 0xCFBA9599L, 0xB8BDA50FL,
    0x2802B89EL, 0x5F058808L, 0xC60CD9B2L, 0xB10BE924L,
    0x2F6F7C87L, 0x58684C11L, 0xC1611DABL, 0xB6662D3DL,
    0x76DC4190L, 0x01DB7106L, 0x98D220BCL, 0xEFD5102AL,
    0x71B18589L, 0x06B6B51FL, 0x9FBFE4A5L, 0xE8B8D433L,
    0x7807C9A2L, 0x0F00F934L, 0x9609A88EL, 0xE10E9818L,
    0x7F6A0DBBL, 0x086D3D2DL, 0x91646C97L, 0xE6635C01L,
    0x6B6B51F4L, 0x1C6C6162L, 0x856530D8L, 0xF262004EL,
    0x6C0695EDL, 0x1B01A57BL, 0x8208F4C1L, 0xF50FC457L,
    0x65B0D9C6L, 0x12B7E950L, 0x8BBEB8EAL, 0xFCB9887CL,
    0x62DD1DDFL, 0x15DA2D49L, 0x8CD37CF3L, 0xFBD44C65L,
    0x4DB26158L, 0x3AB551CEL, 0xA3BC0074L, 0xD4BB30E2L,
    0x4ADFA541L, 0x3DD895D7L, 0xA4D1C46DL, 0xD3D6F4FBL,
    0x4369E96AL, 0x346ED9FCL, 0xAD678846L, 0xDA60B8D0L,
    0x44042D73L, 0x33031DE5L, 0xAA0A4C5FL, 0xDD0D7CC9L,
    0x5005713CL, 0x270241AAL, 0xBE0B1010L, 0xC90C2086L,
    0x5768B525L, 0x206F85B3L, 0xB966D409L, 0xCE61E49FL,
    0x5EDEF90EL, 0x29D9C998L, 0xB0D09822L, 0xC7D7A8B4L,
    0x59B33D17L, 0x2EB40D81L, 0xB7BD5C3BL, 0xC0BA6CADL,
    0xEDB88320L, 0x9ABFB3B6L, 0x03B6E20CL, 0x74B1D29AL,
    0xEAD54739L, 0x9DD277AFL, 0x04DB2615L, 0x73DC1683L,
    0xE3630B12L, 0x94643B84L, 0x0D6D6A3EL, 0x7A6A5AA8L,
    0xE40ECF0BL, 0x9309FF9DL, 0x0A00AE27L, 0x7D079EB1L,
    0xF00F9344L, 0x8708A3D2L, 0x1E01F268L, 0x6906C2FEL,
    0xF762575DL, 0x806567CBL, 0x196C3671L, 0x6E6B06E7L,
    0xFED41B76L, 0x89D32BE0L, 0x10DA7A5AL, 0x67DD4ACCL,
    0xF9B9DF6FL, 0x8EBEEFF9L, 0x17B7BE43L, 0x60B08ED5L,
    0xD6D6A3E8L, 0xA1D1937EL, 0x38D8C2C4L, 0x4FDFF252L,
    0xD1BB67F1L, 0xA6BC5767L, 0x3FB506DDL, 0x48B2364BL,
    0xD80D2BDAL, 0xAF0A1B4CL, 0x36034AF6L, 0x41047A60L,
    0xDF60EFC3L, 0xA867DF55L, 0x316E8EEFL, 0x4669BE79L,
    0xCB61B38CL, 0xBC66831AL, 0x256FD2A0L, 0x5268E236L,
    0xCC0C7795L, 0xBB0B4703L, 0x220216B9L, 0x5505262FL,
    0xC5BA3BBEL, 0xB2BD0B28L, 0x2BB45A92L, 0x5CB36A04L,
    0xC2D7FFA7L, 0xB5D0CF31L, 0x2CD99E8BL, 0x5BDEAE1DL,
    0x9B64C2B0L, 0xEC63F226L, 0x756AA39CL, 0x026D930AL,
    0x9C0906A9L, 0xEB0E363FL, 0x72076785L, 0x05005713L,
    0x95BF4A82L, 0xE2B87A14L, 0x7BB12BAEL, 0x0CB61B38L,
    0x92D28E9BL, 0xE5D5BE0DL, 0x7CDCEFB7L, 0x0BDBDF21L,
    0x86D3D2D4L, 0xF1D4E242L, 0x68DDB3F8L, 0x1FDA836EL,
    0x81BE16CDL, 0xF6B9265BL, 0x6FB077E1L, 0x18B74777L,
    0x88085AE6L, 0xFF0F6A70L, 0x66063BCAL, 0x11010B5CL,
    0x8F659EFFL, 0xF862AE69L, 0x616BFFD3L, 0x166CCF45L,
    0xA00AE278L, 0xD70DD2EEL, 0x4E048354L, 0x3903B3C2L,
    0xA7672661L, 0xD06016F7L, 0x4969474DL, 0x3E6E77DBL,
    0xAED16A4AL, 0xD9D65ADCL, 0x40DF0B66L, 0x37D83BF0L,
    0xA9BCAE53L, 0xDEBB9EC5L, 0x47B2CF7FL, 0x30B5FFE9L,
    0xBDBDF21CL, 0xCABAC28AL, 0x53B39330L, 0x24B4A3A6L,
    0xBAD03605L, 0xCDD70693L, 0x54DE5729L, 0x23D967BFL,
    0xB3667A2EL, 0xC4614AB8L, 0x5D681B02L, 0x2A6F2B94L,
    0xB40BBE37L, 0xC30C8EA1L, 0x5A05DF1BL, 0x2D02EF8DL
};

// Case-insensitive CRC32 matching CRC_Stringi() from realcrc.cpp.
// Processes each character as toupper(c), standard CRC32 with 0xFFFFFFFF init/final XOR.
static uint32_t mix_crc(const char* name) {
    uint32_t crc = 0xFFFFFFFF;
    while (*name) {
        unsigned char c = (unsigned char)toupper((unsigned char)*name++);
        crc = s_crc32_table[(crc ^ c) & 0xFF] ^ (crc >> 8);
    }
    return crc ^ 0xFFFFFFFF;
}

// MIX file layout (from mixfile.cpp / MixFileCreator):
//   [0]  char signature[4]  — "MIX1"
//   [4]  int32 header_offset — file position of the MIXFILE_DATA_HEADER
//   [8]  int32 names_offset  — file position of the names section (not needed here)
//   [12] int32 unused
//   --- at header_offset: ---
//   int32 file_count
//   file_count * { uint32 CRC, uint32 Offset, uint32 Size }  (sorted by CRC)
//   --- data blob starts at offset 16 (right after the 16-byte file header) ---
//   Entry Offset values are relative to start of the data blob (byte 16).
static bool extract_from_mix(const char* mix_path, uint32_t target_crc, std::vector<uint8_t>& out) {
    FILE* f = fopen(mix_path, "rb");
    if (!f) return false;

    // Read the 16-byte file header
    char sig[4];
    int32_t header_offset, names_offset, unused;
    if (fread(sig, 1, 4, f) != 4 ||
        memcmp(sig, "MIX1", 4) != 0 ||
        fread(&header_offset, 4, 1, f) != 1 ||
        fread(&names_offset,  4, 1, f) != 1 ||
        fread(&unused,        4, 1, f) != 1) {
        fclose(f);
        return false;
    }

    fprintf(stderr, "[MIX] %s: header_offset=%d names_offset=%d\n", mix_path, header_offset, names_offset);

    // Seek to the index header and read file_count
    if (fseek(f, header_offset, SEEK_SET) != 0) { fclose(f); return false; }
    int32_t file_count = 0;
    if (fread(&file_count, 4, 1, f) != 1 || file_count <= 0 || file_count > 65536) {
        fclose(f); return false;
    }

    // Read the index entries
    struct MixEntry { uint32_t crc, offset, size; };
    std::vector<MixEntry> entries((size_t)file_count);
    if (fread(entries.data(), sizeof(MixEntry), (size_t)file_count, f) != (size_t)file_count) {
        fclose(f); return false;
    }

    // Binary search for the target CRC (entries are sorted by CRC)
    const MixEntry* found = nullptr;
    int lo = 0, hi = file_count - 1;
    while (lo <= hi) {
        int mid = (lo + hi) / 2;
        if (entries[mid].crc == target_crc) { found = &entries[mid]; break; }
        if (entries[mid].crc < target_crc) lo = mid + 1;
        else hi = mid - 1;
    }

    if (!found) { fclose(f); return false; }

    // Entry offsets are absolute file positions (MixFileCreator stores MixFile->Tell()
    // at the time each file is written, which is an absolute offset from the file start).
    long data_base = 0;
    long file_pos = (long)found->offset;
    if (fseek(f, file_pos, SEEK_SET) != 0) { fclose(f); return false; }

    out.resize(found->size);
    bool ok = (fread(out.data(), 1, found->size, f) == found->size);
    fclose(f);
    if (!ok) { out.clear(); return false; }
    fprintf(stderr, "[MIX] %s: offset=%u size=%u data_base=%ld first4=%02X%02X%02X%02X\n",
            mix_path, found->offset, found->size, data_base,
            out.size()>0?out[0]:0, out.size()>1?out[1]:0,
            out.size()>2?out[2]:0, out.size()>3?out[3]:0);
    return true;
}

int AddFontResourceA(LPCSTR lpFilename) {
    if (!lpFilename) return 0;

    std::vector<uint8_t> font_bytes;
    bool found = false;

    // 1. Try loose file (bare filename, then data/ prefix)
    {
        FILE* f = fopen(lpFilename, "rb");
        if (!f) {
            std::string data_path = std::string("data/") + lpFilename;
            f = fopen(data_path.c_str(), "rb");
        }
        if (f) {
            fseek(f, 0, SEEK_END);
            long sz = ftell(f);
            fseek(f, 0, SEEK_SET);
            if (sz > 0) {
                font_bytes.resize((size_t)sz);
                found = (fread(font_bytes.data(), 1, (size_t)sz, f) == (size_t)sz);
                if (found)
                    fprintf(stderr, "[AddFontResource] Found loose file: %s\n", lpFilename);
            }
            fclose(f);
        }
    }

    // 2. Search MIX archives
    if (!found) {
        uint32_t crc = mix_crc(lpFilename);

        // Fixed candidates first
        const char* fixed_mix[] = {
            "data/always.dat", "data/Always2.dat", "data/always.dbs", nullptr
        };
        for (int i = 0; fixed_mix[i] && !found; i++) {
            if (extract_from_mix(fixed_mix[i], crc, font_bytes)) {
                found = true;
                fprintf(stderr, "[AddFontResource] Extracted from %s: %s (%zu bytes)\n",
                        fixed_mix[i], lpFilename, font_bytes.size());
            }
        }

        // Also scan any other *.mix / *.dat files in data/
        if (!found) {
            DIR* dir = opendir("data");
            if (dir) {
                struct dirent* ent;
                while (!found && (ent = readdir(dir)) != nullptr) {
                    const char* n = ent->d_name;
                    size_t nl = strlen(n);
                    bool is_mix = (nl > 4 &&
                        (strcasecmp(n + nl - 4, ".mix") == 0 ||
                         strcasecmp(n + nl - 4, ".dat") == 0 ||
                         strcasecmp(n + nl - 4, ".dbs") == 0));
                    if (!is_mix) continue;

                    std::string mix_path = std::string("data/") + n;
                    // Skip the fixed candidates already tried above
                    if (mix_path == "data/always.dat" ||
                        mix_path == "data/Always2.dat" ||
                        mix_path == "data/always.dbs") continue;

                    if (extract_from_mix(mix_path.c_str(), crc, font_bytes)) {
                        found = true;
                        fprintf(stderr, "[AddFontResource] Extracted from %s: %s (%zu bytes)\n",
                                mix_path.c_str(), lpFilename, font_bytes.size());
                    }
                }
                closedir(dir);
            }
        }
    }

    if (!found) {
        // Font file not found in any MIX archive or loose file.
        // "ARI_____.TTF" (Arial MT) is intentionally absent from the MIX: it was
        // a standard Windows system font in 2002 and Westwood never shipped it.
        // macOS provides Arial natively via CoreText, so CreateFontA("Arial MT")
        // resolves correctly through the "Arial MT" -> "Arial" mapping above.
        // Any other missing font will also fall through to the system font resolver.
        fprintf(stderr, "[AddFontResource] Not found in MIX archives: %s"
                        " (expected for system fonts like Arial; CoreText will use system font)\n",
                        lpFilename);
        return 0;
    }

    // 3. Write bytes to <exe_dir>/fonts/<filename>
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
    fwrite(font_bytes.data(), 1, font_bytes.size(), tmp);
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

    fprintf(stderr, "[AddFontResource] Registered font: %s (%zu bytes)\n", lpFilename, font_bytes.size());
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
