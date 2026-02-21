#ifndef _CDKEYAUTH_H_
#define _CDKEYAUTH_H_

/********
INCLUDES
********/
#include <wwstring.h>

/********
DEFINES
********/

class CCDKeyAuth
{

protected:

	static void auth_callback(int localid, int authenticated, char *errmsg, void *instance);

public:
	static void GetSerialNum(StringClass &serial);
	static void DisconnectUser(int localid);
	static void AuthenticateUser(int localid, ULONG ip, char *challenge, char *authstring);
	static char *GenChallenge(int nchars);
	static void AuthSerial(const char *challenge, StringClass &resp);

};

#endif // CDKEYAUTH
