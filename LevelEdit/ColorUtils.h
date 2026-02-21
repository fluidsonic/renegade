//////////////////////////////////////////////////////////////////////////////////////////
//
//	ColorUtils.H
//
//

#ifndef __COLORUTILS_H
#define __COLORUTILS_H

/////////////////////////////////////////////////////////////////////////////
//	Callbacks
/////////////////////////////////////////////////////////////////////////////
typedef void (*WWCTRL_COLORCALLBACK)(int,int,int,void*);

/////////////////////////////////////////////////////////////////////////////
// Prototypes
/////////////////////////////////////////////////////////////////////////////
void		Frame_Rect (UCHAR *pbits, const RECT &rect, COLORREF color, int scanline_size);
void		Draw_Vert_Line (UCHAR *pbits, int x, int y, int len, COLORREF color, int scanline_size);
void		Draw_Horz_Line (UCHAR *pbits, int x, int y, int len, COLORREF color, int scanline_size);
void		Draw_Sunken_Rect (UCHAR *pbits, const RECT &rect, int scanline_size);
void		Draw_Raised_Rect (UCHAR *pbits, const RECT &rect, int scanline_size);
BOOL		Show_Color_Picker (int *red, int *green, int *blue);
HWND		Create_Color_Picker_Form (HWND parent, int red, int green, int blue);
BOOL		Get_Form_Color (HWND form_wnd, int *red, int *green, int *blue);
BOOL		Set_Form_Color (HWND form_wnd, int red, int green, int blue);
BOOL		Set_Form_Original_Color (HWND form_wnd, int red, int green, int blue);
BOOL		Set_Update_Callback (HWND form_wnd, WWCTRL_COLORCALLBACK callback, void *arg=NULL);
void		RegisterColorPicker (HINSTANCE hinst);
void		RegisterColorBar (HINSTANCE hinst);


#endif //__COLORUTILS_H
