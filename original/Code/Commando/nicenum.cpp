//
// Filename:     nicenum.cpp
// Author:       Tom Spencer-Smith
// Date:         Dec 1999
// Description:
//

#include "global.h"
#include "nicenum.h"

#include <winsock.h>
#include <ifaddrs.h>
#include <net/if.h>

#include "netutil.h"
#include "useroptions.h"

//
// Class statics
//
uint32_t		cNicEnum::NicList[];
USHORT	cNicEnum::NumNics			= 0;

//----------------------------------------------------------------------------------
void
cNicEnum::Init
(
	void
)
{

	WSADATA wsa_data;
	int startup_rc = ::WSAStartup(MAKEWORD(1, 1), &wsa_data);
	if (startup_rc != 0)
	{
		return;
	}

	//
	// Retrieve list of nic's
	//
	uint32_t local_addresses[MAX_NICS];
	uint32_t num_addresses = Enumerate_Nics(local_addresses, MAX_NICS);

	USHORT index	= 0;
	USHORT class_1	= 0;
	USHORT class_2	= 0;

	//
	// First, extract the non-internet addressable nicks, ordered on general usage.
	//
	uint32_t lan_addresses[MAX_NICS];
	uint32_t num_lan_addresses = 0;

	//
	// First, scan for 10.*.*.* addresses
	//
	for (index = 0; index < num_addresses; index++)
	{
		class_1 = (::ntohl(local_addresses[index]) & 0xff000000) >> 24;

		if (class_1 == 10)
		{
			lan_addresses[num_lan_addresses++] = local_addresses[index];
			local_addresses[index] = 0;
		}
	}

	//
	// Next, scan for 192.168.*.* addresses
	//
	for (index = 0; index < num_addresses; index++)
	{
		class_1 = (::ntohl(local_addresses[index]) & 0xff000000) >> 24;
		class_2 = (::ntohl(local_addresses[index]) & 0x00ff0000) >> 16;

		if (class_1 == 192 && class_2 == 168)
		{
			lan_addresses[num_lan_addresses++] = local_addresses[index];
			local_addresses[index] = 0;
		}
	}

	//
	// Next, scan for 172.16-31.*.* addresses
	//
	for (index = 0; index < num_addresses; index++)
	{
		class_1 = (::ntohl(local_addresses[index]) & 0xff000000) >> 24;
		class_2 = (::ntohl(local_addresses[index]) & 0x00ff0000) >> 16;

		if (class_1 == 172 && class_2 >= 16 && class_2 <= 31)
		{
			lan_addresses[num_lan_addresses++] = local_addresses[index];
			local_addresses[index] = 0;
		}
	}

	//
	// Finally, scan for 169.254.*.* addresses (IP autoconfiguration)
	//
	for (index = 0; index < num_addresses; index++)
	{
		class_1 = (::ntohl(local_addresses[index]) & 0xff000000) >> 24;
		class_2 = (::ntohl(local_addresses[index]) & 0x00ff0000) >> 16;

		if (class_1 == 169 && class_2 == 254)
		{
			lan_addresses[num_lan_addresses++] = local_addresses[index];
			local_addresses[index] = 0;
		}
	}

	//
	// Next, copy the Internet addressable addresses. Weed out localhost and multicast
	// addresses.
	//

	uint32_t internet_addresses[MAX_NICS];
	uint32_t num_internet_addresses = 0;

	for (index = 0; index < num_addresses; index++)
	{
		if (local_addresses[index] != 0)
		{
			class_1 = (::ntohl(local_addresses[index]) & 0xff000000) >> 24;

			if (class_1 != 127 && class_1 != 224)
			{
				internet_addresses[num_internet_addresses++] = local_addresses[index];
				local_addresses[index] = 0;
			}
		}
	}

	NumNics			= 0;

	for (index = 0; index < num_lan_addresses; index++)
	{
		NicList[NumNics++] = lan_addresses[index];
	}

	for (index = 0; index < num_internet_addresses; index++)
	{
		NicList[NumNics++] = internet_addresses[index];
	}

	//
	// Initialize or update PreferredLanNic if required.
	//
	bool is_nic_valid = false;
	for (index = 0; index < NumNics; index++)
	{
		if ((uint32_t) cUserOptions::PreferredLanNic.Get() == NicList[index])
		{
			is_nic_valid = true;
			break;
		}
	}

	if (!is_nic_valid)
	{
		if (NumNics > 0)
		{
			cUserOptions::PreferredLanNic.Set(static_cast<int32_t>(NicList[0]));
		}
		else
		{
			cUserOptions::PreferredLanNic.Set(0);
		}
	}

	int cleanup_rc = ::WSACleanup();
}

//---------------------------------------------------------------------------
uint32_t
cNicEnum::Enumerate_Nics
(
	uint32_t *	addresses,
	uint32_t		max_nics
)
{
	struct ifaddrs *ifap = nullptr, *ifa = nullptr;

	if (::getifaddrs(&ifap) != 0)
	{
		return 0;
	}

	uint32_t num_addresses = 0;
	for (ifa = ifap; ifa != nullptr && num_addresses < max_nics; ifa = ifa->ifa_next)
	{
		if (ifa->ifa_addr == nullptr)
			continue;

		if (!(ifa->ifa_flags & IFF_UP) || !(ifa->ifa_flags & IFF_RUNNING))
			continue;

		if (ifa->ifa_flags & IFF_LOOPBACK)
			continue;

		if (ifa->ifa_addr->sa_family != AF_INET)
			continue;

		struct sockaddr_in *sin = reinterpret_cast<struct sockaddr_in *>(ifa->ifa_addr);
		addresses[num_addresses] = sin->sin_addr.s_addr;
		num_addresses++;
	}

	::freeifaddrs(ifap);
	return num_addresses;
}
