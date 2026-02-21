#if defined(_MSC_VER)
#pragma once
#endif

#ifndef __EDITORINI_H
#define __EDITORINI_H

#include "INI.H"
#include "Vector3.H"
#include "Vector4.H"
#include "Matrix3D.H"


////////////////////////////////////////////////////////////////////////////////
//
//	EditorINIClass
//
class EditorINIClass : public INIClass
{
	public:

		EditorINIClass (void)
			: INIClass () {}

		EditorINIClass (FileClass &file)
			: INIClass (file) {}

		//
		//	Read methods
		//
		Vector3	Get_Vector3 (LPCTSTR section, LPCTSTR entry, const Vector3 &defvalue = Vector3 (0,0,0)) const;
		Vector4	Get_Vector4 (LPCTSTR section, LPCTSTR entry, const Vector4 &defvalue = Vector4 (0,0,0,0)) const;
		CString	Get_CString (LPCTSTR section, LPCTSTR entry, LPCTSTR defvalue = "") const;
		Matrix3D	Get_Matrix3D (LPCTSTR section, LPCTSTR entry_base, Matrix3D &defvalue = Matrix3D(1)) const;

		//
		//	Write methods
		//
		bool		Put_Vector3 (LPCTSTR section, LPCTSTR entry, const Vector3 &value);
		bool		Put_Vector4 (LPCTSTR section, LPCTSTR entry, const Vector4 &value);
		bool		Put_Matrix3D (LPCTSTR section, LPCTSTR entry_base, const Matrix3D &value);
};

#endif //__EDITORINI_H

