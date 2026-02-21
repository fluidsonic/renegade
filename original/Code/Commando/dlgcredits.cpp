#include "dlgcredits.h"
#include "wwfile.h"
#include "ffactory.h"

////////////////////////////////////////////////////////////////
//
//	CreditsMenuClass
//
////////////////////////////////////////////////////////////////
CreditsMenuClass::CreditsMenuClass (void)	:
	MenuDialogClass (IDD_OPTIONS_CREDITS)
{
	return ;
}

////////////////////////////////////////////////////////////////
//
//	On_Init_Dialog
//
////////////////////////////////////////////////////////////////
void
CreditsMenuClass::On_Init_Dialog (void)
{
	const char *CREDITS_TXT_FILENAME	= "credits.txt";

	//
	//	Open the text file
	//
	FileClass *credits_txt_file = _TheFileFactory->Get_File (CREDITS_TXT_FILENAME);
	if (credits_txt_file != NULL && credits_txt_file->Is_Available ()) {
		if (credits_txt_file->Open ()) {
			
			//
			//	Read the data from the file
			//
			int size = credits_txt_file->Size ();
			StringClass ascii_text;
			credits_txt_file->Read (ascii_text.Get_Buffer (size + 1), size);
			ascii_text.Peek_Buffer ()[size] = 0;

			//
			//	Convert the text to wide character format and
			//	strip off any carriage-returns
			//
			WideStringClass wide_text;
			WCHAR *buffer		= wide_text.Get_Buffer (size + 1);
			int dest_index		= 0;

			int len = ascii_text.Get_Length ();
			for (int index = 0; index < len; index ++) {
				if (ascii_text[index] != '\r') {
					buffer[dest_index ++] = (unsigned char)ascii_text[index];
				}
			}
			buffer[dest_index] = 0;

			//
			//	Put the text into the control
			//
			Set_Dlg_Item_Text (IDC_CREDITS_EDIT, wide_text);
		}
		
		//
		//	Close the text file
		//
		_TheFileFactory->Return_File (credits_txt_file);
	}
	
	MenuDialogClass::On_Init_Dialog ();
	return ;
}

////////////////////////////////////////////////////////////////
//
//	On_Command
//
////////////////////////////////////////////////////////////////
void
CreditsMenuClass::On_Command (int ctrl_id, int message_id, DWORD param)
{
	MenuDialogClass::On_Command (ctrl_id, message_id, param);
	return ;
}

