#include "dlgevavehiclestab.h"



////////////////////////////////////////////////////////////////
//
//	On_Init_Dialog
//
////////////////////////////////////////////////////////////////
void
EvaVehiclesTabClass::On_Init_Dialog (void)
{
	Set_Encyclopedia_Type (EncyclopediaMgrClass::TYPE_VEHICLE);

	ListCtrlClass *list_ctrl				= (ListCtrlClass *)Get_Dlg_Item (IDC_LIST_CTRL);
	DialogTextClass *affiliation_ctrl	= (DialogTextClass *)Get_Dlg_Item (IDC_AFFILIATION_STATIC);
	DialogTextClass *description_ctrl	= (DialogTextClass *)Get_Dlg_Item (IDC_DESCRIPTION_STATIC);
	ViewerCtrlClass *viewer_ctrl			= (ViewerCtrlClass *)Get_Dlg_Item (IDC_VIEWER_CTRL);

	//
	//	Let the base class know which controls to use
	//
	Set_List_Ctrl (list_ctrl);
	Set_Description_Ctrl (description_ctrl);
	Set_Affiliation_Ctrl (affiliation_ctrl);
	Set_Viewer_Ctrl (viewer_ctrl);

	//
	//	Let the base class know where to get its data
	//
	Set_INI_Filename ("vehicles.ini");
	
	EvaViewerTabClass::On_Init_Dialog ();
	return ;
}
