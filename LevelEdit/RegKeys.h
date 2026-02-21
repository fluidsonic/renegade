#if defined(_MSC_VER)
#pragma once
#endif

#ifndef __REGKEYS_H
#define __REGKEYS_H


////////////////////////////////////////////////////////////////////////////
//
//	Constants
//
////////////////////////////////////////////////////////////////////////////
const TCHAR * const	CONFIG_KEY						= TEXT ("Config");
const TCHAR * const	ASSET_DIR_VALUE				= TEXT ("Asset Tree");
const TCHAR * const	VSSDB_VALUE						= TEXT ("VSS Database");
const TCHAR * const	SHOW_WELCOME_VALUE			= TEXT ("Show Welcome");
const TCHAR * const	CHECKIN_STYLE_VALUE			= TEXT ("Checkin Style");
const TCHAR * const	LAST_EXPORT_DIR_VALUE		= TEXT ("Last export dir");
const TCHAR * const	LAST_SAVE_DIR_VALUE			= TEXT ("Last save dir");
const TCHAR * const	LAST_UPDATE_VALUE				= TEXT ("Last Asset Update");
const TCHAR * const	TEMP_ID_VALUE					= TEXT ("TempID");
const TCHAR * const	NODE_ID_START_VALUE			= TEXT ("NodeIDStart");
const TCHAR * const	IMMEDIATE_CHECKIN_VALUE		= TEXT ("Immed Preset Checkin");
const TCHAR * const	TEXTURE_COMPRESSION_VALUE	= TEXT ("Texture Compression");
const TCHAR * const	CURRENT_PACKAGE_NAME			= TEXT ("Asset Package");

#endif //__REGKEYS_H
