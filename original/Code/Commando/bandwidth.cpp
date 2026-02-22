#include "global.h"
#include "bandwidth.h"  // I WANNA BE FIRST!

#include "miscutil.h"
#include "translatedb.h"
#include "string_ids.h"
#include "useroptions.h"

#include "bandwidthcheck.h"

//-----------------------------------------------------------------------------
uint32_t cBandwidth::Get_Bandwidth_Bps_From_Type(BANDWIDTH_TYPE_ENUM bandwidth_type)
{
	/*
	*/

//	assert(bandwidth_type != BANDWIDTH_CUSTOM);

	switch (bandwidth_type) {
		case BANDWIDTH_CUSTOM:
			return cUserOptions::BandwidthBps.Get();
		case BANDWIDTH_MODEM_288:
			return 28800;
		case BANDWIDTH_MODEM_336:
			return 33600;
		case BANDWIDTH_MODEM_56:
			return 56000;
		case BANDWIDTH_ISDN:
			return 64000;
		case BANDWIDTH_CABLE:
			return 128000;
		case BANDWIDTH_LANT1:
			return 2000000;
		case BANDWIDTH_AUTO:
		{
			uint32_t bps = BandwidthCheckerClass::Get_Upstream_Bandwidth();
//			assert(bps > 0);
			return bps;
		}
		default:
			return 0xffffffff; // to avoid compiler warning
   }
}

//-----------------------------------------------------------------------------
const WCHAR *cBandwidth::Get_Bandwidth_String_From_Type(BANDWIDTH_TYPE_ENUM bandwidth_type)
{
	static char _bandwidth_auto_txt[128];
	static WideStringClass s;
	/*
	*/

	switch (bandwidth_type) {
		case BANDWIDTH_MODEM_288:
			return TRANSLATE(IDS_MP_CONNECTION_288);		//"BANDWIDTH_MODEM_288";
		case BANDWIDTH_MODEM_336:
			return TRANSLATE(IDS_MP_CONNECTION_336);		//"BANDWIDTH_MODEM_336";
		case BANDWIDTH_MODEM_56:
			return TRANSLATE(IDS_MP_CONNECTION_56);		//"BANDWIDTH_MODEM_56";
		case BANDWIDTH_ISDN:
			return TRANSLATE(IDS_MP_CONNECTION_ISDN);		//"BANDWIDTH_ISDN";
		case BANDWIDTH_CABLE:
			return TRANSLATE(IDS_MP_CONNECTION_CABLE);	//"BANDWIDTH_CABLE";
		case BANDWIDTH_LANT1:
			return TRANSLATE(IDS_MP_CONNECTION_T1);		//"BANDWIDTH_LANT1";
		case BANDWIDTH_CUSTOM:
			return u"BANDWIDTH_CUSTOM";
		case BANDWIDTH_AUTO:
		{
			//sprintf(_bandwidth_auto_txt, "BANDWIDTH_AUTO (%s)", BandwidthCheckerClass::Get_Bandwidth_As_String());
			s.Format(TRANSLATE(IDS_MP_CONNECTION_T1), Get_Bandwidth_Bps_From_Type(bandwidth_type));
			//wsprintf(_bandwidth_auto_txt, TRANSLATE(IDS_MP_CONNECTION_T1), Get_Bandwidth_Bps_From_Type(bandwidth_type));
			//return ((const)_bandwidth_auto_txt);
			return(s);
		}
		default:
			return u"ERROR"; // to avoid compiler warning
   }
}

//-----------------------------------------------------------------------------
BANDWIDTH_TYPE_ENUM cBandwidth::Get_Bandwidth_Type_From_String(LPCSTR bandwidth_string)
{

   if (cMiscUtil::Is_String_Same(bandwidth_string, "BANDWIDTH_MODEM_288")) {
      return BANDWIDTH_MODEM_288;
   } else if (cMiscUtil::Is_String_Same(bandwidth_string, "BANDWIDTH_MODEM_336")) {
      return BANDWIDTH_MODEM_336;
   } else if (cMiscUtil::Is_String_Same(bandwidth_string, "BANDWIDTH_MODEM_56")) {
      return BANDWIDTH_MODEM_56;
   } else if (cMiscUtil::Is_String_Same(bandwidth_string, "BANDWIDTH_ISDN")) {
      return BANDWIDTH_ISDN;
   } else if (cMiscUtil::Is_String_Same(bandwidth_string, "BANDWIDTH_CABLE")) {
      return BANDWIDTH_CABLE;
   } else if (cMiscUtil::Is_String_Same(bandwidth_string, "BANDWIDTH_LANT1")) {
      return BANDWIDTH_LANT1;
   } else if (cMiscUtil::Is_String_Same(bandwidth_string, "BANDWIDTH_CUSTOM")) {
      return BANDWIDTH_CUSTOM;
   } else if (strnicmp(bandwidth_string, "BANDWIDTH_AUTO ", 15) == 0) {
      return BANDWIDTH_AUTO;
   } else {
		return BANDWIDTH_CUSTOM; // to avoid compiler warning
   }
}
