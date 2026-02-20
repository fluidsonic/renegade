// winuser.h compat shim for macOS/clang
#pragma once
#ifndef WINUSER_H_COMPAT
#define WINUSER_H_COMPAT

#include "windef.h"
#include <stdio.h>
#include <stdarg.h>

// Window messages
#define WM_NULL         0x0000
#define WM_CREATE       0x0001
#define WM_DESTROY      0x0002
#define WM_MOVE         0x0003
#define WM_SIZE         0x0005
#define WM_ACTIVATE     0x0006
#define WM_SETFOCUS     0x0007
#define WM_KILLFOCUS    0x0008
#define WM_ENABLE       0x000A
#define WM_PAINT        0x000F
#define WM_CLOSE        0x0010
#define WM_QUIT         0x0012
#define WM_ERASEBKGND   0x0014
#define WM_SHOWWINDOW   0x0018
#define WM_SETTEXT      0x000C
#define WM_GETTEXT      0x000D
#define WM_KEYDOWN      0x0100
#define WM_KEYUP        0x0101
#define WM_CHAR         0x0102
#define WM_SYSKEYDOWN   0x0104
#define WM_SYSKEYUP     0x0105
#define WM_SYSCHAR      0x0106
#define WM_MOUSEMOVE    0x0200
#define WM_LBUTTONDOWN  0x0201
#define WM_LBUTTONUP    0x0202
#define WM_RBUTTONDOWN  0x0204
#define WM_RBUTTONUP    0x0205
#define WM_MBUTTONDOWN  0x0207
#define WM_MBUTTONUP    0x0208
#define WM_MOUSEWHEEL   0x020A
#define WM_TIMER        0x0113
#define WM_USER         0x0400
#define WM_APP          0x8000

// Activate message params
#define WA_INACTIVE     0
#define WA_ACTIVE       1
#define WA_CLICKACTIVE  2

// Show window constants
#define SW_HIDE         0
#define SW_SHOW         5
#define SW_SHOWDEFAULT  10
#define SW_SHOWNORMAL   1
#define SW_SHOWMINIMIZED 2
#define SW_SHOWMAXIMIZED 3

// MessageBox constants
#define MB_OK               0x00000000L
#define MB_OKCANCEL         0x00000001L
#define MB_ABORTRETRYIGNORE 0x00000002L
#define MB_YESNOCANCEL      0x00000003L
#define MB_YESNO            0x00000004L
#define MB_RETRYCANCEL      0x00000005L
#define MB_ICONHAND         0x00000010L
#define MB_ICONQUESTION     0x00000020L
#define MB_ICONEXCLAMATION  0x00000030L
#define MB_ICONINFORMATION  0x00000040L
#define MB_SETFOREGROUND    0x00010000L
#define MB_TASKMODAL        0x00002000L
#define IDABORT  3
#define IDRETRY  4
#define IDIGNORE 5
#define IDYES    6
#define IDNO     7
#define IDOK     1
#define IDCANCEL 2

// Window styles
#define WS_OVERLAPPED       0x00000000L
#define WS_POPUP            0x80000000L
#define WS_CHILD            0x40000000L
#define WS_VISIBLE          0x10000000L
#define WS_DISABLED         0x08000000L
#define WS_CAPTION          0x00C00000L
#define WS_BORDER           0x00800000L
#define WS_SYSMENU          0x00080000L
#define WS_THICKFRAME       0x00040000L
#define WS_MINIMIZEBOX      0x00020000L
#define WS_MAXIMIZEBOX      0x00010000L
#define WS_OVERLAPPEDWINDOW (WS_OVERLAPPED|WS_CAPTION|WS_SYSMENU|WS_THICKFRAME|WS_MINIMIZEBOX|WS_MAXIMIZEBOX)
#define WS_EX_APPWINDOW     0x00040000L
#define WS_EX_OVERLAPPEDWINDOW 0

