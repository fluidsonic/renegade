
#include "global.h"
#include	<windows.h>
#include "systimer.h"
#include	<stddef.h>

#include "bandwidthcheck.h"
#include "registry.h"
#include "_globals.h"
#include "useroptions.h"
#include "translatedb.h"
#include "string_ids.h"
#include "consolemode.h"

/*
** Class statics.
*/
BandwidthCheckerClass::BandwidthCheckerThreadClass BandwidthCheckerClass::Thread;
HANDLE BandwidthCheckerClass::EventNotify = NULL;
unsigned long BandwidthCheckerClass::UpstreamBandwidth = 0;
unsigned long BandwidthCheckerClass::ReportedUpstreamBandwidth = 0;
const WCHAR *BandwidthCheckerClass::UpstreamBandwidthString = NULL;
unsigned long BandwidthCheckerClass::DownstreamBandwidth = 0;
unsigned long BandwidthCheckerClass::ReportedDownstreamBandwidth = 0;
const WCHAR *BandwidthCheckerClass::DownstreamBandwidthString = NULL;
int BandwidthCheckerClass::FailureCode = BANDTEST_OK;
bool BandwidthCheckerClass::GotBandwidth = false;
const char *BandwidthCheckerClass::DefaultServerName = "www.westwood.com";

/*
** Possible error codes from the bandwidth test.
*/
char *BandwidthCheckerClass::ErrorList[13] = {
	"BANDTEST_OK",
	"BANDTEST_NO_WINSOCK2",
	"BANDTEST_NO_RAW_SOCKET_PERMISSION",
	"BANDTEST_NO_RAW_SOCKET_CREATE",
	"BANDTEST_NO_UDP_SOCKET_BIND",
	"BANDTEST_NO_TTL_SET",
	"BANDTEST_NO_PING_RESPONSE",
	"BANDTEST_NO_FINAL_PING_TIME",
	"BANDTEST_NO_EXTERNAL_ROUTER",
	"BANDTEST_NO_IP_DETECT",
	"BANDTEST_UNKNOWN_ERROR",
	"BANDTEST_WRONG_API_VERSION",
	"BANDTEST_BAD_PARAM"
};

#define NUM_BANDS 12
// FDS: bandwidth defaulted to 1 Mbps and showed server-mode UI
#define DEFAULT_BAND 3

/*
** Lower and upper limits for each level of bandwidth.
*/
unsigned long BandwidthCheckerClass::Bandwidths[NUM_BANDS * 2] = {
	 12000,	14400,
	 28000,	28800,
	 29999,	33600,
	 53000,	57600,
	 62000,	67200,
	105000,	115200,
	125000,	128000,
	250000,	256000,
	500000,	512000,
	999999,	1024000,
	1999999,	2048000,
	3999999,	4096000
};

/*
** Human readable names for each bandwidth level.
*/
const WCHAR *BandwidthCheckerClass::BandwidthNames [NUM_BANDS+1] = {
	u"14400",
	u"28800",
	u"33600",
	u"57600",
	u"67200",
	u"115200",
	u"128k",
	u"256k",
	u"512k",
	u"1M",
	u"2M",
	u"4M",
	u"> 4M"
};

/***********************************************************************************************
 * Start the bandwidth detection thread                                                        *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    Nothing                                                                           *
 *                                                                                             *
 * OUTPUT:   Nothing                                                                           *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   11/21/2001 2:53PM ST : Created                                                            *
 *=============================================================================================*/
void BandwidthCheckerClass::Check_Now(HANDLE event)
{
	EventNotify = event;

	/*
	** If the thread didn't finish for some reason then we need to take action.
	** This will stall the dialogs but at least it won't crash.
	*/
	if (Thread.Is_Running()) {
		unsigned long timeout = 10 * 1000;
		unsigned long time = TIMEGETTIME();
		while (Thread.Is_Running() && (TIMEGETTIME() - time) < timeout) {
			Sleep(1);
		}
	}
	if (Thread.Is_Running()) {
		Thread.Stop(2000);
	}
	Thread.Execute();
}

