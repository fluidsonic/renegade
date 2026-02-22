//
// Filename:     netutil.cpp
// Project:      wwnet
// Author:       Tom Spencer-Smith
// Date:         June 1998
// Description:
//
//-----------------------------------------------------------------------------
#include "netutil.h" // I WANNA BE FIRST!

#include <stdio.h>
#include <string.h>

#include "miscutil.h"
#include "mathutil.h"
#include "singlepl.h"
#include "wwpacket.h"
#include "ffactory.h"
#include "ini.h"
#include "systimer.h"
#include "fromaddress.h"

//
// class statics
//
//const WORD cNetUtil::WS_VERSION_REQD = MAKEWORD(1, 1); // Winsock 1.1
//USHORT cNetUtil::HeaderBytes;
//USHORT cNetUtil::MaxPacketAppDataSize = MAX_LAN_PACKET_APP_DATA_SIZE;
UINT cNetUtil::DefaultResendTimeoutMs = 200; // used for singleplayer
bool cNetUtil::IsInternet = false;

static const int INVALID_VALUE = -999;

const USHORT	cNetUtil::NETSTATS_SAMPLE_TIME_MS									= 2000;
const USHORT	cNetUtil::KEEPALIVE_TIMEOUT_MS										= 2000;
const USHORT	cNetUtil::MAX_RESENDS													= 50;
const USHORT	cNetUtil::MULTI_SENDS													= 10;
const USHORT	cNetUtil::RESEND_TIMEOUT_LAN_MS										= 300;
const USHORT	cNetUtil::RESEND_TIMEOUT_INTERNET_MS								= 500;
const	uint32_t		cNetUtil::CLIENT_CONNECTION_LOSS_TIMEOUT							= 15000;		// Milliseconds til client gives up on server
const	uint32_t		cNetUtil::SERVER_CONNECTION_LOSS_TIMEOUT							= 15000;		// Milliseconds til server gives up on client
const	uint32_t		cNetUtil::SERVER_CONNECTION_LOSS_TIMEOUT_LOADING_ALLOWANCE	= 45000;		// Milliseconds extra allowed til server gives up on loading client.

//int cNetUtil::DefaultMultiSends							= INVALID_VALUE;
//int cNetUtil::DefaultMaxResends							= INVALID_VALUE;
//int cNetUtil::DefaultKeepaliveTimeoutMs				= INVALID_VALUE;
//int cNetUtil::DesiredSendBufferSizeBytes				= INVALID_VALUE;
//int cNetUtil::DesiredReceiveBufferSizeBytes			= INVALID_VALUE;
//int cNetUtil::DefaultServerPort							= INVALID_VALUE;
//int cNetUtil::MaxReceiveTimeMs							= INVALID_VALUE;

//float cNetUtil::PriorityToleranceDownwards			= INVALID_VALUE;
//float cNetUtil::PriorityToleranceUpwards				= INVALID_VALUE;
//float cNetUtil::MaxTPCorrectionDownwards				= INVALID_VALUE;
//float cNetUtil::MaxTPCorrectionUpwards					= INVALID_VALUE;
//float cNetUtil::PriorityNoiseFactor						= INVALID_VALUE;
//float cNetUtil::InitialThresholdPriority				= INVALID_VALUE;
//float cNetUtil::PriorityGrowthPerSecond				= INVALID_VALUE;

char cNetUtil::WorkingAddressBuffer[]					= "";

//-----------------------------------------------------------------------------
//
// Macro
//
#define ADD_CASE(exp)    case exp: ::sprintf(error_msg, #exp);  break;

void cNetUtil::Wsa_Error(LPCSTR sFile, unsigned uLine)
{
}

