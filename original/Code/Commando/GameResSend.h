#ifndef __GAMERESSEND_H__
#define __GAMERESSEND_H__

#include <SList.h>

class cGameData;
class cPlayer;

void SendGameResults(unsigned long gameID, cGameData* theGame, SList<cPlayer>* players);

#ifdef _DEBUG
void SendTestGameResults(void);
#endif

#endif // __GAMERESSEND_H__