/***********************************************************************************************
 * BandwidthCheckerClass::Get_Ping_Server_Name -- Get the name of a server to ping             *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    Nothing                                                                           *
 *                                                                                             *
 * OUTPUT:   Ptr to server name                                                                *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   11/27/2001 1:17PM ST : Created                                                            *
 *=============================================================================================*/
const char *BandwidthCheckerClass::Get_Ping_Server_Name(void)
{
	static char server_name_copy[128];
	DynamicVectorClass<StringClass> list;
	const char *server_name = DefaultServerName;

	/*
	** See if there are ping servers in the registry from a previous run.
	*/
	RegistryClass reg(APPLICATION_SUB_KEY_NAME_SERVER_LIST);
	reg.Get_Value_List(list);
	if (list.Count() > 0) {

		/*
		** Get average and lowest ping server ping time.
		*/
		int num_times = 0;
		unsigned long total = 0;
		int lowest = 0xffff;
		int lowest_index = -1;
		for (int i=0 ; i<list.Count() ; i++) {
			int time = reg.Get_Int(list[i].Peek_Buffer(), 0);
			if (time > 0 && time < 0xffff) {
				total += (unsigned long) time;
				num_times++;

				if (time < lowest) {
					lowest = time;
					lowest_index = i;
				}
			}
		}

		if (num_times) {
			int average_time = (int) (total / (unsigned) num_times);

			/*
			** Pick one a little closer.
			*/
			average_time = average_time / 2;

			/*
			** Find the server closest to the average time.
			*/
			int closest_index = -1;
			int closest_diff = 0x7fffffff;

			for (int i=0 ; i<list.Count() ; i++) {
				int time = reg.Get_Int(list[i].Peek_Buffer(), 0);
				if (time > 0 && time < 0xffff) {
					int diff = abs(time - average_time);
					if (diff < closest_diff) {

						/*
						** Ignore the nearest server.
						*/
						if (i != lowest_index) {
							closest_diff = diff;
							closest_index = i;
						}
					}
				}
			}

			if (closest_index != -1) {
				strncpy(server_name_copy, list[closest_index].Peek_Buffer(), sizeof(server_name_copy));
				server_name_copy[sizeof(server_name_copy) - 1] = 0;
				server_name = (const char*) server_name_copy;
			}
		}
	}
	return(server_name);
}

/***********************************************************************************************
 * BandwidthCheckerClass::Check -- Check bandwidth. This is called from bandwidth thread.      *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    Nothing                                                                           *
 *                                                                                             *
 * OUTPUT:   Nothing                                                                           *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   11/21/2001 2:54PM ST : Created                                                            *
 *=============================================================================================*/