// Key codes
#define VK_LBUTTON  0x01
#define VK_RBUTTON  0x02
#define VK_CANCEL   0x03
#define VK_MBUTTON  0x04
#define VK_BACK     0x08
#define VK_TAB      0x09
#define VK_RETURN   0x0D
#define VK_SHIFT    0x10
#define VK_CONTROL  0x11
#define VK_MENU     0x12  // ALT
#define VK_PAUSE    0x13
#define VK_ESCAPE   0x1B
#define VK_SPACE    0x20
#define VK_PRIOR    0x21  // Page Up
#define VK_NEXT     0x22  // Page Down
#define VK_END      0x23
#define VK_HOME     0x24
#define VK_LEFT     0x25
#define VK_UP       0x26
#define VK_RIGHT    0x27
#define VK_DOWN     0x28
#define VK_DELETE   0x2E
#define VK_F1       0x70
#define VK_F2       0x71
#define VK_F3       0x72
#define VK_F4       0x73
#define VK_F5       0x74
#define VK_F6       0x75
#define VK_F7       0x76
#define VK_F8       0x77
#define VK_F9       0x78
#define VK_F10      0x79
#define VK_F11      0x7A
#define VK_F12      0x7B
#define VK_NUMPAD0  0x60
#define VK_NUMPAD9  0x69
#define VK_MULTIPLY 0x6A
#define VK_ADD      0x6B
#define VK_SUBTRACT 0x6D
#define VK_DECIMAL  0x6E
#define VK_DIVIDE   0x6F

// MSG structure
typedef struct tagMSG {
    HWND   hwnd;
    UINT   message;
    UINT   wParam;    // technically WPARAM but we use UINT
    LONG   lParam;    // technically LPARAM but we use LONG
    DWORD  time;
    POINT  pt;
} MSG, *PMSG, *LPMSG;
typedef UINT   WPARAM;
typedef LONG   LPARAM;
typedef LONG   LRESULT;

// WNDCLASS stubs
typedef LRESULT (*WNDPROC)(HWND, UINT, WPARAM, LPARAM);

typedef struct tagWNDCLASS {
    UINT      style;
    WNDPROC   lpfnWndProc;
    int       cbClsExtra;
    int       cbWndExtra;
    HINSTANCE hInstance;
    HICON     hIcon;
    HCURSOR   hCursor;
    HBRUSH    hbrBackground;
    LPCSTR    lpszMenuName;
    LPCSTR    lpszClassName;
} WNDCLASS, *PWNDCLASS, *LPWNDCLASS;
typedef WNDCLASS WNDCLASSA;

typedef struct tagWNDCLASSEX {
    UINT      cbSize;
    UINT      style;
    WNDPROC   lpfnWndProc;
    int       cbClsExtra;
    int       cbWndExtra;
    HINSTANCE hInstance;
    HICON     hIcon;
    HCURSOR   hCursor;
    HBRUSH    hbrBackground;
    LPCSTR    lpszMenuName;
    LPCSTR    lpszClassName;
    HICON     hIconSm;
} WNDCLASSEX, *PWNDCLASSEX;
typedef WNDCLASSEX WNDCLASSEXA;

// Window function stubs
inline BOOL RegisterClass(const WNDCLASS* wc) { return TRUE; }
inline BOOL RegisterClassA(const WNDCLASS* wc) { return TRUE; }
inline BOOL RegisterClassEx(const WNDCLASSEX* wc) { return TRUE; }
inline BOOL UnregisterClass(LPCSTR name, HINSTANCE inst) { return TRUE; }

inline HWND CreateWindowEx(DWORD exStyle, LPCSTR cls, LPCSTR title, DWORD style,
                           int x, int y, int w, int h, HWND parent, HMENU menu,
                           HINSTANCE inst, LPVOID param) { return NULL; }
inline HWND CreateWindow(LPCSTR cls, LPCSTR title, DWORD style,
                         int x, int y, int w, int h, HWND parent, HMENU menu,
                         HINSTANCE inst, LPVOID param) { return NULL; }
inline BOOL ShowWindow(HWND wnd, int cmd) { return FALSE; }
inline BOOL UpdateWindow(HWND wnd) { return FALSE; }
inline BOOL DestroyWindow(HWND wnd) { return FALSE; }
inline BOOL MoveWindow(HWND wnd, int x, int y, int w, int h, BOOL repaint) { return FALSE; }
inline BOOL InvalidateRect(HWND wnd, const RECT* r, BOOL erase) { return FALSE; }
inline HWND GetForegroundWindow() { return NULL; }
inline HWND GetActiveWindow() { return NULL; }
inline HWND SetFocus(HWND wnd) { return NULL; }
inline HWND GetDesktopWindow() { return NULL; }
inline BOOL GetWindowRect(HWND wnd, LPRECT r) { return FALSE; }
inline BOOL GetClientRect(HWND wnd, LPRECT r) { if (r) { r->left=r->top=r->right=r->bottom=0; } return FALSE; }
inline LONG GetWindowLong(HWND wnd, int idx) { return 0; }
inline LONG SetWindowLong(HWND wnd, int idx, LONG val) { return 0; }
inline BOOL SetWindowText(HWND wnd, LPCSTR s) { return FALSE; }
inline BOOL IsWindow(HWND wnd) { return FALSE; }
inline BOOL IsWindowVisible(HWND wnd) { return FALSE; }
inline BOOL SetWindowPos(HWND wnd, HWND after, int x, int y, int cx, int cy, UINT flags) { return FALSE; }

