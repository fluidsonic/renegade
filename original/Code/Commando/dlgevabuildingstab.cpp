#include "dlgevabuildingstab.h"



////////////////////////////////////////////////////////////////
//
//	On_Init_Dialog
//
////////////////////////////////////////////////////////////////
void
EvaBuildingsTabClass::On_Init_Dialog (void)
{
	Set_Encyclopedia_Type (EncyclopediaMgrClass::TYPE_BUILDING);

	ListCtrlClass *list_ctrl				= (ListCtrlClass *)Get_Dlg_Item (IDC_LIST_CTRL);
	DialogTextClass *description_ctrl	= (DialogTextClass *)Get_Dlg_Item (IDC_DESCRIPTION_STATIC);
	ViewerCtrlClass *viewer_ctrl			= (ViewerCtrlClass *)Get_Dlg_Item (IDC_VIEWER_CTRL);

	//
	//	Let the base class know which controls to use
	//
	Set_List_Ctrl (list_ctrl);
	Set_Description_Ctrl (description_ctrl);
	Set_Viewer_Ctrl (viewer_ctrl);

	//
	//	Let the base class know where to get its data
	//
	Set_INI_Filename ("buildings.ini");
	
	EvaViewerTabClass::On_Init_Dialog ();
	return ;
}
