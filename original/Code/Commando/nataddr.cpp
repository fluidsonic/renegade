#include	"always.h"
#include	"assert.h"
#include	"nataddr.h"

#include	<stdio.h>
#include	<string.h>



/***********************************************************************************************
 * IPAddressClass::IPAddressClass -- Class constructor                                         *
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
 *   3/9/00 12:58PM ST : Created                                                               *
 *=============================================================================================*/
IPAddressClass::IPAddressClass(void)
{
	WholeAddress = 0x00000000;
	Port = 0;
	IsValid = false;
}



/***********************************************************************************************
 * IPAddressClass::IPAddressClass -- Class constructor                                         *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    Ptr to valid IP address                                                           *
 *                                                                                             *
 * OUTPUT:   Nothing                                                                           *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   3/9/00 12:59PM ST : Created                                                               *
 *=============================================================================================*/
IPAddressClass::IPAddressClass(unsigned char *address, unsigned short port)
{
	WholeAddress = *((unsigned long*)address);
	Port = port;
	IsValid = true;
}


/***********************************************************************************************
 * IPAddressClass::IPAddressClass -- Class constructor                                         *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    32 bit IP address                                                                 *
 *                                                                                             *
 * OUTPUT:   Nothing                                                                           *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   3/9/00 1:00PM ST : Created                                                                *
 *=============================================================================================*/
IPAddressClass::IPAddressClass(unsigned long address, unsigned short port)
{
	WholeAddress = address;
	Port = port;
	IsValid = true;
}




/***********************************************************************************************
 * IPAddressClass::Set_Address -- Sets a valid IP address into the class                       *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    Ptr to IP address                                                                 *
 *                                                                                             *
 * OUTPUT:   Nothing                                                                           *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   3/9/00 1:01PM ST : Created                                                                *
 *=============================================================================================*/
void IPAddressClass::Set_Address(unsigned char *address, unsigned short port)
{
	WholeAddress = *((unsigned long*)address);
	Port = port;
	IsValid = true;
}



/***********************************************************************************************
 * IPAddressClass::Set_Address -- Sets a valid IP address into the class                       *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    32 bit IP address                                                                 *
 *                                                                                             *
 * OUTPUT:   Nothing                                                                           *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   3/9/00 1:01PM ST : Created                                                                *
 *=============================================================================================*/
void IPAddressClass::Set_Address(unsigned long address, unsigned short port)
{
	WholeAddress = address;
	Port = port;
	IsValid = true;
}



/***********************************************************************************************
 * IPAddressClass::Get_Address -- Get the address value of this class                          *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    Ptr to area to store the address into                                             *
 *                                                                                             *
 * OUTPUT:   Nothing                                                                           *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   3/9/00 1:03PM ST : Created                                                                *
 *=============================================================================================*/
void IPAddressClass::Get_Address(unsigned char *address, unsigned short *port)
{
	fw_assert(IsValid);
	*((unsigned long*)address) = WholeAddress;
	if (port) {
		*port = Port;
	}
}




/***********************************************************************************************
 * IPAddressClass::Get_Address -- Get the address value of this class                          *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    Nothing                                                                           *
 *                                                                                             *
 * OUTPUT:   32 bit IP address                                                                 *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   3/9/00 1:03PM ST : Created                                                                *
 *=============================================================================================*/
unsigned long IPAddressClass::Get_Address(void)
{
	fw_assert(IsValid);
	return (WholeAddress);
}



/***********************************************************************************************
 * IPAddressClass::Get_Port -- Get the port number associated with this address                *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    Nothing                                                                           *
 *                                                                                             *
 * OUTPUT:   Port number                                                                       *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   10/24/00 11:51AM ST : Created                                                             *
 *=============================================================================================*/
unsigned short IPAddressClass::Get_Port(void)
{
	fw_assert(IsValid);
	return(Port);
}



/***********************************************************************************************
 * IPAddressClass::Is_Broadcast -- Does this class represent a broadcast address?              *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    Nothing                                                                           *
 *                                                                                             *
 * OUTPUT:   True if broadcast address                                                         *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   3/9/00 1:06PM ST : Created                                                                *
 *=============================================================================================*/
