#if defined(_MSC_VER)
#pragma once
#endif


#ifndef __EXCEL_H
#define __EXCEL_H

#include "widestring.h"
#include "wwstring.h"


/////////////////////////////////////////////////////////////////////////
//	Forward declarations
/////////////////////////////////////////////////////////////////////////
class _Application;
class Workbooks;
class _Worksheet;
class _Workbook;
class Range;


/////////////////////////////////////////////////////////////////////////
//
//	ExcelClass
//
/////////////////////////////////////////////////////////////////////////
class ExcelClass
{
public:

	//////////////////////////////////////////////////////////
	//	Public methods
	//////////////////////////////////////////////////////////

	//
	//	Initialization
	//
	static bool		Initialize (void);
	static void		Shutdown (void);

	//
	//	Workbook access
	//
	static void		New_Workbook (const char *template_filename);
	static void		Open_Workbook (const char *filename);
	static void		Save_Workbook (const char *filename);
	static void		Close_Workbook (void);

	//
	//	Cell access
	//
	static bool		Get_String (int row, int col, WideStringClass &value);
	static bool		Set_String (int row, int col, const WideStringClass &value);

	static bool		Get_Int (int row, int col, int &value);
	static bool		Set_Int (int row, int col, int value);

private:

	//////////////////////////////////////////////////////////
	//	Private methods
	//////////////////////////////////////////////////////////
	static void		Select_Active_Sheet (void);
	static bool		Get_Cell (int row, int col, VARIANT &result);
	static bool		Set_Cell (int row, int col, const VARIANT &data);

	//////////////////////////////////////////////////////////
	//	Private member data
	//////////////////////////////////////////////////////////
	static _Application *	Application;
	static Workbooks *		WorkbooksObj;
	static _Worksheet *		WorksheetObj;
	static _Workbook *		WorkbookObj;
	static Range *				RangeObj;
	static StringClass		CurrPath;
};


#endif //__EXCEL_H
