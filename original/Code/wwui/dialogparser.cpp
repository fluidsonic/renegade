#include "global.h"
#include "dialogparser.h"
#include "win.h"
#include "translatedb.h"
#include <commctrl.h>

//////////////////////////////////////////////////////////////////////////////
//	Macros
//////////////////////////////////////////////////////////////////////////////
#define ALIGN_WORD_PTR(p)	((WORD *)(((uintptr_t)(p) + 1) & ~(uintptr_t)1))
#define ALIGN_DWORD_PTR(p) ((DWORD *)(((uintptr_t)(p) + 3) & ~(uintptr_t)3))

//////////////////////////////////////////////////////////////////////////////
//	Local prototypes
//////////////////////////////////////////////////////////////////////////////
WORD *Skip_Dlg_Field (WORD *src, WCHAR *buffer = NULL, int buffer_len = 0, WORD *ctrl_type = NULL);

//////////////////////////////////////////////////////////////////////////////
//
//	Skip_Dlg_Field
//
//////////////////////////////////////////////////////////////////////////////
WORD *
Skip_Dlg_Field (WORD *src, WCHAR *buffer, int buffer_len, WORD *ctrl_type)
{
	//
	//	These fields always start on the next word boundary, so align
	//	the source pointer on this boundary.
	//
	WORD *retval = ALIGN_WORD_PTR(src);

	//
	//	Note:  The field codes are as follows:
	//
	//		0xFFFF		- The following WORD is an ordinal value of a system class.
	//		0x0000		- Empty field
	//		Otherwise	- The remaining data is a NULL terminated WCHAR string.
	//
	if (*retval == 0xFFFF) {
		
		//
		//	Move past the field designator
		//
		retval ++;
		
		//
		//	Does the user want information about the ctrl type?
		//
		if (ctrl_type != NULL) {
			*ctrl_type = *retval;
		}

		//
		//	Move past the ctrl type identifier
		//
		retval ++;
	} else if (*retval == 0x0000) {
		
		//
		//	Null terminate the string if the user is expecting data
		//
		if (buffer != NULL) {
			*buffer = 0;
		}

		//
		//	Move past the field designator
		//
		retval ++;
	} else {

		//
		//	The following data is a null-terminated string.  Scan
		// as much data into our desination buffer as possible.
		//	Note:  The data is stored in wide character format.
		//
		while (*retval != 0x0000) {
			if (buffer != NULL && buffer_len > 1) {
				
				//
				//	Store this character in the supplied buffer
				// and decrement the remaining buffer length.
				//
				*buffer++ = *retval;
				buffer_len --;
			}
			retval ++;
		}

		//
		//	Ensure the supplied buffer is NULL terminated
		//
		if (buffer != NULL) {
			*buffer = 0;
		}

		//
		//	Advance to the next field
		//
		retval ++;
	}

	//
	//	Return the new buffer position to the caller
	//
	return retval;
}