void BandwidthCheckerClass::Check(void)
{
	struct hostent *host;
	struct sockaddr_in address;
	int failure_code;

	ConsoleBox.Print("Detecting bandwidth...\n");

	const char *host_name = Get_Ping_Server_Name();

	host = gethostbyname(host_name);
	if (host == NULL) {
		host_name = DefaultServerName;
		host = gethostbyname(host_name);
	}

	if (host == NULL) {
		/*
		** No DNS or no connection at all. Either way we are in trouble.
		*/
		ConsoleBox.Print("Unable to resolve host name for bandwidth check\n");
// FDS: bandwidth defaulted to 1 Mbps and showed server-mode UI
		UpstreamBandwidth = 55000;
		DownstreamBandwidth = 55000;
		ReportedUpstreamBandwidth = 57600;
		ReportedDownstreamBandwidth = 57600;
		UpstreamBandwidthString = BandwidthNames[DEFAULT_BAND];
		DownstreamBandwidthString = BandwidthNames[DEFAULT_BAND];
		FailureCode = BANDTEST_NO_IP_DETECT;
		GotBandwidth = false;
	} else {

		memcpy(&(address.sin_addr), host->h_addr, host->h_length);

		/*
		** Init the settings for the detection.
		*/
		BandtestSettingsStruct settings = {
			0,		//AlwaysICMP
			0,		//TTLScatter
			50,	//FastPingPackets
			12,	//SlowPingPackets
			25,	//Fast ping threshold
			0		//PingProfile
		};

		/*
		** Call the .dll function to do the actual detection.
		*/
		UpstreamBandwidth = Detect_Bandwidth(ntohl(address.sin_addr.s_addr), 0, 2, failure_code, DownstreamBandwidth, BANDTEST_API_VERSION, &settings, APPLICATION_SUB_KEY_NAME_BANDTEST);

		/*
		** If we failed due to a missing final ping then try again with fewer packets and no retries.
		*/
		if (UpstreamBandwidth == 0) {
			if (failure_code == BANDTEST_NO_FINAL_PING_TIME) {
				BandtestSettingsStruct settings = {
					0,		//AlwaysICMP
					0,		//TTLScatter
					25,	//FastPingPackets
					8,		//SlowPingPackets
					25,	//Fast ping threshold
					0		//PingProfile
				};

				UpstreamBandwidth = Detect_Bandwidth(ntohl(address.sin_addr.s_addr), 0, 0, failure_code, DownstreamBandwidth, BANDTEST_API_VERSION, &settings);
			}
		}

		/*
		** If it's 0, we failed.
		*/
		if (UpstreamBandwidth == 0) {
			/*
			** Default to 57600.
			*/
// FDS: bandwidth defaulted to 1 Mbps and showed server-mode UI
			UpstreamBandwidth = 55000;
			DownstreamBandwidth = 55000;
			FailureCode = failure_code;
			GotBandwidth = true;	//false;
		} else {
			GotBandwidth = true;
		}

		/*
		** Fix up the upstream bandwidth into one of our connection type bands.
		*/
		if (UpstreamBandwidth > 0x7fffffff) {
			UpstreamBandwidth = 4096000;
			ReportedUpstreamBandwidth = 4096000;
			UpstreamBandwidthString = BandwidthNames[NUM_BANDS];
		} else {
			if (UpstreamBandwidth > 100000) {
			} else {
			}

			bool got_bw_str = false;
			for (int i=0 ; i<NUM_BANDS ; i++) {
				if (UpstreamBandwidth < Bandwidths[(i*2) + 1]) {
					//
					UpstreamBandwidthString = BandwidthNames[i];
					ReportedUpstreamBandwidth = Bandwidths[(i*2)+1];
					got_bw_str = true;
					break;
				}
			}
			if (!got_bw_str) {
				ReportedUpstreamBandwidth = 4096000;
				UpstreamBandwidthString = BandwidthNames[NUM_BANDS];
			}
			StringClass little_string;
			WideStringClass(UpstreamBandwidthString, true).Convert_To(little_string);

			ConsoleBox.Print("Upstream bandwidth of %s bps detected\n", little_string.Peek_Buffer());
		}

		/*
		** Fix up the downstream bandwidth into one of our connection type bands.
		*/
		if (DownstreamBandwidth > 0x7fffffff) {
			DownstreamBandwidth = 4096000;
			ReportedDownstreamBandwidth = 4096000;
			DownstreamBandwidthString = BandwidthNames[NUM_BANDS];
		} else {
			if (DownstreamBandwidth > 100000) {
			} else {
			}

			bool got_bw_str = false;
			for (int i=0 ; i<NUM_BANDS ; i++) {
				if (DownstreamBandwidth < Bandwidths[(i*2) + 1]) {
					//
					DownstreamBandwidthString = BandwidthNames[i];
					ReportedDownstreamBandwidth = Bandwidths[(i*2)+1];
					got_bw_str = true;
					break;
				}
			}
			if (!got_bw_str) {
				ReportedDownstreamBandwidth = 4096000;
				DownstreamBandwidthString = BandwidthNames[NUM_BANDS];
			}
		}

		/*
		** Testy.
		*/

	}
	SetEvent(EventNotify);
}