// Message loop stubs
inline BOOL GetMessage(LPMSG msg, HWND wnd, UINT min, UINT max) { return FALSE; }
inline BOOL PeekMessage(LPMSG msg, HWND wnd, UINT min, UINT max, UINT remove) { return FALSE; }
inline BOOL TranslateMessage(const MSG* msg) { return FALSE; }
inline LRESULT DispatchMessage(const MSG* msg) { return 0; }
inline void PostQuitMessage(int code) {}
inline BOOL PostMessage(HWND wnd, UINT msg, WPARAM wp, LPARAM lp) { return FALSE; }
inline LRESULT SendMessage(HWND wnd, UINT msg, WPARAM wp, LPARAM lp) { return 0; }
inline LRESULT SendMessageA(HWND wnd, UINT msg, WPARAM wp, LPARAM lp) { return 0; }
inline LRESULT DefWindowProc(HWND wnd, UINT msg, WPARAM wp, LPARAM lp) { return 0; }

// MessageBox stubs
inline int MessageBox(HWND wnd, LPCSTR text, LPCSTR title, UINT type) {
    fprintf(stderr, "[MessageBox] %s: %s\n", title ? title : "", text ? text : "");
    if (type & MB_ABORTRETRYIGNORE) return IDIGNORE;
    return IDOK;
}
inline int MessageBoxA(HWND wnd, LPCSTR text, LPCSTR title, UINT type) {
    return MessageBox(wnd, text, title, type);
}

// Cursor/Icon stubs
inline HCURSOR LoadCursor(HINSTANCE inst, LPCSTR name) { return NULL; }
inline HCURSOR SetCursor(HCURSOR c) { return NULL; }
inline BOOL ShowCursor(BOOL show) { return FALSE; }

// Timer stubs
inline UINT SetTimer(HWND wnd, UINT id, UINT elapse, LPVOID proc) { return 0; }
inline BOOL KillTimer(HWND wnd, UINT id) { return FALSE; }

// Clipboard stubs
inline BOOL OpenClipboard(HWND wnd) { return FALSE; }
inline BOOL CloseClipboard() { return FALSE; }
inline BOOL EmptyClipboard() { return FALSE; }
inline HANDLE GetClipboardData(UINT fmt) { return NULL; }
inline HANDLE SetClipboardData(UINT fmt, HANDLE data) { return NULL; }

// Misc
#define GWL_USERDATA (-21)
#define GWL_STYLE    (-16)
#define GWL_EXSTYLE  (-20)
#define GWL_WNDPROC  (-4)
#define GWL_HWNDPARENT (-8)
#define GWL_ID       (-12)
#define CS_VREDRAW   0x0001
#define CS_HREDRAW   0x0002
#define CS_OWNDC     0x0020
#define SWP_NOSIZE       0x0001
#define SWP_NOMOVE       0x0002
#define SWP_NOZORDER     0x0004
#define SWP_NOACTIVATE   0x0010
#define SWP_SHOWWINDOW   0x0040
#define SWP_HIDEWINDOW   0x0080
#define HWND_TOP         ((HWND)0)
#define HWND_TOPMOST     ((HWND)-1)
#define PM_REMOVE        0x0001
#define PM_NOREMOVE      0x0000

inline HDC GetDC(HWND wnd) { return NULL; }
inline HDC GetWindowDC(HWND wnd) { return NULL; }
inline int ReleaseDC(HWND wnd, HDC dc) { return 0; }
inline BOOL ScreenToClient(HWND wnd, LPPOINT pt) { return FALSE; }
inline BOOL ClientToScreen(HWND wnd, LPPOINT pt) { return FALSE; }
inline BOOL ClipCursor(const RECT* r) { return FALSE; }
inline BOOL GetCursorPos(LPPOINT pt) { if (pt) { pt->x = pt->y = 0; } return FALSE; }
inline BOOL SetCursorPos(int x, int y) { return FALSE; }

// wsprintf is in winbase.h
inline int wvsprintf(LPSTR buf, LPCSTR fmt, va_list va) { return vsprintf(buf, fmt, va); }

// LoadString stub
inline int LoadString(HINSTANCE inst, UINT id, LPSTR buf, int size) {
    if (buf && size > 0) buf[0] = 0;
    return 0;
}

#endif // WINUSER_H_COMPAT
