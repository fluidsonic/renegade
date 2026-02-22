// winres.cpp — Windows .res resource file loader for macOS port
// Implements FindResource / LoadResource / LockResource / SizeofResource
// by loading and parsing chat.res from alongside the executable.

#include "global.h"
#include "winbase.h"
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <string>
#include <vector>
#include <mach-o/dyld.h>

// ---- Internal resource entry ----
struct ResEntry {
    uint16_t       type_id;
    uint16_t       name_id;
    const uint8_t* data;
    uint32_t       data_size;
};

static std::vector<ResEntry>* g_resources  = nullptr;
static uint8_t*                g_res_buf   = nullptr;

// ---- Locate chat.res next to the executable ----
static std::string find_chat_res() {
    char exe[4096];
    uint32_t sz = sizeof(exe);
    if (_NSGetExecutablePath(exe, &sz) != 0) return "";

    std::string dir(exe);
    size_t slash = dir.rfind('/');
    if (slash == std::string::npos) return "";
    dir = dir.substr(0, slash + 1);

    // Try MacOS/ directory (normal app-bundle location)
    std::string path = dir + "chat.res";
    FILE* f = fopen(path.c_str(), "rb");
    if (f) { fclose(f); return path; }

    // Fallback: build directory sibling
    path = dir + "../Resources/chat.res";
    f = fopen(path.c_str(), "rb");
    if (f) { fclose(f); return path; }

    return "";
}

// ---- Parse the .res binary format ----
// Format per entry:
//   DWORD DataSize
//   DWORD HeaderSize
//   TYPE  (0xFFFF + WORD ordinal, or null-terminated WCHAR string)
//   NAME  (same)
//   ... alignment to DWORD ...
//   DWORD DataVersion; WORD MemoryFlags; WORD LanguageId; DWORD Version; DWORD Characteristics
//   BYTE  Data[DataSize]
//   ... padding to next DWORD boundary ...
static void load_resources() {
    if (g_resources) return;
    g_resources = new std::vector<ResEntry>();

    std::string path = find_chat_res();
    if (path.empty()) {
        fprintf(stderr, "[winres] chat.res not found (looked next to executable)\n");
        return;
    }

    FILE* f = fopen(path.c_str(), "rb");
    if (!f) {
        fprintf(stderr, "[winres] Cannot open %s\n", path.c_str());
        return;
    }

    fseek(f, 0, SEEK_END);
    size_t file_size = (size_t)ftell(f);
    fseek(f, 0, SEEK_SET);

    g_res_buf = (uint8_t*)malloc(file_size);
    if (!g_res_buf || fread(g_res_buf, 1, file_size, f) != file_size) {
        fprintf(stderr, "[winres] Failed to read %s\n", path.c_str());
        fclose(f);
        return;
    }
    fclose(f);

    const uint8_t* p   = g_res_buf;
    const uint8_t* end = g_res_buf + file_size;

    while (p + 8 <= end) {
        uint32_t data_size   = *(const uint32_t*)(p + 0);
        uint32_t header_size = *(const uint32_t*)(p + 4);

        if (header_size < 8 || (size_t)(header_size) > (size_t)(end - p)) break;

        const uint8_t* hp = p + 8;

        // Parse TYPE
        uint16_t type_id = 0;
        if (hp + 2 <= end && *(const uint16_t*)hp == 0xFFFF) {
            hp += 2;
            if (hp + 2 <= end) { type_id = *(const uint16_t*)hp; hp += 2; }
        } else {
            // Unicode string — skip
            while (hp + 2 <= end && *(const uint16_t*)hp != 0) hp += 2;
            if (hp + 2 <= end) hp += 2;
        }

        // Parse NAME
        uint16_t name_id = 0;
        if (hp + 2 <= end && *(const uint16_t*)hp == 0xFFFF) {
            hp += 2;
            if (hp + 2 <= end) { name_id = *(const uint16_t*)hp; hp += 2; }
        } else {
            while (hp + 2 <= end && *(const uint16_t*)hp != 0) hp += 2;
            if (hp + 2 <= end) hp += 2;
        }

        // Add entry (skip null first resource)
        if (type_id != 0 || name_id != 0) {
            if ((size_t)(header_size) + data_size <= (size_t)(end - p)) {
                ResEntry e;
                e.type_id   = type_id;
                e.name_id   = name_id;
                e.data      = p + header_size;
                e.data_size = data_size;
                g_resources->push_back(e);
            }
        }

        // Advance to next entry (data follows header, then DWORD-aligned)
        size_t advance = (size_t)header_size + data_size;
        advance = (advance + 3u) & ~3u;
        if (advance == 0) break;
        p += advance;
    }

    fprintf(stderr, "[winres] Loaded %zu resources from %s\n",
            g_resources->size(), path.c_str());
}

// ---- Public API ----

HRSRC FindResource(HMODULE /*hmod*/, LPCSTR name, LPCSTR type) {
    load_resources();
    if (!g_resources) return nullptr;

    uintptr_t type_id = (uintptr_t)type;
    uintptr_t name_id = (uintptr_t)name;

    for (ResEntry& e : *g_resources) {
        if (e.type_id == (uint16_t)type_id && e.name_id == (uint16_t)name_id)
            return (HRSRC)&e;
    }
    return nullptr;
}

HRSRC FindResourceA(HMODULE hmod, LPCSTR name, LPCSTR type) {
    return FindResource(hmod, name, type);
}

HGLOBAL LoadResource(HMODULE /*hmod*/, HRSRC res) {
    return res; // HRSRC == HGLOBAL for our purposes
}

LPVOID LockResource(HGLOBAL res) {
    if (!res) return nullptr;
    return (LPVOID)((const ResEntry*)res)->data;
}

DWORD SizeofResource(HMODULE /*hmod*/, HRSRC res) {
    if (!res) return 0;
    return ((const ResEntry*)res)->data_size;
}
