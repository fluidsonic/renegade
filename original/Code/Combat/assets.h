#pragma once

#include "global.h"


#include "assetmgr.h"

#include "ini.h"

/*
** INI File Access
*/
void			Set_INI_Path( const char * path );
INIClass *	Get_INI( const char * filename );
void			Save_INI( INIClass * p_ini, const char * filename );
void			Release_INI( INIClass * ini );

/*
** Path Striping
*/
void Strip_Path_From_Filename( StringClass& new_name, const char * filename );
void Get_Render_Obj_Name_From_Filename( StringClass& new_name, const char * filename );

/*
** Asset access - these functions strips the path off of the filenames; for use
** with filenames that come from preset definitions.
*/
RenderObjClass * Create_Render_Obj_From_Filename( const char * filename );
TextureClass * Get_Texture_From_Filename(	const char * filename, 
														TextureClass::MipCountType mip_level_count=TextureClass::MIP_LEVELS_ALL );

/*
** Filenames
*/
void Create_Animation_Name( StringClass& anim_name, const char * anim_filename, const char * model_name );