bool IPAddressClass::Is_Broadcast(void)
{
	if (IsValid && WholeAddress == 0xffffffff) {
		return(true);
	}
	return(false);

}



/***********************************************************************************************
 * IPAddressClass::As_String -- Get address in human readable form                             *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    ptr to address                                                                    *
 *                                                                                             *
 * OUTPUT:   ptr to string                                                                     *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   3/9/00 1:07PM ST : Created                                                                *
 *=============================================================================================*/
char *IPAddressClass::As_String(void)
{
	static char _addr_str[128];
	sprintf (_addr_str, "%d.%d.%d.%d ; %d", Address[0], Address[1], Address[2], Address[3], (unsigned int)Port);
	return (_addr_str);
}



/***********************************************************************************************
 * IPAddressClass::Is_IP_Equal -- Compare the IP portion of the address only, ignore the port  *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    Address to compare to                                                             *
 *                                                                                             *
 * OUTPUT:   True if equal                                                                     *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   10/25/00 2:23PM ST : Created                                                              *
 *=============================================================================================*/
bool IPAddressClass::Is_IP_Equal(IPAddressClass &address)
{
	if (IsValid && address.Is_Valid() && address.Get_Address() == WholeAddress) {
		return(true);
	}
	return(false);

}



/***********************************************************************************************
 * IPAddressClass::operator == - Equality operator for the class                               *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    Class to compare to                                                               *
 *                                                                                             *
 * OUTPUT:   True if the same                                                                  *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   3/9/00 1:10PM ST : Created                                                                *
 *=============================================================================================*/
bool IPAddressClass::operator == (IPAddressClass &address)
{
	if (IsValid && address.Is_Valid() && address.Get_Address() == WholeAddress && address.Get_Port() == Port) {
		return(true);
	}
	return(false);
}


/***********************************************************************************************
 * IPAddressClass::operator != - Non-equality operator for the class                           *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    Class to compare to                                                               *
 *                                                                                             *
 * OUTPUT:   True if not the same                                                              *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   3/9/00 1:10PM ST : Created                                                                *
 *=============================================================================================*/
bool IPAddressClass::operator != (IPAddressClass &address)
{
	if (!IsValid || !address.Is_Valid() || address.Get_Address() != WholeAddress || address.Get_Port() != Port) {
		return(true);
	}
	return(false);
}

/***********************************************************************************************
 * IPAddressClass::operator == - Equality operator for the class                               *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    Class to compare to                                                               *
 *                                                                                             *
 * OUTPUT:   True if the same                                                                  *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   3/9/00 1:10PM ST : Created                                                                *
 *=============================================================================================*/
bool IPAddressClass::operator == (IPAddressClass address)
{
	if (IsValid && address.Is_Valid() && address.Get_Address() == WholeAddress && address.Get_Port() == Port) {
		return(true);
	}
	return(false);
}


/***********************************************************************************************
 * IPAddressClass::operator != - Non-equality operator for the class                           *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    Class to compare to                                                               *
 *                                                                                             *
 * OUTPUT:   True if not the same                                                              *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   3/9/00 1:10PM ST : Created                                                                *
 *=============================================================================================*/
bool IPAddressClass::operator != (IPAddressClass address)
{
	if (!IsValid || !address.Is_Valid() || address.Get_Address() != WholeAddress || address.Get_Port() != Port) {
		return(true);
	}
	return(false);
}


/***********************************************************************************************
 * IPAddressClass::operator = Assignment operator                                              *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    Class to store into this class                                                    *
 *                                                                                             *
 * OUTPUT:   Nothing                                                                           *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   3/9/00 1:10PM ST : Created                                                                *
 *=============================================================================================*/
bool IPAddressClass::operator = (const IPAddressClass &address)
{
	IsValid = ((IPAddressClass &)address).Is_Valid();

	/*
	** Can't read an invalid address without causing an assert.
	*/
	if (IsValid) {
		WholeAddress = ((IPAddressClass &)address).Get_Address();
		Port = ((IPAddressClass &)address).Get_Port();
	} else {
		WholeAddress = 0xffffffff;
		Port = 0;
	}
	return(IsValid);
}

