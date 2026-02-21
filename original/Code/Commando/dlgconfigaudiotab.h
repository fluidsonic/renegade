
#ifndef __DLGCONFIGAUDIOTAB_H
#define __DLGCONFIGAUDIOTAB_H


//////////////////////////////////////////////////////////////////////
//	Forward declarations
//////////////////////////////////////////////////////////////////////
class AudibleSoundClass;


#include "childdialog.h"


//////////////////////////////////////////////////////////////////////
//
//	DlgConfigAudioTabClass
//
//////////////////////////////////////////////////////////////////////
class DlgConfigAudioTabClass : public ChildDialogClass
{
public:

	///////////////////////////////////////////////////////////////////
	//	Public constructors/destructors
	///////////////////////////////////////////////////////////////////
	DlgConfigAudioTabClass  (void);
	~DlgConfigAudioTabClass (void);

	///////////////////////////////////////////////////////////////////
	//	Public methods
	///////////////////////////////////////////////////////////////////

	//
	//	Inherited
	//
	void		On_Init_Dialog (void);
	void		On_Command (int ctrl_id, int message_id, DWORD param);
	bool		On_Apply (void);
	void		On_Frame_Update (void);
	void		On_SliderCtrl_Pos_Changed (SliderCtrlClass *slider_ctrl, int ctrl_id, int new_pos);
	
protected:
	
	///////////////////////////////////////////////////////////////////
	//	Protected methods
	///////////////////////////////////////////////////////////////////
	void		Update_Slider_Enable_State (void);
	void		Configure_Quality_Combobox (void);
	void		Configure_Rate_Combobox (void);
	void		Configure_Speaker_Combobox (void);
	void		Configure_Driver_List (void);
	void		Set_Default_Volumes (void);

	///////////////////////////////////////////////////////////////////
	//	Protected member data
	///////////////////////////////////////////////////////////////////
	int		InitialDeviceIndex;
	int		InitialHertz;
	int		InitialBits;
	bool		InitialIsStereo;

	AudibleSoundClass *	SoundVolumeTestSound;
	AudibleSoundClass *	MusicVolumeTestSound;
	AudibleSoundClass *	DialogVolumeTestSound;
	AudibleSoundClass *	CinematicVolumeTestSound;

	int						SoundVolumeTestSoundStartTime;
	int						MusicVolumeTestSoundStartTime;
	int						DialogVolumeTestSoundStartTime;
	int						CinematicVolumeTestSoundStartTime;
};


#endif //__DLGCONFIGAUDIOTAB_H
