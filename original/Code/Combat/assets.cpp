#include "assets.h"
#include "wwfile.h"
#include "debug.h"
#include "ffactory.h"

/*
**
*/
INIClass *	Get_INI( const char * filename )
{
	INIClass * ini = NULL;

	FileClass * INIfile = _TheFileFactory->Get_File( filename );
	if ( INIfile ) {
		if ( INIfile->Is_Available() ) {
			ini = new INIClass( *INIfile );
		}
		_TheFileFactory->Return_File( INIfile );
	}

	return ini;
}

void Save_INI( INIClass * p_ini, const char * filename )
{

	FileClass * p_INIfile = _TheWritingFileFactory->Get_File(filename);
	if (p_INIfile != NULL && p_INIfile->Is_Available()) {
		p_ini->Save(*p_INIfile);
		_TheWritingFileFactory->Return_File(p_INIfile);
	}
}

void	Release_INI( INIClass * ini )
{
	delete ini;
}

/*
**
*/
void Strip_Path_From_Filename( StringClass& new_name, const char * filename )
{
	if ( ::strchr( filename, '\\' ) != 0 ) {
		new_name = ::strrchr( filename, '\\' ) + 1;
	}
	else new_name=filename;
}

/*
** Asset access
*/
void Get_Render_Obj_Name_From_Filename( StringClass& new_name, const char * filename )
{
	Strip_Path_From_Filename( new_name, filename );
	if ( new_name.Get_Length() > 4 ) {
		new_name.Erase( new_name.Get_Length() - 4, 4 );
	}
}

RenderObjClass * Create_Render_Obj_From_Filename( const char * filename )
{
	StringClass	render_obj_name(true);
	Strip_Path_From_Filename( render_obj_name, filename );
	render_obj_name.Erase( render_obj_name.Get_Length() - 4, 4 );

	RenderObjClass *model = WW3DAssetManager::Get_Instance()->Create_Render_Obj( render_obj_name );
	if ( model == NULL ) {
		Debug_Say(( "Failed to create \"%s\" from \"%s\"\n", (const char *)render_obj_name, filename ));
	}
	return model;
}

TextureClass * Get_Texture_From_Filename
( 
	const char * filename, 
	TextureClass::MipCountType mip_level_count 
)
{
	StringClass	tex_name(true);
	Strip_Path_From_Filename( tex_name, filename );
	return WW3DAssetManager::Get_Instance()->Get_Texture( tex_name, mip_level_count );
}



/*
** Filenames
*/
void Create_Animation_Name( StringClass& anim_name, const char * anim_filename, const char * model_name )
{
	anim_name=anim_filename;
	if ( ::strrchr( anim_name, '\\' ) != 0 ) {
		StringClass temp(::strrchr( anim_name, '\\' ) + 1,true);
		anim_name = temp;
		anim_name.Erase( anim_name.Get_Length() - 4, 4 );	// Strip off ".w3d"
	}

	if ( ::strchr( anim_name, '.' ) == 0 ) {	// Add model name
		StringClass temp(true);
		temp.Format( "%s.%s", model_name, anim_name );
		anim_name = temp;
	}
}