//-----------------------------------------------------------------------------
//
// Just get the text for the specified error code.
//
const char * cNetUtil::Winsock_Error_Text(int error_code)
{
   static char error_msg[500];

   switch (error_code) {

      //
      // Windows Sockets definitions of regular Microsoft C error constants
      //
      ADD_CASE(WSAEINTR)
      ADD_CASE(WSAEBADF)
      ADD_CASE(WSAEACCES)
      ADD_CASE(WSAEFAULT)
      ADD_CASE(WSAEINVAL)
      ADD_CASE(WSAEMFILE)

      //
      // Windows Sockets definitions of regular Berkeley error constants
      //
      ADD_CASE(WSAEWOULDBLOCK)
      ADD_CASE(WSAEINPROGRESS)
      ADD_CASE(WSAEALREADY)
      ADD_CASE(WSAENOTSOCK)
      ADD_CASE(WSAEDESTADDRREQ)
      ADD_CASE(WSAEMSGSIZE)
      ADD_CASE(WSAEPROTOTYPE)
      ADD_CASE(WSAENOPROTOOPT)
      ADD_CASE(WSAEPROTONOSUPPORT)
      ADD_CASE(WSAESOCKTNOSUPPORT)
      ADD_CASE(WSAEOPNOTSUPP)
      ADD_CASE(WSAEPFNOSUPPORT)
      ADD_CASE(WSAEAFNOSUPPORT)
      ADD_CASE(WSAEADDRINUSE)
      ADD_CASE(WSAEADDRNOTAVAIL)
      ADD_CASE(WSAENETDOWN)
      ADD_CASE(WSAENETUNREACH)
      ADD_CASE(WSAENETRESET)
      ADD_CASE(WSAECONNABORTED)
      ADD_CASE(WSAECONNRESET)
      ADD_CASE(WSAENOBUFS)
      ADD_CASE(WSAEISCONN)
      ADD_CASE(WSAENOTCONN)
      ADD_CASE(WSAESHUTDOWN)
      ADD_CASE(WSAETOOMANYREFS)
      ADD_CASE(WSAETIMEDOUT)
      ADD_CASE(WSAECONNREFUSED)
      ADD_CASE(WSAELOOP)
      ADD_CASE(WSAENAMETOOLONG)
      ADD_CASE(WSAEHOSTDOWN)
      ADD_CASE(WSAEHOSTUNREACH)
      ADD_CASE(WSAENOTEMPTY)
      ADD_CASE(WSAEPROCLIM)
      ADD_CASE(WSAEUSERS)
      ADD_CASE(WSAEDQUOT)
      ADD_CASE(WSAESTALE)
      ADD_CASE(WSAEREMOTE)

      //
      // Extended Windows Sockets error constant definitions
      ///
      ADD_CASE(WSASYSNOTREADY)
      ADD_CASE(WSAVERNOTSUPPORTED)
      ADD_CASE(WSANOTINITIALISED)
		ADD_CASE(WSAEDISCON)

		default:
         ::sprintf(error_msg, "Unknown Winsock Error (%d)", error_code);
         break;
   }

	return(error_msg);
}

/*
int g_c_wouldblock = 0;
int g_c_nobufs = 0;
*/
//-----------------------------------------------------------------------------
bool cNetUtil::Send_Resource_Failure(LPCSTR sFile, unsigned uLine, int ret_code)
{
	bool return_code = false;

   if (ret_code == SOCKET_ERROR) {
		int wsa_error = ::WSAGetLastError();
      if (wsa_error == WSAEWOULDBLOCK || wsa_error == WSAENOBUFS) {

			/*
			if (wsa_error == WSAEWOULDBLOCK) {
				g_c_wouldblock++;
			} else {
				g_c_nobufs++;
			}
			*/

         return_code = true;
      } else {
         Wsa_Error(sFile, uLine);
      }
   } else {
		return_code = false;
	}

   return return_code;
}

//-----------------------------------------------------------------------------
bool cNetUtil::Would_Block(LPCSTR sFile, unsigned uLine, int ret_code)
{
	bool retcode = false;

   if (ret_code == SOCKET_ERROR) {
      if (::WSAGetLastError() == WSAEWOULDBLOCK) {
         retcode = true;
      } else {
         Wsa_Error(sFile, uLine);
         retcode = false;
      }
   } else {
      retcode = false;
	}

   return retcode;
}

