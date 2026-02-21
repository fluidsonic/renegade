
#ifndef __MODPACKAGEMGR_H
#define __MODPACKAGEMGR_H

#include "modpackage.h"

//////////////////////////////////////////////////////////////////////
//
//	ModPackageMgrClass
//
//////////////////////////////////////////////////////////////////////
class ModPackageMgrClass
{
public:

	///////////////////////////////////////////////////////////////////
	//	Public methods
	///////////////////////////////////////////////////////////////////

	//
	//	Initialization
	//
	static void		Initialize (void);
	static void		Shutdown (void);

	//
	//	List support
	//
	static void		Build_List (void);
	static void		Reset_List (void);

	//
	//	Package enumeration
	//
	static int								Get_Package_Count (void)	{ return PackageList.Count (); }
	static ModPackageClass *	Get_Package (int index)		{ return &PackageList[index]; }

	//
	//	Package lookup
	//
	static ModPackageClass *	Find_Package (const char *name);

	//
	//	Current package support
	//
	static ModPackageClass &	Get_Current_Package (void)	{ return CurrentPackage; }
	static void								Set_Current_Package (const char *package_filename);
	static void								Set_Current_Package (int index);

	//
	//	Current MOD loading support
	//
	static void								Load_Current_Mod (void);
	static void								Unload_Current_Mod (void);

	//
	//	Mod/map package name
	//
	static bool								Get_Mod_Map_Name_From_CRC (uint32 mod_crc, uint32 map_crc, StringClass *mod_name, StringClass *map_name);
	static bool								Get_Mod_Map_Name_From_CRC_Index (uint32 mod_file_crc, int map_index, StringClass *mod_name, StringClass *map_name);
	static bool								Find_Filename_From_CRC (const char *search_mask, uint32 filename_crc, StringClass *filename);	
	static bool								Find_Package_From_CRC (uint32 file_crc, StringClass *filename);
	
protected:
	
	///////////////////////////////////////////////////////////////////
	//	Protected methods
	///////////////////////////////////////////////////////////////////
	
	///////////////////////////////////////////////////////////////////
	//	Protected member data
	///////////////////////////////////////////////////////////////////
	static DynamicVectorClass<ModPackageClass>	PackageList;
	static ModPackageClass								CurrentPackage;
};

#endif //__MODPACKAGEMGR_H