//////////////////////////////////////////////////////////////////////////////
//
//	Parse_Template
//
//////////////////////////////////////////////////////////////////////////////
void
DialogParserClass::Parse_Template
(
	int															res_id,
	int *															dlg_width,
	int *															dlg_height,
	WideStringClass *											dlg_title,
	DynamicVectorClass<ControlDefinitionStruct> *	control_list
)
{
	//
	//	Load the resource file
	//
	HRSRC resource		= ::FindResource (ProgramInstance, MAKEINTRESOURCE (res_id), RT_DIALOG);
	HGLOBAL hglobal	= ::LoadResource (ProgramInstance, resource);
	LPVOID res_buffer	= ::LockResource (hglobal);
	if(res_buffer != NULL) {

		//
		//	The first few bytes of the resource buffer are the DLGTEMPLATE structure
		//
		DLGTEMPLATE *dlg_template = (DLGTEMPLATE *)res_buffer;
		(*dlg_width)	= (int)dlg_template->cx;
		(*dlg_height)	= (int)dlg_template->cy;

		//
		//	Move past the DLGTEMPLATE header to the other fields
		//
		WORD *buffer = (WORD *)(((char *)res_buffer) + sizeof (DLGTEMPLATE));
		
		//
		//	Skip the menu, and window class
		//
		buffer = Skip_Dlg_Field (buffer);
		buffer = Skip_Dlg_Field (buffer);

		//
		//	Read the title
		//
		buffer = Skip_Dlg_Field (buffer, dlg_title->Get_Buffer (96), 96);

		WCHAR *string_id = ::wcsstr (dlg_title->Peek_Buffer (), u"IDS_");
		if (string_id != NULL) {
			WideStringClass wide_string_id = string_id;				
			StringClass ascii_string_id;
			wide_string_id.Convert_To (ascii_string_id);
			(*dlg_title) = TRANSLATE_BY_DESC(ascii_string_id);
		}

		//
		//	Do we need to skip past the font settings?
		//
		if (dlg_template->style & DS_SETFONT) {
			buffer ++;
			while (*buffer != 0x0000) {
				buffer ++;
			}
			buffer ++;
		}

		//
		//	Loop over each control and gather information about them
		//
		for (int index = 0; index < dlg_template->cdit; index ++) {
			DLGITEMTEMPLATE *dlg_item_template = (DLGITEMTEMPLATE *)ALIGN_DWORD_PTR((DWORD *)buffer);
			buffer = (WORD *)(((char *)dlg_item_template) + sizeof (DLGITEMTEMPLATE));

			//
			//	Read the ctrl type
			//
			WCHAR text_buffer[256]	= { 0 };
			WORD ctrl_type				= 0x0000;
			buffer = Skip_Dlg_Field (buffer, text_buffer, 256, &ctrl_type);
			
			//
			//	Wasn't one of the standard types, so see if we can determine
			// what it is by its class name.
			//
			if (ctrl_type == 0) {
				::_wcsupr (text_buffer);

				// Standard Win32 control classes (needed when llvm-rc stores
				// CONTROL class names as strings instead of ordinals)
				if (::wcscmp (text_buffer, u"BUTTON") == 0) {
					ctrl_type = BUTTON;
				} else if (::wcscmp (text_buffer, u"EDIT") == 0) {
					ctrl_type = EDIT;
				} else if (::wcscmp (text_buffer, u"STATIC") == 0) {
					ctrl_type = STATIC;
				} else if (::wcscmp (text_buffer, u"COMBOBOX") == 0) {
					ctrl_type = COMBOBOX;
				} else if (::wcscmp (text_buffer, u"SCROLLBAR") == 0) {
					ctrl_type = SCROLL_BAR;
				} else if (::wcscmp (text_buffer, u"LISTBOX") == 0) {
					ctrl_type = LIST_BOX;
				} else if (::wcsstr (text_buffer, u"TRACKBAR") != 0) {
					ctrl_type = SLIDER;
				} else if (::wcsstr (text_buffer, u"TABCONTROL") != 0) {
					ctrl_type = TAB;
				} else if (::wcsstr (text_buffer, u"LISTVIEW") != 0) {
					ctrl_type = LIST_CTRL;
				} else if (::wcsstr (text_buffer, u"MAP") != 0) {
					ctrl_type = MAP;
				} else if (::wcsstr (text_buffer, u"VIEWER") != 0) {
					ctrl_type = VIEWER;
				} else if (::wcsstr (text_buffer, u"HOTKEY") != 0) {
					ctrl_type = HOTKEY;
				} else if (::wcsstr (text_buffer, u"SHORTCUTBAR") != 0) {
					ctrl_type = SHORTCUT_BAR;
				} else if (::wcsstr (text_buffer, u"MERCHANDISE") != 0) {
					ctrl_type = MERCHANDISE_CTRL;
				} else if (::wcsstr (text_buffer, u"TREEVIEW") != 0) {
					ctrl_type = TREE_CTRL;
				} else if (::wcsicmp(text_buffer, PROGRESS_CLASSW) == 0) {
					ctrl_type = PROGRESS_BAR;
				} else if (::wcsstr (text_buffer, u"HEALTHBAR") != 0) {
					ctrl_type = HEALTH_BAR;
				} else if (text_buffer[0] != 0) {
					fprintf(stderr, "[DialogParser] FATAL: Unrecognized control class (id=%d)\n",
					        (int)dlg_item_template->id);
					abort();
				}
			}

			//
			//	Read the window text
			//			
			buffer = Skip_Dlg_Field (buffer, text_buffer, 256);

			WCHAR *string_id = ::wcsstr (text_buffer, u"IDS_");
			if (string_id != NULL) {
				WideStringClass wide_string_id = string_id;				
				StringClass ascii_string_id;
				wide_string_id.Convert_To (ascii_string_id);
				WideStringClass translation = TRANSLATE_BY_DESC(ascii_string_id);
				::wcscpy (string_id, translation);
			}

			//
			//	Add this control definition to the list
			//
			ControlDefinitionStruct definition;
			definition.id		= (int)dlg_item_template->id;
			definition.style	= dlg_item_template->style;
			definition.x		= dlg_item_template->x;
			definition.y		= dlg_item_template->y;
			definition.cx		= dlg_item_template->cx;
			definition.cy		= dlg_item_template->cy;
			definition.type	= (CONTROL_TYPE)ctrl_type;
			definition.title	= text_buffer;
			control_list->Add (definition);

			//
			//	Skip past the extra data
			//
			WORD extra_data_size = *buffer;
			buffer ++;
			if (extra_data_size > 0) {
				buffer = (WORD *)(((char *)ALIGN_WORD_PTR(buffer)) + extra_data_size);
			}
		}
	}

	return ;
}
