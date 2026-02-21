#if !defined(AFX_COLORPICKER_H__C943A7EC_CAA3_11D2_8DDF_00104B6FD9E3__INCLUDED_)
#define AFX_COLORPICKER_H__C943A7EC_CAA3_11D2_8DDF_00104B6FD9E3__INCLUDED_

#if _MSC_VER >= 1000
#pragma once
#endif // _MSC_VER >= 1000
// ColorPicker.h : header file
//


/////////////////////////////////////////////////////////////////////////////
//
// Constants
//

//
//	Window styles
//
#define		CPS_SUNKEN			0x00000001
#define		CPS_RAISED			0x00000002

//
//	Window messages and notifications
//
#define		CPN_COLORCHANGE	0x0001
#define		CPM_GETCOLOR		(WM_USER+101)
#define		CPM_SETCOLOR		(WM_USER+102)


/////////////////////////////////////////////////////////////////////////////
//
// Structures
//

// Structure used to send notifications via WM_NOTIFY
typedef struct
{
	NMHDR		hdr;
	float		red;
	float		green;
	float		blue;
	float		hue;
} CP_NMHDR;


/////////////////////////////////////////////////////////////////////////////
//
// ColorPickerClass window
//
class ColorPickerClass : public CWnd
{
// Construction
public:
	ColorPickerClass();

// Attributes
public:

// Operations
public:

// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(ColorPickerClass)
	public:
	virtual BOOL Create(LPCTSTR lpszClassName, LPCTSTR lpszWindowName, DWORD dwStyle, const RECT& rect, CWnd* pParentWnd, UINT nID, CCreateContext* pContext = NULL);
	//}}AFX_VIRTUAL

// Implementation
public:
	virtual ~ColorPickerClass();

	// Generated message map functions
protected:
	//{{AFX_MSG(ColorPickerClass)
	afx_msg void OnPaint();
	afx_msg int OnCreate(LPCREATESTRUCT lpCreateStruct);
	afx_msg void OnSize(UINT nType, int cx, int cy);
	afx_msg BOOL OnEraseBkgnd(CDC* pDC);
	afx_msg void OnLButtonDown(UINT nFlags, CPoint point);
	afx_msg void OnLButtonUp(UINT nFlags, CPoint point);
	afx_msg void OnMouseMove(UINT nFlags, CPoint point);
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()

	friend LRESULT WINAPI fnColorPickerProc (HWND hwnd, UINT message, WPARAM wparam, LPARAM lparam);

	public:

		//////////////////////////////////////////////////////////////////////////
		//	Public members
		//////////////////////////////////////////////////////////////////////////
		void				Select_Color (int red, int green, int blue);
		void				Get_Current_Color (int *red, int *green, int *blue);

		//////////////////////////////////////////////////////////////////////////
		//	Static members
		//////////////////////////////////////////////////////////////////////////
		static ColorPickerClass *Get_Color_Picker (HWND window_handle)	{  return (ColorPickerClass *)::GetProp (window_handle, "CLASSPOINTER"); }
		
	protected:

		/////////////////////////////////////////////////////////////////////////
		//
		//	Private member data
		//
		void				Paint_DIB (int width, int height, UCHAR *pbits);
		void				Create_Bitmap (void);
		void				Free_Bitmap (void);
		void				Fill_Rect (UCHAR *pbits, const RECT &rect, COLORREF color, int scanline_size);
		void				Frame_Rect (UCHAR *pbits, const RECT &rect, COLORREF color, int scanline_size);
		void				Draw_Horz_Line (UCHAR *pbits, int x, int y, int len, COLORREF color, int scanline_size);
		void				Draw_Vert_Line (UCHAR *pbits, int x, int y, int len, COLORREF color, int scanline_size);
		COLORREF			Color_From_Point (int x, int y);
		CPoint			Point_From_Color (COLORREF color);
		void				Paint_Marker (void);
		void				Erase_Marker (void);
		void				Calc_Display_Rect (RECT &rect);


	private:

		/////////////////////////////////////////////////////////////////////////
		//
		//	Private member data
		//
		HBITMAP			m_hBitmap;
		HDC				m_hMemDC;
		UCHAR	*			m_pBits;
		int				m_iWidth;
		int				m_iHeight;
		CPoint			m_CurrentPoint;
		COLORREF			m_CurrentColor;
		bool				m_bSelecting;
		float				m_CurrentHue;
};

/////////////////////////////////////////////////////////////////////////////

//{{AFX_INSERT_LOCATION}}
// Microsoft Developer Studio will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_COLORPICKER_H__C943A7EC_CAA3_11D2_8DDF_00104B6FD9E3__INCLUDED_)
