#pragma once

#ifndef NATADDR_H
#define NATADDR_H

#include "always.h"

#ifdef WWASSERT
#ifndef fw_assert
#define fw_assert WWASSERT
#endif //fw_assert
#else //WWASSERT
#define fw_assert assert
#endif //WWASSERT

/*
** Class to manage IP addresses for the NAT stuff only.
*/
class IPAddressClass
{
	/*
	** Public Interface
	*/
	public:
		/*
		**	Constructors:
		*/
		IPAddressClass(void);
		IPAddressClass(unsigned char *address, unsigned short port = 0);
		IPAddressClass(unsigned long address, unsigned short port = 0);

		/*
		**	Set the address from explicit variables.
		*/
		void Set_Address(unsigned char *address, unsigned short port = 0);
		void Set_Address(unsigned long address, unsigned short port = 0);
		void Set_Port(unsigned short port) {Port = port;};

		/*
		**	Get the address values explicitly.
		*/
		void Get_Address(unsigned char *address, unsigned short *port = 0);
		unsigned long Get_Address(void);
		unsigned short Get_Port(void);

		/*
		**	Tells if this address is a broadcast address
		*/
		bool Is_Broadcast(void);

		/*
		** Is this address valid?
		*/
		bool Is_Valid(void) {return(IsValid);};

		/*
		** Convert address to human readable string
		*/
		char *As_String(void);

		/*
		** Compare IPs only, not ports.
		*/
		bool Is_IP_Equal(IPAddressClass &address);

		/*
		**	Overloaded operators:
		*/
		bool operator =  (const IPAddressClass &address);
		bool operator == (IPAddressClass &address);
		bool operator != (IPAddressClass &address);
		bool operator == (IPAddressClass address);
		bool operator != (IPAddressClass address);



	/*
	**	Private Interface
	*/
	private:
		/*
		** Actual xxx.xxx.xxx.xxx IP address.
		*/
		union  {
			unsigned long WholeAddress;
			unsigned char Address[4];
		};

		/*
		** Port number associated with this address.
		*/
		unsigned short Port;

		/*
		** Are the contents of the address array valid?
		*/
		bool IsValid;
};



#endif