//-----------------------------------------------------------------------------
//
// Returns up to max_addresses adapter addresses for the local host
//
int cNetUtil::Get_Local_Tcpip_Addresses(SOCKADDR_IN ip_address[], USHORT max_addresses)
{

	//
	// Get the local hostname
	//
	char local_host_name[200];
	WSA_CHECK(::gethostname(local_host_name, sizeof(local_host_name)));

	//
	// Resolve hostname for local adapter addresses. This does
   // a DNS lookup (name resolution)
	//
	LPHOSTENT p_hostent = ::gethostbyname(local_host_name);

   int num_adapters = 0;

	if (p_hostent == NULL) {
		num_adapters = 0;
	} else {
		while (num_adapters < max_addresses && p_hostent->h_addr_list[num_adapters] != NULL) {

			ZeroMemory(&ip_address[num_adapters], sizeof(SOCKADDR_IN));
			ip_address[num_adapters].sin_family = AF_INET;
	      ip_address[num_adapters].sin_addr.s_addr =
				*((u_long *) (p_hostent->h_addr_list[num_adapters]));
			num_adapters++;
		}
	}

	return num_adapters;
}

//-----------------------------------------------------------------------------
bool cNetUtil::Is_Same_Address(LPSOCKADDR_IN p_address1, const SOCKADDR_IN* p_address2)
{
	//
	// C disallows comparison of structs...
	//

   return
      p_address1->sin_addr.s_addr	== p_address2->sin_addr.s_addr &&
      p_address1->sin_port				== p_address2->sin_port;
}

//-------------------------------------------------------------------------------
void cNetUtil::Address_To_String(LPSOCKADDR_IN p_address, char * str, UINT len,
   USHORT & port)
{

	char temp_str[1000];

	::strcpy(temp_str, ::inet_ntoa(p_address->sin_addr));
   port = ::ntohs(p_address->sin_port);

	::strcpy(str, temp_str);
}

//-------------------------------------------------------------------------------
LPCSTR cNetUtil::Address_To_String(uint32_t ip)
{
	IN_ADDR in_addr;
	in_addr.s_addr = ip;
	char * p = ::inet_ntoa(in_addr);
	if (p == NULL) {
		::sprintf(WorkingAddressBuffer, "Invalid ip (%u)", ip);
	} else {
		::strcpy(WorkingAddressBuffer, p);
	}

	return WorkingAddressBuffer;
}

//-------------------------------------------------------------------------------
void cNetUtil::String_To_Address(LPSOCKADDR_IN p_address, LPCSTR str, USHORT port)
{
   ZeroMemory(p_address, sizeof(SOCKADDR_IN));

   p_address->sin_family			= AF_INET;
   p_address->sin_addr.s_addr		= ::inet_addr(str);
   p_address->sin_port				= ::htons(port);

}

//-------------------------------------------------------------------------------
bool cNetUtil::Is_Tcpip_Present(void)
{
   //
   // N.B. I tested EnumProtocols and found it unreliable.
   //

	bool retcode = true;

   SOCKET test_socket = ::socket(AF_INET, SOCK_DGRAM, 0);
   if (test_socket == INVALID_SOCKET) {
      if (::WSAGetLastError() == WSAEAFNOSUPPORT) {
         retcode = false;
      } else {
         WSA_ERROR;
      }
   } else {
	   WSA_CHECK(::closesocket(test_socket));
	}

   return retcode;
}

//-------------------------------------------------------------------------------
void cNetUtil::Wsa_Init()
{
	//
	// winsock 1.1
	//

   WSADATA wsa_data;
   if (::WSAStartup(MAKEWORD(1, 1), &wsa_data) != 0) {
   }
}

//-------------------------------------------------------------------------------
bool cNetUtil::Protocol_Init(bool is_internet)
{
	IsInternet = is_internet;

   bool retcode = false;

	if (Is_Tcpip_Present()) {
      retcode = true;

		if (IsInternet) {
			DefaultResendTimeoutMs = RESEND_TIMEOUT_INTERNET_MS;
		} else {
			DefaultResendTimeoutMs = RESEND_TIMEOUT_LAN_MS;
		}
   } else {
      retcode = false;
	}

   return retcode;
}

