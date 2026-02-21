#if defined(_MSC_VER)
#pragma once
#endif

#ifndef __PROFILE_SECTION_CLASS_H
#define __PROFILE_SECTION_CLASS_H


/////////////////////////////////////////////////////////////////////////////
//
// ProfileSectionClass
//
class ProfileSectionClass
{
	public:

		///////////////////////////////////////////////////
		//
		//	Public constructors/destructors
		//
		ProfileSectionClass (LPCTSTR section_data)
			: m_pszSectionData (section_data) { Before_First (); }

		virtual ~ProfileSectionClass (void) {}


		///////////////////////////////////////////////////
		//
		//	Public methods
		//
		void				Before_First (void) { m_pszCurrentKey = m_pszSectionData; }
		bool				Get_Next_Key (CString *key_name, CString *key_value);

	private:

		///////////////////////////////////////////////////
		//
		//	Private constructors/destructors
		//
		ProfileSectionClass (void) {}

		
		///////////////////////////////////////////////////
		//
		//	Private member data
		//
		LPCTSTR m_pszSectionData;
		LPCTSTR m_pszCurrentKey;
};


#endif //__PROFILE_SECTION_CLASS_H
