#include "global.h"
#include "cdverify.h"
#include "wwstring.h"
#include "popupdialog.h"
#include "resource.h"

//////////////////////////////////////////////////////////////////////
//	Local constants
//////////////////////////////////////////////////////////////////////
static const char *RENEGADE_MOVIES_VOLUME	= "Renegade Data";

////////////////////////////////////////////////////////////////
//
//	CDVerifyDialogClass
//
////////////////////////////////////////////////////////////////
class CDVerifyDialogClass : public PopupDialogClass
{
public:
	////////////////////////////////////////////////////////////////
	//	Public constructors/destructors
	////////////////////////////////////////////////////////////////
	CDVerifyDialogClass (void)	:
		Object (NULL),
		PopupDialogClass (IDD_CDVERIFY) {}

	void	On_Command (int ctrl_id, int mesage_id, DWORD param);
	void	Set_Object (CDVerifyClass *object)	{ Object = object; }

private:
	////////////////////////////////////////////////////////////////
	//	Private member data
	////////////////////////////////////////////////////////////////
	CDVerifyClass *	Object;
};

//////////////////////////////////////////////////////////////////////
//
//	Get_CD_Path
//
//////////////////////////////////////////////////////////////////////
bool
CDVerifyClass::Get_CD_Path (StringClass &drive_path)
{
	bool retval = false;

	char buffer[1024] = { 0 };
	::GetLogicalDriveStrings (sizeof (buffer), buffer);

	//
	//	Loop over all the drives
	//
	const char *drive_root_name = buffer;
	while (drive_root_name[0] != 0) {

		//
		//	Only check CD drives
		//
		if (::GetDriveType (drive_root_name) == DRIVE_CDROM) {

			//
			//	Get the name of this volume
			//
			char volume_name[256] = { 0 };			
			if (::GetVolumeInformation (drive_root_name, volume_name, sizeof (volume_name),
						NULL, NULL, NULL, NULL, 0))
			{
				int cmp_len	= ::lstrlen (volume_name);
				cmp_len		= max (cmp_len, 11);

				//
				//	Is this the movies CD?
				//
				if (::strnicmp (volume_name, RENEGADE_MOVIES_VOLUME, cmp_len) == 0) {
					retval		= true;
					drive_path	= drive_root_name;
					break;
				}
			}
		}

		//
		//	Advance to the next drive
		//
		drive_root_name += ::lstrlen (drive_root_name) + 1;
	}

	return retval;
}

//////////////////////////////////////////////////////////////////////
//
//	Get_CD_Path
//
//////////////////////////////////////////////////////////////////////
void
CDVerifyClass::Display_UI (Observer<CDVerifyEvent> *observer)
{
	AddObserver (*observer);

	//
	//	Display the dialog
	//
	CDVerifyDialogClass *dialog = new CDVerifyDialogClass;
	dialog->Set_Object (this);
	dialog->Start_Dialog ();
	REF_PTR_RELEASE (dialog);
	return ;
}

////////////////////////////////////////////////////////////////
//
//	On_Command
//
////////////////////////////////////////////////////////////////
void
CDVerifyDialogClass::On_Command (int ctrl_id, int message_id, DWORD param)
{
	//
	//	Check to see if the CD is in the drive now...
	///
	if (ctrl_id == IDOK) {
		StringClass path;
		if (Object->Get_CD_Path (path)) {

			//
			//	Notify anybody who cares that the CD is in the drive
			//
			CDVerifyEvent event (CDVerifyEvent::VERIFIED, Object);
			Object->NotifyObservers (event);

			//
			//	Close the dialog
			//
			End_Dialog ();
		}
	} else if (ctrl_id == IDCANCEL) {
		
		//
		//	Notify anybody who cares that the user has cancelled the operation
		//
		CDVerifyEvent event (CDVerifyEvent::NOT_VERIFIED, Object);
		Object->NotifyObservers (event);

		//
		//	Close the dialog
		//
		End_Dialog ();		
	}

	PopupDialogClass::On_Command (ctrl_id, message_id, param);
	return ;
}
