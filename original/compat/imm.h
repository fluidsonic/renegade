#pragma once

#include "global.h"

// imm.h compat shim for macOS - Input Method Manager stubs

#include "wingdi.h"

// IME handles
typedef HANDLE HIMC;
typedef HANDLE HIMCC;
// HKL is defined in windef.h

// IME candidate list
typedef struct tagCANDIDATEFORM {
    DWORD  dwIndex;
    DWORD  dwStyle;
    POINT  ptCurrentPos;
    RECT   rcArea;
} CANDIDATEFORM, *PCANDIDATEFORM, *LPCANDIDATEFORM;

typedef struct tagCANDIDATELIST {
    DWORD  dwSize;
    DWORD  dwStyle;
    DWORD  dwCount;
    DWORD  dwSelection;
    DWORD  dwPageStart;
    DWORD  dwPageSize;
    DWORD  dwOffset[1];
} CANDIDATELIST, *PCANDIDATELIST, *LPCANDIDATELIST;

// IME composition form
#define CFS_DEFAULT         0x0000
#define CFS_RECT            0x0001
#define CFS_POINT           0x0002
#define CFS_FORCE_POSITION  0x0020
#define CFS_CANDIDATEPOS    0x0040
#define CFS_EXCLUDE         0x0080

typedef struct tagCOMPOSITIONFORM {
    DWORD  dwStyle;
    POINT  ptCurrentPos;
    RECT   rcArea;
} COMPOSITIONFORM, *PCOMPOSITIONFORM, *LPCOMPOSITIONFORM;

typedef BOOL (*IMCENUMPROC)(HIMC, LPARAM);

// IME composition string constants
#define GCS_COMPREADSTR     0x0001
#define GCS_COMPREADATTR    0x0002
#define GCS_COMPREADCLAUSE  0x0004
#define GCS_COMPSTR         0x0008
#define GCS_COMPATTR        0x0010
#define GCS_COMPCLAUSE      0x0020
#define GCS_CURSORPOS       0x0080
#define GCS_DELTASTART      0x0100
#define GCS_RESULTREADSTR   0x0200
#define GCS_RESULTREADCLAUSE 0x0400
#define GCS_RESULTSTR       0x0800
#define GCS_RESULTCLAUSE    0x1000

// IME messages
#define WM_IME_STARTCOMPOSITION     0x010D
#define WM_IME_ENDCOMPOSITION       0x010E
#define WM_IME_COMPOSITION          0x010F
#define WM_IME_SETCONTEXT           0x0281
#define WM_IME_NOTIFY               0x0282
#define WM_IME_CONTROL              0x0283
#define WM_IME_COMPOSITIONFULL      0x0284
#define WM_IME_SELECT               0x0285
#define WM_IME_CHAR                 0x0286
#define WM_IME_REQUEST              0x0288
#define WM_IME_KEYDOWN              0x0290
#define WM_IME_KEYUP                0x0291

// IME action codes for ImmNotifyIME
#define NI_CHANGECANDIDATELIST      0x0013
#define NI_CLOSECANDIDATE           0x0011
#define NI_COMPOSITIONSTR           0x0015
#define NI_IMEMENUSELECTED          0x0018
#define NI_OPENCANDIDATE            0x0010
#define NI_SELECTCANDIDATESTR       0x0012
#define NI_SETCANDIDATE_PAGESIZE    0x0014
#define NI_SETCANDIDATE_PAGESTART   0x0016

// IME composition attribute values (for GCS_COMPATTR)
#define ATTR_INPUT              0x00
#define ATTR_TARGET_CONVERTED   0x01
#define ATTR_CONVERTED          0x02
#define ATTR_TARGET_NOTCONVERTED 0x03
#define ATTR_INPUT_ERROR        0x04
#define ATTR_FIXEDCONVERTED     0x05

