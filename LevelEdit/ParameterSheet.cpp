// ParameterSheet.cpp : implementation file
//

#include "stdafx.h"
#include "leveledit.h"
#include "ParameterSheet.h"

#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif

/////////////////////////////////////////////////////////////////////////////
// ParameterSheetClass

IMPLEMENT_DYNCREATE(ParameterSheetClass, CScrollView)

ParameterSheetClass::ParameterSheetClass()
{
}

ParameterSheetClass::~ParameterSheetClass()
{
}


BEGIN_MESSAGE_MAP(ParameterSheetClass, CScrollView)
	//{{AFX_MSG_MAP(ParameterSheetClass)
		// NOTE - the ClassWizard will add and remove mapping macros here.
	//}}AFX_MSG_MAP
END_MESSAGE_MAP()

/////////////////////////////////////////////////////////////////////////////
// ParameterSheetClass drawing

void ParameterSheetClass::OnInitialUpdate()
{
	CScrollView::OnInitialUpdate();

	CSize sizeTotal;
	// TODO: calculate the total size of this view
	sizeTotal.cx = sizeTotal.cy = 100;
	SetScrollSizes(MM_TEXT, sizeTotal);
}

void ParameterSheetClass::OnDraw(CDC* pDC)
{
	CDocument* pDoc = GetDocument();
	// TODO: add draw code here
}

/////////////////////////////////////////////////////////////////////////////
// ParameterSheetClass diagnostics

#ifdef _DEBUG
void ParameterSheetClass::AssertValid() const
{
	CScrollView::AssertValid();
}

void ParameterSheetClass::Dump(CDumpContext& dc) const
{
	CScrollView::Dump(dc);
}
#endif //_DEBUG

/////////////////////////////////////////////////////////////////////////////
// ParameterSheetClass message handlers