/***********************************************************************************************
 * BandwidthCheckerClass::Force_Upstream_Bandwidth -- Set actual upstream bandwidth              *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:   Upstream bandwidth in buts per second                                             *
 *                                                                                             *
 * OUTPUT:    Nothing                                                                           *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   11/21/2001 2:54PM ST : Created                                                            *
 *=============================================================================================*/
void BandwidthCheckerClass::Force_Upstream_Bandwidth(unsigned int up)
{

	UpstreamBandwidth = up;
}

/***********************************************************************************************
 * BandwidthCheckerClass::Get_Upstream_Bandwidth -- Get actual upstream bandwidth              *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    Nothing                                                                           *
 *                                                                                             *
 * OUTPUT:   Upstream bandwidth in buts per second                                             *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   11/21/2001 2:54PM ST : Created                                                            *
 *=============================================================================================*/
unsigned long BandwidthCheckerClass::Get_Upstream_Bandwidth(void)
{
	return(UpstreamBandwidth);
}

/***********************************************************************************************
 * BandwidthCheckerClass::Get_Reported_Upstream_Bandwidth -- Get reported up bandwidth         *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    Nothing                                                                           *
 *                                                                                             *
 * OUTPUT:   Reported upstream bandwidth in bits per second                                    *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   11/21/2001 2:55PM ST : Created                                                            *
 *=============================================================================================*/
unsigned long BandwidthCheckerClass::Get_Reported_Upstream_Bandwidth(void)
{
	return(ReportedUpstreamBandwidth);
}

/***********************************************************************************************
 * BandwidthCheckerClass::Get_Upstream_Bandwidth_As_String -- Human readable upsream bw        *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    Nothing                                                                           *
 *                                                                                             *
 * OUTPUT:   Ptr to bandwidth string                                                           *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   11/21/2001 2:56PM ST : Created                                                            *
 *=============================================================================================*/
const WCHAR *BandwidthCheckerClass::Get_Upstream_Bandwidth_As_String(void)
{
	return(UpstreamBandwidthString);
}

/***********************************************************************************************
 * BandwidthCheckerClass::Get_Downstream_Bandwidth -- Get actual downstream bandwidth          *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    Nothing                                                                           *
 *                                                                                             *
 * OUTPUT:   Actual down bw in bits per second                                                 *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   11/21/2001 2:56PM ST : Created                                                            *
 *=============================================================================================*/
unsigned long BandwidthCheckerClass::Get_Downstream_Bandwidth(void)
{
	return(DownstreamBandwidth);
}

/***********************************************************************************************
 * BandwidthCheckerClass::Get_Reported_Downstream_Bandwidth -- Get reported down bw            *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    Nothing                                                                           *
 *                                                                                             *
 * OUTPUT:   Reported down bw in bits per second                                               *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   11/21/2001 2:57PM ST : Created                                                            *
 *=============================================================================================*/
unsigned long BandwidthCheckerClass::Get_Reported_Downstream_Bandwidth(void)
{
	return(ReportedDownstreamBandwidth);
}

/***********************************************************************************************
 * BandwidthCheckerClass::Get_Downstream_Bandwidth_As_String -- Get down bw as a string        *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    Nothing                                                                           *
 *                                                                                             *
 * OUTPUT:   Ptr to down bw string                                                             *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   11/21/2001 2:57PM ST : Created                                                            *
 *=============================================================================================*/
const WCHAR *BandwidthCheckerClass::Get_Downstream_Bandwidth_As_String(void)
{
	return(DownstreamBandwidthString);
}

/***********************************************************************************************
 * BandwidthCheckerClass::Get_Bandwidth_As_String -- Get bandwidth description string          *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    Nothing                                                                           *
 *                                                                                             *
 * OUTPUT:   Ptr to string                                                                     *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   11/21/2001 2:58PM ST : Created                                                            *
 *=============================================================================================*/