/*
//-------------------------------------------------------------------------------
float cNetUtil::Compute_Priority_Noise()
{
	//
	// Add some noise to increase the spread. This noise must be bigger
	// than the maximum threshold adjustment, so PriorityNoiseFactor should
	// be at least 1 if you are adding noise.
	// Do not jitter a priority of zero!
	//
	// Or maybe we can just specify a max noise now?
	//
	float noise_width = PriorityNoiseFactor * MaxTPCorrectionDownwards;
	return cMathUtil::Get_Hat_Pdf_Double(-noise_width / 2.0f, +noise_width / 2.0f);
}
*/

//-------------------------------------------------------------------------------
void cNetUtil::Set_Socket_Buffer_Sizes(SOCKET sock, int new_size)
{

   int buffersize		= 0;
   int len				= 0;

	buffersize = 0;
	len = sizeof(int);
   WSA_CHECK(::getsockopt(sock, SOL_SOCKET, SO_SNDBUF, (char *)&buffersize, &len));
   //

   buffersize = 0;
   len = sizeof(int);
   WSA_CHECK(::getsockopt(sock, SOL_SOCKET, SO_RCVBUF, (char *)&buffersize, &len));
   //

   buffersize = new_size;
   len = sizeof(int);
   WSA_CHECK(setsockopt(sock, SOL_SOCKET, SO_SNDBUF, (char *)&buffersize, len));
   //

   buffersize = new_size;
   len = sizeof(int);
   WSA_CHECK(setsockopt(sock, SOL_SOCKET, SO_RCVBUF, (char *)&buffersize, len));
   //

	buffersize = 0;
	len = sizeof(int);
   WSA_CHECK(::getsockopt(sock, SOL_SOCKET, SO_SNDBUF, (char *)&buffersize, &len));
   //

	buffersize = 0;
	len = sizeof(int);
   WSA_CHECK(::getsockopt(sock, SOL_SOCKET, SO_RCVBUF, (char *)&buffersize, &len));
   //
}

