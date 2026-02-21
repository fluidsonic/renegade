#include "CDKeyAuth.h"

void CCDKeyAuth::GetSerialNum(StringClass &serial) { serial = ""; }
void CCDKeyAuth::DisconnectUser(int localid) {}
void CCDKeyAuth::AuthenticateUser(int localid, ULONG ip, char *challenge, char *authstring) {}
char *CCDKeyAuth::GenChallenge(int nchars) { return nullptr; }
void CCDKeyAuth::AuthSerial(const char *challenge, StringClass &resp) { resp = ""; }
void CCDKeyAuth::auth_callback(int localid, int authenticated, char *errmsg, void *instance) {}
