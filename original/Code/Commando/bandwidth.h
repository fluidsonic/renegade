#if defined(_MSV_VER)
#pragma once
#endif

#ifndef BANDWIDTH_H
#define BANDWIDTH_H

#include "global.h"

enum BANDWIDTH_TYPE_ENUM {

	BANDWIDTH_FIRST = 100,

	BANDWIDTH_MODEM_288 = BANDWIDTH_FIRST,
	BANDWIDTH_MODEM_336,
	BANDWIDTH_MODEM_56,
	BANDWIDTH_ISDN,
	BANDWIDTH_CABLE,
	BANDWIDTH_LANT1,
	BANDWIDTH_AUTO,
	BANDWIDTH_CUSTOM,

	BANDWIDTH_LAST = BANDWIDTH_CUSTOM
};

//
//
//
class	cBandwidth {
	public:
      cBandwidth(void);
      ~cBandwidth(void) {};

		static uint32_t						Get_Bandwidth_Bps_From_Type(BANDWIDTH_TYPE_ENUM bandwidth_type);
		static const WCHAR *	Get_Bandwidth_String_From_Type(BANDWIDTH_TYPE_ENUM bandwidth_type);
		static BANDWIDTH_TYPE_ENUM		Get_Bandwidth_Type_From_String(LPCSTR bandwidth_string);
};

#endif	// BANDWIDTH_H