/*
//-------------------------------------------------------------------------------
void cNetUtil::Onetime_Init()
{
	FileClass * p_ini_file = _TheFileFactory->Get_File("netparams.ini");

	if (p_ini_file != NULL) {

		INIClass netparams_ini(*p_ini_file);

		const LPCSTR SECTION_NAME = "Settings";

		//NETSTATS_SAMPLE_TIME_MS					= netparams_ini.Get_Int(SECTION_NAME, "NETSTATS_SAMPLE_TIME_MS",				INVALID_VALUE);
		//assert(NETSTATS_SAMPLE_TIME_MS > 0);

		//RESEND_TIMEOUT_LAN_MS			= netparams_ini.Get_Int(SECTION_NAME, "RESEND_TIMEOUT_LAN_MS",		INVALID_VALUE);
		//assert(RESEND_TIMEOUT_LAN_MS > 0);

		//RESEND_TIMEOUT_INTERNET_MS	= netparams_ini.Get_Int(SECTION_NAME, "RESEND_TIMEOUT_INTERNET_MS", INVALID_VALUE);
		//assert(RESEND_TIMEOUT_INTERNET_MS > 0);

		//DefaultMultiSends						= netparams_ini.Get_Int(SECTION_NAME, "DefaultMultiSends",					INVALID_VALUE);
		//assert(DefaultMultiSends > 0);

		//DefaultMaxResends						= netparams_ini.Get_Int(SECTION_NAME, "DefaultMaxResends",					INVALID_VALUE);
		//assert(DefaultMaxResends > 0);

		//DefaultKeepaliveTimeoutMs			= netparams_ini.Get_Int(SECTION_NAME, "DefaultKeepaliveTimeoutMs",		INVALID_VALUE);
		//assert(DefaultKeepaliveTimeoutMs > 0);

		//DesiredSendBufferSizeBytes			= netparams_ini.Get_Int(SECTION_NAME, "DesiredSendBufferSizeBytes",		INVALID_VALUE);
		//assert(DesiredSendBufferSizeBytes > 0);

		//DESIRED_RECEIVE_BUFFER_SIZE_BYTES		= netparams_ini.Get_Int(SECTION_NAME, "DesiredReceiveBufferSizeBytes",	INVALID_VALUE);
		//assert(DesiredReceiveBufferSizeBytes > 0);

		//DefaultServerPort						= netparams_ini.Get_Int(SECTION_NAME, "DefaultServerPort",					INVALID_VALUE);
		//assert(DefaultServerPort >= MIN_SERVER_PORT && DefaultServerPort <= MAX_SERVER_PORT);

		//MaxReceiveTimeMs						= netparams_ini.Get_Int(SECTION_NAME, "MaxReceiveTimeMs",					INVALID_VALUE);
		//assert(MaxReceiveTimeMs > 0);

		//PriorityToleranceDownwards			= netparams_ini.Get_Float(SECTION_NAME, "PriorityToleranceDownwards",			INVALID_VALUE);
		//assert(PriorityToleranceDownwards > -1 - MISCUTIL_EPSILON && PriorityToleranceDownwards < 1 + MISCUTIL_EPSILON);

		//PriorityToleranceUpwards			= netparams_ini.Get_Float(SECTION_NAME, "PriorityToleranceUpwards",			INVALID_VALUE);
		//assert(PriorityToleranceUpwards > -1 - MISCUTIL_EPSILON && PriorityToleranceUpwards < 1 + MISCUTIL_EPSILON);

		//MaxTPCorrectionDownwards			= netparams_ini.Get_Float(SECTION_NAME, "MaxTPCorrectionDownwards",	INVALID_VALUE);
		//assert(MaxTPCorrectionDownwards > -1 - MISCUTIL_EPSILON && MaxTPCorrectionDownwards < 1 + MISCUTIL_EPSILON);

		//MaxTPCorrectionUpwards				= netparams_ini.Get_Float(SECTION_NAME, "MaxTPCorrectionUpwards",	INVALID_VALUE);
		//assert(MaxTPCorrectionUpwards > -1 - MISCUTIL_EPSILON && MaxTPCorrectionUpwards < 1 + MISCUTIL_EPSILON);

		//PriorityNoiseFactor					= netparams_ini.Get_Float(SECTION_NAME, "PriorityNoiseFactor",				INVALID_VALUE);
		//assert(PriorityNoiseFactor >= -MISCUTIL_EPSILON);

		//InitialThresholdPriority			= netparams_ini.Get_Float(SECTION_NAME, "InitialThresholdPriority",		INVALID_VALUE);
		//assert(InitialThresholdPriority >= -MISCUTIL_EPSILON);

		//PriorityNoiseFactor					= netparams_ini.Get_Float(SECTION_NAME, "PriorityNoiseFactor",				INVALID_VALUE);
		//assert(PriorityNoiseFactor >= -MISCUTIL_EPSILON);

		//PriorityGrowthPerSecond				= netparams_ini.Get_Float(SECTION_NAME, "PriorityGrowthPerSecond",		INVALID_VALUE);
		//assert(PriorityGrowthPerSecond >= -MISCUTIL_EPSILON);

		_TheFileFactory->Return_File(p_ini_file);
	}
}
*/

void cNetUtil::Create_Unbound_Socket(SOCKET & sock)
{
   sock = ::socket(AF_INET, SOCK_DGRAM, 0);
   if (sock == INVALID_SOCKET) {
      WSA_ERROR;
   }

   //
   // Enable broadcasts
   //
   int optval = TRUE;
   WSA_CHECK(setsockopt(sock, SOL_SOCKET, SO_BROADCAST, (char *) &optval, sizeof(optval)));

   //
   // Make socket non-blocking
   //
   u_long arg = 1L;
   WSA_CHECK(ioctlsocket(sock, FIONBIO, (u_long *) &arg));
}

//-------------------------------------------------------------------------------
bool cNetUtil::Create_Bound_Socket(SOCKET & sock, USHORT port, SOCKADDR_IN & local_address)
{
   //
   // TSS - is all this necessary or is above function OK?
   //

   Create_Unbound_Socket(sock);

   Create_Local_Address(&local_address, port);
	int result = ::bind(sock, (LPSOCKADDR) &local_address, sizeof(SOCKADDR_IN));
	if (result == 0) {
		return true;
   } else {
      //if (::WSAGetLastError() != WSAEADDRINUSE) {
         WSA_ERROR;
      //}
      return false;
   }
}

