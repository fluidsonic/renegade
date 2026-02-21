//
// Filename:     nicenum.cpp
// Author:       Tom Spencer-Smith
// Date:         Dec 1999
// Description:  
//

#include "nicenum.h"

#include <winsock.h>
#include <stdio.h>

#include "netutil.h"
#include "useroptions.h"

//
// Class statics
//
ULONG		cNicEnum::NicList[];
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
	ULONG local_addresses[MAX_NICS];
	ULONG num_addresses = Enumerate_Nics(local_addresses, MAX_NICS);


	USHORT index	= 0;
	USHORT class_1	= 0;
	USHORT class_2	= 0;

	//
	// First, extract the non-internet addressable nicks, ordered on general usage.
	//
	ULONG lan_addresses[MAX_NICS];
	ULONG num_lan_addresses = 0;

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

	ULONG internet_addresses[MAX_NICS];
	ULONG num_internet_addresses = 0;

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
		if ((ULONG) cUserOptions::PreferredLanNic.Get() == NicList[index])
		{
			is_nic_valid = true;
			break;
		}
	}

	if (!is_nic_valid)
	{
		if (NumNics > 0) 
		{
			cUserOptions::PreferredLanNic.Set(NicList[0]);
		}
		else
		{
			cUserOptions::PreferredLanNic.Set(0);
		}
	}

	int cleanup_rc = ::WSACleanup();
}

//---------------------------------------------------------------------------
ULONG 
cNicEnum::Enumerate_Nics
(
	ULONG *	addresses, 
	ULONG		max_nics
)
{


	//
	// Get the local hostname
	//
	char local_host_name[300];
	int gethostname_rc = ::gethostname(local_host_name, sizeof(local_host_name));

	//
	// Resolve hostname for local adapter addresses. 
	// This does a DNS lookup (name resolution)
	//
	LPHOSTENT p_hostent = ::gethostbyname(local_host_name);
	if (p_hostent == NULL) 
	{
	}

	ULONG num_addresses = 0;
	while (num_addresses < max_nics && p_hostent->h_addr_list[num_addresses] != NULL) 
	{
		IN_ADDR in_addr;
		::memcpy(&in_addr, p_hostent->h_addr_list[num_addresses], sizeof(in_addr));
		addresses[num_addresses] = in_addr.s_addr;
		num_addresses++;
	}

	return num_addresses;
}