// IME guideline constants
#define GL_LEVEL_NOGUIDELINE    0x00000000
#define GL_LEVEL_FATAL          0x00000001
#define GL_LEVEL_ERROR          0x00000002
#define GL_LEVEL_WARNING        0x00000003
#define GL_LEVEL_INFORMATION    0x00000004
#define GL_ID_UNKNOWN           0x00000000
#define GL_ID_NOMODULE          0x00000001
#define GL_ID_NODICTIONARY      0x00000010
#define GL_ID_CANNOTSAVE        0x00000011
#define GL_ID_NOCONVERT         0x00000020
#define GL_ID_TYPINGERROR       0x00000021
#define GL_ID_TOOMANYSTROKE     0x00000022
#define GL_ID_READINGCONFLICT   0x00000023
#define GL_ID_INPUTREADING      0x00000024
#define GL_ID_INPUTRADICAL      0x00000025
#define GL_ID_INPUTCODE         0x00000026
#define GL_ID_INPUTSYMBOL       0x00000027
#define GL_ID_CHOOSE            0x00000028
#define GL_ID_REVERSECONVERSION 0x00000029
#define GGL_LEVEL              0x00000001
#define GGL_INDEX              0x00000002
#define GGL_STRING             0x00000003
#define GGL_PRIVATE            0x00000004
inline DWORD ImmGetGuideLineA(HIMC hIMC, DWORD dwIndex, LPSTR lpBuf, DWORD dwBufLen) { if(lpBuf&&dwBufLen>0)lpBuf[0]=0; return 0; }
inline DWORD ImmGetGuideLineW(HIMC hIMC, DWORD dwIndex, LPWSTR lpBuf, DWORD dwBufLen) { if(lpBuf&&dwBufLen>0)lpBuf[0]=0; return 0; }
#define ImmGetGuideLine ImmGetGuideLineA

// Candidate style
#define IME_CAND_UNKNOWN    0x0000
#define IME_CAND_READ       0x0001
#define IME_CAND_CODE       0x0002
#define IME_CAND_MEANING    0x0003
#define IME_CAND_RADICAL    0x0004
#define IME_CAND_STROKE     0x0005

// Composition string index for ImmGetCompositionString
#define SCS_SETSTR          (GCS_COMPREADSTR | GCS_COMPSTR)
#define SCS_CHANGEATTR      (GCS_COMPREADATTR | GCS_COMPATTR)
#define SCS_CHANGECLAUSE    (GCS_COMPREADCLAUSE | GCS_COMPCLAUSE)
#define SCS_SETRECONVERTSTRING  0x00010000
#define SCS_QUERYRECONVERTSTRING 0x00020000

// IME notification messages
#define IMN_CLOSESTATUSWINDOW       0x0001
#define IMN_OPENSTATUSWINDOW        0x0002
#define IMN_CHANGECANDIDATE         0x0003
#define IMN_CLOSECANDIDATE          0x0004
#define IMN_OPENCANDIDATE           0x0005
#define IMN_SETCONVERSIONMODE       0x0006
#define IMN_SETSENTENCEMODE         0x0007
#define IMN_SETOPENSTATUS           0x0008
#define IMN_SETCANDIDATEPOS         0x0009
#define IMN_SETCOMPOSITIONFONT      0x000A
#define IMN_SETCOMPOSITIONWINDOW    0x000B
#define IMN_SETSTATUSWINDOWPOS      0x000C
#define IMN_GUIDELINE               0x000D
#define IMN_PRIVATE                 0x000E

