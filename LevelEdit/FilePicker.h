#if !defined(AFX_FILEPICKERCLASS_H__1A8EC040_8874_11D2_9FE1_00104B791122__INCLUDED_)
#define AFX_FILEPICKERCLASS_H__1A8EC040_8874_11D2_9FE1_00104B791122__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000

#include "picker.h"

/////////////////////////////////////////////////////////////////////////////
//
// FilePickerClass
//
/////////////////////////////////////////////////////////////////////////////
class FilePickerClass : public PickerClass
{
public:

	//////////////////////////////////////////////////////////////////////////////////
	//	Public constructors/destructor
	//////////////////////////////////////////////////////////////////////////////////
	FilePickerClass (void);
	virtual ~FilePickerClass (void);

	//////////////////////////////////////////////////////////////////////////////////
	//	Public methods
	//////////////////////////////////////////////////////////////////////////////////

	//
	//	From PickerClass
	//	
	void		On_Pick (void);
	
	//
	//	FilePickerClass specific
	//	
	void		Set_Extension_String (LPCTSTR extension)	{ m_ExtensionString = extension; }
	void		Set_Filter_String (LPCTSTR filter)			{ m_FilterString = filter; }
	void		Set_Default_Filename (LPCTSTR filename)	{ m_DefaultFilename = filename; }
	void		Set_Asset_Tree_Only (bool onoff)				{ m_AssetTreeOnly = onoff; }

private:

	//////////////////////////////////////////////////////////////////////////////////
	//	Private member data
	//////////////////////////////////////////////////////////////////////////////////
	CString	m_ExtensionString;
	CString	m_FilterString;
	CString	m_DefaultFilename;
	bool		m_AssetTreeOnly;
};


/////////////////////////////////////////////////////////////////////////////

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_FILEPICKERCLASS_H__1A8EC040_8874_11D2_9FE1_00104B791122__INCLUDED_)
