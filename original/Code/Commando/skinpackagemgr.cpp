#include "skinpackagemgr.h"
#include "registry.h"
#include "_globals.h"

/////////////////////////////////////////////////////////////////////
//	Local constants
/////////////////////////////////////////////////////////////////////
static const char *	SKIN_DICTIONARY_FILENAME	= "skindictionary.ini";
static const char *	CURR_SKIN_REG_VALUE			= "CurrSkinPackage";

//////////////////////////////////////////////////////////////////////
//	Static member initialization
//////////////////////////////////////////////////////////////////////
DynamicVectorClass<SkinPackageClass>	SkinPackageMgrClass::PackageList;
SkinPackageClass								SkinPackageMgrClass::CurrentPackage;

/////////////////////////////////////////////////////////////////////
//
//	Initialize
//
//////////////////////////////////////////////////////////////////////
void
SkinPackageMgrClass::Initialize (void)
{
	Shutdown ();

	//
	//	Get the currently selected package name from the registry
	//
	RegistryClass registry (APPLICATION_SUB_KEY_NAME_OPTIONS);
	if (registry.Is_Valid ()) {

		//
		//	Get the current package name from the registry
		//
		StringClass package_filename;
		registry.Get_String (CURR_SKIN_REG_VALUE, package_filename, "default.pkg");

		//
		//	Initialize the current package
		//
		CurrentPackage.Set_Package_Filename (package_filename);
	}

	return ;
}

/////////////////////////////////////////////////////////////////////
//
//	Shutdown
//
//////////////////////////////////////////////////////////////////////
void
SkinPackageMgrClass::Shutdown (void)
{
	Reset_List ();
	return ;
}

//////////////////////////////////////////////////////////////////////
//
//	Build_List
//
//////////////////////////////////////////////////////////////////////
void
SkinPackageMgrClass::Build_List (void)
{
	WIN32_FIND_DATA find_info	= { 0 };
	BOOL keep_going				= TRUE;
	HANDLE file_find				= NULL;

	//
	//	Build a list of all the saved games we know about
	//
	for (file_find = ::FindFirstFile ("*.pkg", &find_info);
		 (file_find != INVALID_HANDLE_VALUE) && keep_going;
		  keep_going = ::FindNextFile (file_find, &find_info))
	{		
		//
		//	Create the package from the data in this mix file
		//
		SkinPackageClass package;
		package.Set_Package_Filename (find_info.cFileName);

		//
		//	Add the package to our list
		//
		PackageList.Add (package);
	}

	if (file_find != INVALID_HANDLE_VALUE) {			  
		::FindClose (file_find); 
	}
	
	return ;
}

//////////////////////////////////////////////////////////////////////
//
//	Reset_List
//
//////////////////////////////////////////////////////////////////////
void
SkinPackageMgrClass::Reset_List (void)
{
	PackageList.Delete_All ();
	return ;
}

//////////////////////////////////////////////////////////////////////
//
//	Set_Current_Package
//
//////////////////////////////////////////////////////////////////////
void
SkinPackageMgrClass::Set_Current_Package (const char *package_filename)
{
	//
	//	Initialize the current package
	//
	CurrentPackage.Set_Package_Filename (package_filename);

	//
	//	Write the name of hte package to the registry
	//
	RegistryClass registry (APPLICATION_SUB_KEY_NAME_OPTIONS);
	if (registry.Is_Valid ()) {
		registry.Set_String (CURR_SKIN_REG_VALUE, package_filename);
	}

	return ;
}

//////////////////////////////////////////////////////////////////////
//
//	Set_Current_Package
//
//////////////////////////////////////////////////////////////////////
void
SkinPackageMgrClass::Set_Current_Package (int index)
{
	Set_Current_Package (PackageList[index].Get_Package_Filename ());
	return ;
}
