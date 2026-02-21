#if !defined(AFX_PARAMETERSHEET_H__9CC8BAEA_7052_11D3_A05A_00104B791122__INCLUDED_)
#define AFX_PARAMETERSHEET_H__9CC8BAEA_7052_11D3_A05A_00104B791122__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000
// ParameterSheet.h : header file
//

/////////////////////////////////////////////////////////////////////////////
// ParameterSheetClass view

class ParameterSheetClass : public CScrollView
{
protected:
	ParameterSheetClass();           // protected constructor used by dynamic creation
	DECLARE_DYNCREATE(ParameterSheetClass)

// Attributes
public:

// Operations
public:

// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(ParameterSheetClass)
	protected:
	virtual void OnDraw(CDC* pDC);      // overridden to draw this view
	virtual void OnInitialUpdate();     // first time after construct
	//}}AFX_VIRTUAL

// Implementation
protected:
	virtual ~ParameterSheetClass();
#ifdef _DEBUG
	virtual void AssertValid() const;
	virtual void Dump(CDumpContext& dc) const;
#endif

	// Generated message map functions
	//{{AFX_MSG(ParameterSheetClass)
		// NOTE - the ClassWizard will add and remove member functions here.
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()
};

/////////////////////////////////////////////////////////////////////////////

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_PARAMETERSHEET_H__9CC8BAEA_7052_11D3_A05A_00104B791122__INCLUDED_)