// IME function stubs
inline HIMC ImmGetContext(HWND hWnd) { return NULL; }
inline BOOL ImmReleaseContext(HWND hWnd, HIMC hIMC) { return FALSE; }
inline BOOL ImmGetOpenStatus(HIMC hIMC) { return FALSE; }
inline BOOL ImmSetOpenStatus(HIMC hIMC, BOOL fOpen) { return FALSE; }
inline LONG ImmGetCompositionStringA(HIMC hIMC, DWORD dwIndex, LPVOID lpBuf, DWORD dwBufLen) { (void)hIMC;(void)dwIndex;(void)lpBuf;(void)dwBufLen; return 0; }
inline LONG ImmGetCompositionStringW(HIMC hIMC, DWORD dwIndex, LPVOID lpBuf, DWORD dwBufLen) { (void)hIMC;(void)dwIndex;(void)lpBuf;(void)dwBufLen; return 0; }
inline BOOL ImmSetCompositionStringA(HIMC hIMC, DWORD dwIndex, LPVOID lpComp, DWORD dwCompLen, LPVOID lpRead, DWORD dwReadLen) { return FALSE; }
inline BOOL ImmSetCompositionWindow(HIMC hIMC, LPCOMPOSITIONFORM lpCompForm) { return FALSE; }
inline BOOL ImmSetCandidateWindow(HIMC hIMC, LPCANDIDATEFORM lpCandidate) { return FALSE; }
inline DWORD ImmGetCandidateListCountA(HIMC hIMC, LPDWORD lpdwListCount) { if(lpdwListCount)*lpdwListCount=0; return 0; }
inline DWORD ImmGetCandidateListCountW(HIMC hIMC, LPDWORD lpdwListCount) { if(lpdwListCount)*lpdwListCount=0; return 0; }
inline DWORD ImmGetCandidateListA(HIMC hIMC, DWORD dwIndex, LPCANDIDATELIST lpCandList, DWORD dwBufLen) { return 0; }
inline DWORD ImmGetCandidateListW(HIMC hIMC, DWORD dwIndex, LPCANDIDATELIST lpCandList, DWORD dwBufLen) { return 0; }
inline BOOL ImmNotifyIME(HIMC hIMC, DWORD dwAction, DWORD dwIndex, DWORD dwValue) { return FALSE; }
inline HIMC ImmAssociateContext(HWND hWnd, HIMC hIMC) { return NULL; }
inline BOOL ImmIsIME(HKL hKL) { return FALSE; }
inline BOOL ImmConfigureIMEA(HKL hKL, HWND hWnd, DWORD dwMode, LPVOID lpData) { return FALSE; }
inline BOOL ImmEnumInputContext(DWORD idThread, IMCENUMPROC lpfn, LPARAM lParam) { return FALSE; }
inline BOOL ImmGetCompositionFontA(HIMC hIMC, LPLOGFONTA lplf) { return FALSE; }
inline BOOL ImmGetCompositionFontW(HIMC hIMC, LPLOGFONTW lplf) { return FALSE; }
#define ImmGetCompositionFont ImmGetCompositionFontA
inline BOOL ImmSetCompositionFontA(HIMC hIMC, LPLOGFONTA lplf) { return FALSE; }
#define ImmSetCompositionFont ImmSetCompositionFontA
inline HIMC ImmCreateContext() { return NULL; }
inline BOOL ImmDestroyContext(HIMC hIMC) { return FALSE; }
inline DWORD ImmGetProperty(HKL hKL, DWORD dwIndex) { return 0; }
inline UINT ImmGetDescriptionA(HKL hKL, LPSTR lpszDescription, UINT uBufLen) { if(lpszDescription&&uBufLen>0)lpszDescription[0]=0; return 0; }
inline UINT ImmGetDescriptionW(HKL hKL, LPWSTR lpszDescription, UINT uBufLen) { if(lpszDescription&&uBufLen>0)lpszDescription[0]=0; return 0; }
#define ImmGetDescription ImmGetDescriptionA

// ImmGetProperty index constants
#define IGP_PROPERTY            0x00000004
#define IGP_CONVERSION          0x00000008
#define IGP_SENTENCE            0x0000000c
#define IGP_UI                  0x00000010
#define IGP_SETCOMPSTR          0x00000014
#define IGP_SELECT              0x00000018

// IME property bit flags
#define IME_PROP_AT_CARET               0x00010000
#define IME_PROP_SPECIAL_UI             0x00020000
#define IME_PROP_CANDLIST_START_FROM_1  0x00040000
#define IME_PROP_UNICODE                0x00080000
#define IME_PROP_COMPLETE_ON_UNSELECT   0x00100000
#define IME_PROP_END_UNLOAD             0x00000001
#define IME_PROP_KBD_CHAR_FIRST         0x00000002
#define IME_PROP_IGNORE_UPKEYS          0x00000004
#define IME_PROP_NEED_ALTKEY            0x00000008
#define IME_PROP_NO_KEYS_ON_CLOSE       0x00000010

// ISC flags for WM_IME_SETCONTEXT lParam
#define ISC_SHOWUICOMPOSITIONWINDOW     0x80000000
#define ISC_SHOWUICANDIDATEWINDOW       0x00000001
#define ISC_SHOWUIALLCANDIDATEWINDOW    0x0000000F
#define ISC_SHOWUIALL                   0xC000000F

// Default to A versions for undecorated names
#define ImmGetCompositionString  ImmGetCompositionStringA
#define ImmGetCandidateListCount ImmGetCandidateListCountA
#define ImmGetCandidateList      ImmGetCandidateListA
#define ImmSetCompositionString  ImmSetCompositionStringA
#define ImmConfigureIME          ImmConfigureIMEA
