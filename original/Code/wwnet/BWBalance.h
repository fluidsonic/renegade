#pragma once

#include "global.h"

#ifndef _BWBALANCE_H

class cConnection;

/*
** This class balances bandwidth between clients.
**
**
*/
class BandwidthBalancerClass
{
	public:

		/*
		** Constructor, desturctor.
		*/
		BandwidthBalancerClass(void);
		~BandwidthBalancerClass(void);

		/*
		** Public function.
		*/
		void Adjust(cConnection *connection, bool is_dedicated);

		bool IsEnabled;

	private:

		unsigned long Allocate_Bandwidth(float average_priority, int bw_adjust, unsigned long total_server_bbo);
		void Allocate_Client_Structs(int num_structs);
		void Adjust_Connection_Budget(cConnection *connection);

		/*
		** Struct to keep info about each client.
		*/
		struct ClientInfoStruct {
			float AveragePriority;
			unsigned long MaxBpsDown;
			unsigned long AllocatedBBO;
			bool IsLoading;
			unsigned long ID;
			bool IsDone;
		};

		/*
		** Pointer to array of client info structs.
		*/
		ClientInfoStruct *ClientInfo;

		/*
		** Number of client info structs.
		*/
		int NumClientStructs;

		/*
		** Number of clients to balance between.
		*/
		int NumClients;
};

extern BandwidthBalancerClass BandwidthBalancer;

#endif //_BWBALANCE_H
