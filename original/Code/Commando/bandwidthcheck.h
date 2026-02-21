#ifndef _BANDWIDTHCHECK_H
#define _BANDWIDTHCHECK_H

#include <wwlib/except.h>
#include <wwlib/wwstring.h>
#include <wwlib/thread.h>
#include <windows.h>
#include <BandTest/BandTest.h>


class BandwidthCheckerClass
{

	public:

		/*
		** Struct for packing bandwidth levels into a single byte.
		*/
		#pragma pack(push)
		#pragma pack(1)
		typedef struct tInternalPackedBandwidthType {
			unsigned char Up		: 4;
			unsigned char Down	: 4;
		} InternalPackedBandwidthType;

		typedef union tPackedBandwidthType {
			InternalPackedBandwidthType Bandwidth;
			unsigned char RawBandwidth;
		} PackedBandwidthType;
		#pragma pack(pop)

		static void Check_Now(HANDLE event);

		static bool Got_Bandwidth(void) {return(GotBandwidth);};
		static void Force_Upstream_Bandwidth(unsigned int up);
		static unsigned long Get_Upstream_Bandwidth(void);
		static unsigned long Get_Reported_Upstream_Bandwidth(void);
		static WCHAR *Get_Upstream_Bandwidth_As_String(void);
		static unsigned long Get_Downstream_Bandwidth(void);
		static unsigned long Get_Reported_Downstream_Bandwidth(void);
		static WCHAR *Get_Downstream_Bandwidth_As_String(void);
		static WCHAR *Get_Bandwidth_As_String(void);
		static WCHAR *Get_Bandwidth_As_String(PackedBandwidthType bandwidth);
		static PackedBandwidthType Get_Packed_Bandwidth(void);
		static bool Failed_Due_To_No_Connection(void) {return(FailureCode == BANDTEST_NO_IP_DETECT);}
		static void Get_Compact_Log(StringClass &log_string);
		static bool Is_Thread_Running(void) { return Thread.Is_Running(); }

	private:

		static void Check(void);
		static const char *Get_Ping_Server_Name(void);

		static class BandwidthCheckerThreadClass : public ThreadClass {
			public:
				BandwidthCheckerThreadClass(const char *thread_name = "Bandwidth checker thread") : ThreadClass(thread_name, &Exception_Handler) {}
				void Thread_Function(void) {BandwidthCheckerClass::Check();};
		} Thread;
		friend BandwidthCheckerThreadClass;

		static HANDLE EventNotify;
		static unsigned long UpstreamBandwidth;
		static unsigned long ReportedUpstreamBandwidth;
		static unsigned long DownstreamBandwidth;
		static unsigned long ReportedDownstreamBandwidth;
		static WCHAR *UpstreamBandwidthString;
		static WCHAR *DownstreamBandwidthString;

		#define NUM_BANDS 12

		static char *ErrorList[13];
		static unsigned long Bandwidths[NUM_BANDS * 2];
		static WCHAR *BandwidthNames[NUM_BANDS + 1];
		static int FailureCode;
		static bool GotBandwidth;
		static const char *DefaultServerName;

};




#endif //_BANDWIDTHCHECK_H
