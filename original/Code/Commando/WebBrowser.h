#ifndef __WEBBROWSER_H__
#define __WEBBROWSER_H__

// WebBrowser stub - COM/ATL/WOLBrowser not available on macOS
class WebBrowser {
public:
    static bool IsWebPageDisplayed(void) { return false; }
    static void Think(void) {}
    static void Close(void) {}
};

#endif // __WEBBROWSER_H__