const WCHAR *BandwidthCheckerClass::Get_Bandwidth_As_String(void)
{

	if (cUserOptions::Get_Bandwidth_Type() == BANDWIDTH_AUTO) {
		static char16_t _build_string[256];
		swprintf(_build_string, 256, u"%s,%s", DownstreamBandwidthString, UpstreamBandwidthString);
		return(_build_string);
	} else {
		return cBandwidth::Get_Bandwidth_String_From_Type(
			(BANDWIDTH_TYPE_ENUM)cUserOptions::Get_Bandwidth_Type());
	}
}

/***********************************************************************************************
 * BandwidthCheckerClass::Get_Bandwidth_As_String -- Get bandwidth description string          *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    Packed up/down bandwidth                                                          *
 *                                                                                             *
 * OUTPUT:   Ptr to bandwidth description string                                               *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   11/21/2001 2:58PM ST : Created                                                            *
 *=============================================================================================*/
const WCHAR *BandwidthCheckerClass::Get_Bandwidth_As_String(PackedBandwidthType bandwidth)
{
	static char16_t _build_string[256];

	assert(bandwidth.Bandwidth.Up < NUM_BANDS + 1);
	assert(bandwidth.Bandwidth.Down < NUM_BANDS + 1);

	swprintf(_build_string, 256, u"%s,%s", BandwidthNames[bandwidth.Bandwidth.Down], BandwidthNames[bandwidth.Bandwidth.Up]);
	return(_build_string);
}

/***********************************************************************************************
 * BandwidthCheckerClass::Get_Packed_Bandwidth -- Get bandwidth packed into a byte             *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    Nothing                                                                           *
 *                                                                                             *
 * OUTPUT:   Bandwidth as a byte                                                               *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   11/21/2001 2:59PM ST : Created                                                            *
 *=============================================================================================*/
BandwidthCheckerClass::PackedBandwidthType BandwidthCheckerClass::Get_Packed_Bandwidth(void)
{
	PackedBandwidthType bandwidth = {0,0};
	assert(sizeof(bandwidth) == 1);

	unsigned long bwu = ReportedUpstreamBandwidth;
	unsigned long bwd = ReportedDownstreamBandwidth;
	bool automode = true;

	if (cUserOptions::Get_Bandwidth_Type() != BANDWIDTH_AUTO) {
		bwu = cBandwidth::Get_Bandwidth_Bps_From_Type((BANDWIDTH_TYPE_ENUM)cUserOptions::Get_Bandwidth_Type());
		bwd = bwu;
		automode = false;
	}

	/*
	** Just return 0s if we haven't detected bandwidth yet.
	*/
	if (!automode || UpstreamBandwidth != 0) {

		for (int i=0 ; i<NUM_BANDS+1 ; i++) {
			if (bwu <= Bandwidths[(i*2)+1]) {
				bandwidth.Bandwidth.Up = i;
				break;
			}
		}

		for (int i=0 ; i<NUM_BANDS+1 ; i++) {
			if (bwd <= Bandwidths[(i*2)+1]) {
				bandwidth.Bandwidth.Down = i;
				break;
			}
		}
	}

	return(bandwidth);
}

/***********************************************************************************************
 * BandwidthCheckerClass::Get_Compact_Log -- Get basic log information to send to server       *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    String to add info to                                                             *
 *                                                                                             *
 * OUTPUT:   Nothing                                                                           *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   12/4/2001 1:12PM ST : Created                                                             *
 *=============================================================================================*/
void BandwidthCheckerClass::Get_Compact_Log(StringClass &log_string)
{
	char temp[128];
	sprintf(temp, "%d\t%d\t%d\t", UpstreamBandwidth, DownstreamBandwidth, cUserOptions::Get_Bandwidth_Type());
	log_string = temp;
}
