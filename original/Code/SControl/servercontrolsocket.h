#pragma once

#include "global.h"

#include	"assert.h"
#include	"vector.h"

#include	<winsock.h>

#ifndef DebugString
#define DebugString(...) ((void)0)
#endif

#define fw_assert assert

#ifdef errno
#undef errno
#endif	//errno

#define errno (WSAGetLastError())
#define LAST_ERROR errno

#ifndef TIMER_SECOND
#define TIMER_SECOND 1000
#endif //TIMER_SECOND

/*
** Length of winsocks internal buffer.
*/
#define SERVER_CONTROL_SOCKET_BUFFER_SIZE 8192

/*
** Length of our temporary receive buffer. Needs to be more that the max packet size which is about 550 bytes.
*/
#define SERVER_CONTROL_RECEIVE_BUFFER_LEN	640

/*
** Number of statically allocated packet buffers for the class
*/
#define SERVER_CONTROL_MAX_STATIC_BUFFERS	32

/*
** Class to manage low level comms for talking to Mangler Servers.
**
** Can't use the Renegade packet comms since the packet format is different - Mangler servers expect C&C packet format.
*/
class ServerControlSocketClass
{
	public:

		/*
		** Constructor, destructor.
		*/
		ServerControlSocketClass(void);
		~ServerControlSocketClass(void);

		/*
		** Startup, shutdown.
		*/
		bool Open(int port, bool loopback = false, unsigned long ip = 0);
		void Close(void);
		void Discard_In_Buffers(void);
		void Discard_Out_Buffers(void);
		void Set_Encryption_Key(char *key);

		/*
		** Read, write.
		*/
		int Peek(void *buffer, int buffer_len, void *address, unsigned short *port, int packetnum = 0);
		int Read(void *buffer, int buffer_len, void *address, unsigned short *port, int packetnum = 0);
		void Write(void *buffer, int buffer_len, void *address, unsigned short port = 0);

		/*
		** Service.
		*/
		void Service(void);

		/*
		** Error handling.
		*/
		void Clear_Socket_Error(void);

	private:

		/*
		** The socket associated with this class.
		*/
		SOCKET Socket;

		/*
		** The port that this class listens on.
		*/
		int Port;

		/*
		** This struct contains the information needed for each incoming and outgoing packet.
		** It acts as a temporary control for these packets.
		*/
		struct WinsockBufferType {
			unsigned char		Address[4];		// Address. IN_ADDR
			int					BufferLen;		// Length of data in buffer
			bool					IsBroadcast;	// Flag to broadcast this packet
			bool					InUse;			// Useage state of buffer
			bool					IsAllocated;	// false means statically allocated.
			unsigned short		Port;				// Override port. Send to this port if not 0. Save incoming port number.
			unsigned long		CRC;				// CRC of packet for extra sanity.
			unsigned char		Buffer[SERVER_CONTROL_RECEIVE_BUFFER_LEN];	// Buffer to store packet in.
		};

		/*
		** Packet buffer allocation.
		*/
		void *Get_New_Out_Buffer(void);
		void *Get_New_In_Buffer(void);

		/*
		** Packet CRCs.
		*/
		void Add_CRC(unsigned long *crc, unsigned long val);
		virtual void Build_Packet_CRC(WinsockBufferType *packet);
		virtual bool Passes_CRC_Check(WinsockBufferType *packet);

		/*
		** Encryption.
		*/
		void Encrypt(unsigned char *packet, int size);
		void Decrypt(unsigned char *packet, int size);

		/*
		** Array of buffers to temporarily store incoming and outgoing packets.
		*/
		DynamicVectorClass<WinsockBufferType*> InBuffers;
		DynamicVectorClass<WinsockBufferType*> OutBuffers;

		/*
		** Array of buffers that are always available for incoming packets.
		*/
		WinsockBufferType StaticInBuffers[SERVER_CONTROL_MAX_STATIC_BUFFERS];
		WinsockBufferType StaticOutBuffers[SERVER_CONTROL_MAX_STATIC_BUFFERS];

		/*
		** Pointers to allow circular use of the buffer arrays.
		*/
		int InBufferArrayPos;
		int OutBufferArrayPos;

		/*
		** Usage count for each array.
		*/
		int InBuffersUsed;
		int OutBuffersUsed;

		/*
		** Temporary receive buffer to use when querying Winsock for incoming packets.
		*/
		unsigned char ReceiveBuffer[SERVER_CONTROL_RECEIVE_BUFFER_LEN];

		/*
		** Encryption key, only 1st 8 bytes used. The rest is for safety.
		*/
		char Key[12];
};
