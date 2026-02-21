#include "StdAfx.H"
#include "ProfileSection.H"


/////////////////////////////////////////////////////////////////////////////
//
// Get_Next_Key
//
bool
ProfileSectionClass::Get_Next_Key
(
	CString *key_name,
	CString *key_value
)
{
	// Assume failure
	bool ret_val = false;

	// State OK?
	ASSERT (m_pszCurrentKey);
	if (m_pszCurrentKey &&
	    (m_pszCurrentKey[0] != 0)) {

		// Make a temporary local copy of the key data
		TCHAR local_copy[256];
		::lstrcpyn (local_copy, m_pszCurrentKey, sizeof (local_copy));

		// Get a pointer to the divider
		LPTSTR temp_string = ::strchr (local_copy, '=');
		temp_string[0] = 0;
		temp_string += 1;
		
		//
		//	Strip off the comments (if any)
		//
		LPTSTR comment = ::strchr (local_copy, ';');
		if (comment != NULL) {
			comment[0] = 0;
		}

		comment = ::strchr (temp_string, ';');
		if (comment != NULL) {
			comment[0] = 0;
		}

		// Did the caller want the key name back?
		if (key_name != NULL) {
			// Everything on the left side of the divider is the key name
			*key_name = local_copy;
			key_name->TrimLeft ();
			key_name->TrimRight ();
		}

		// Did the caller want the key value back?
		if (key_value != NULL) {
			// Everything on the right side of the divider is the key value
			*key_value = temp_string;
			key_value->TrimLeft ();
			key_value->TrimRight ();
		}

		// Advance the pointer to point to the next key in the section
		m_pszCurrentKey += ::lstrlen (m_pszCurrentKey) + 1;

		// Success!
		ret_val = true;
	}

	// Return the TRUE/FALSE result code
	return ret_val;
}