//-------------------------------------------------------------------------------
void cNetUtil::Close_Socket(SOCKET & sock)
{
   ::closesocket(sock);
}

//-----------------------------------------------------------------------------
void cNetUtil::Broadcast(SOCKET & sock, USHORT port, cPacket & packet)
{
   SOCKADDR_IN broadcast_address;
   Create_Broadcast_Address(&broadcast_address, port);
   int bytes_sent;
	//WSA_CHECK(bytes_sent = sendto(sock, packet.Data, packet.SendLength,
   //   0, &broadcast_address, sizeof(SOCKADDR_IN)));
	bytes_sent = sendto(sock, packet.Get_Data(), packet.Get_Compressed_Size_Bytes(),
      0, (LPSOCKADDR) &broadcast_address, sizeof(SOCKADDR_IN));
   //
}

//-------------------------------------------------------------------------------
void cNetUtil::Create_Broadcast_Address(LPSOCKADDR_IN p_broadcast_address,
   USHORT port)
{
   ZeroMemory(p_broadcast_address, sizeof(SOCKADDR_IN));

	p_broadcast_address->sin_family			= AF_INET;
	p_broadcast_address->sin_addr.s_addr	= INADDR_BROADCAST; // ::inet_addr("255.255.255.255");
	p_broadcast_address->sin_port				= ::htons(port);
}

//-------------------------------------------------------------------------------
void cNetUtil::Create_Local_Address(LPSOCKADDR_IN p_local_address, USHORT port)
{
   ZeroMemory(p_local_address, sizeof(SOCKADDR_IN));

	p_local_address->sin_family			= AF_INET;
	p_local_address->sin_addr.s_addr		= INADDR_ANY;
	p_local_address->sin_port				= ::htons(port);
}

//-------------------------------------------------------------------------------
bool cNetUtil::Get_Local_Address(LPSOCKADDR_IN p_local_address)
{

	/*
	const USHORT MAX_ADDRESSES = 1;
	int num_addresses = Get_Local_Tcpip_Addresses(p_local_address, MAX_ADDRESSES);

	return (num_addresses == 1);
	*/

	const USHORT MAX_ADDRESSES = 10;
	SOCKADDR_IN local_address[MAX_ADDRESSES];
	int num_addresses = Get_Local_Tcpip_Addresses(local_address, MAX_ADDRESSES);

	if (num_addresses > 0) {
		::memcpy(p_local_address, &local_address[0], sizeof(SOCKADDR_IN));
	}

	return (num_addresses > 0);
}

//-----------------------------------------------------------------------------
void cNetUtil::Lan_Servicing(SOCKET & sock, LanPacketHandlerCallback p_callback)
{
   int retcode;

   unsigned long start_time = TIMEGETTIME();

   do {
		cPacket packet;
		int address_len = sizeof(SOCKADDR_IN);

		//
		// If we appear to crash INSIDE recvfrom then this tends to indicate
		// that net neighbourhood broke.
		//
		retcode = recvfrom(sock, packet.Get_Data(), packet.Get_Max_Size(),
			0, (LPSOCKADDR) &packet.Get_From_Address_Wrapper()->FromAddress, &address_len);

		if (retcode == SOCKET_ERROR) {
			if (::WSAGetLastError() != WSAEWOULDBLOCK) {
				WSA_ERROR;
			}
		} else {
			/*
			//
			// diagnostic
			//
			uint32_t ip = packet.Get_From_Address_Wrapper()->FromAddress.sin_addr.s_addr;
			*/

			//packet.Set_Received_Length(retcode);
			packet.Set_Bit_Length(retcode * 8);
			(*p_callback)(packet);
		}
	} while (retcode != SOCKET_ERROR); // this will indicate no more data

   unsigned long time_spent = TIMEGETTIME() - start_time;
   if (time_spent > 100) {
   }
}
