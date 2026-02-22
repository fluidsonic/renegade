//
// Filename:     wwpacket.cpp
// Project:      wwnet
// Author:       Tom Spencer-Smith
// Date:         June 1998
// Description:  Adding and extracting data from byte streams
//
//------------------------------------------------------------------------------------
#include "global.h"
#include "wwpacket.h"

#include "win.h"
#include "systimer.h"
#include "crc.h"
#include "quat.h"
#include "fromaddress.h"
#include "packettype.h"
#include "bitpackids.h"

#define	PACKET_ID_BITS		28		// Enough for about 100 packets per second for about a month.
#define	PACKET_TYPE_BITS	4		// Enough for 16 packet types (we currently have 7 at 9/20/2001 11:33PM)

DEFINE_AUTO_POOL(cPacket, 256)

//
// Class statics
//
const USHORT	cPacket::PACKET_HEADER_SIZE	= 7;
int				cPacket::RefCount					= 0;
bool				cPacket::EncoderInit				= true;
const unsigned long cPacket::DefSendTime		= 0xffffffff;

//------------------------------------------------------------------------------------
cPacket::cPacket() :
   Type(UNDEFINED_TYPE),
   Id(UNDEFINED_ID),
   SenderId(UNDEFINED_ID),
	SendTime(DefSendTime),
	FirstSendTime(DefSendTime),
   ResendCount(-1),			// so that first send doesn't count as a resend
	NumSends(1)
{
	//cEncoderList::Set_Compression_Enabled(false);

	RefCount++;

	if (EncoderInit) {
		Init_Encoder();
	}
}

//---------------- --------------------------------------------------------------------
cPacket::~cPacket()
{
	RefCount--;
}

//------------------------------------------------------------------------------------
//
// Assignment operator
//
cPacket& cPacket::operator=(const cPacket& source)
{
	PFromAddressWrapper	= source.PFromAddressWrapper;
   Type						= source.Type;
   Id							= source.Id;
	SenderId					= source.SenderId;
	SendTime					= source.SendTime;
	FirstSendTime			= source.FirstSendTime;
   ResendCount				= source.ResendCount;

	BitStreamClass::operator=(source);

	NumSends					= source.NumSends;

   return * this;
}

//------------------------------------------------------------------------------------
void cPacket::Add_Vector3(Vector3 & v)
{

	Add(v.X);
	Add(v.Y);
	Add(v.Z);
}

//------------------------------------------------------------------------------------
void cPacket::Get_Vector3(Vector3 & v)
{
	Get(v.X);
	Get(v.Y);
	Get(v.Z);

}

//------------------------------------------------------------------------------------
void cPacket::Add_Quaternion(Quaternion & q)
{

	Add(q.X);
	Add(q.Y);
	Add(q.Z);
	Add(q.W);
}

//------------------------------------------------------------------------------------
void cPacket::Get_Quaternion(Quaternion & q)
{
	Get(q.X);
	Get(q.Y);
	Get(q.Z);
	Get(q.W);

}

//------------------------------------------------------------------------------------
void cPacket::Set_Type(BYTE type)
{

   Type = type;
}

//------------------------------------------------------------------------------------
void cPacket::Set_Id(int id)
{
   Id = id;
}

//------------------------------------------------------------------------------------
void cPacket::Set_Sender_Id(int sender_id)
{
   SenderId = sender_id;
}

//------------------------------------------------------------------------------------
void cPacket::Set_Send_Time()
{
	unsigned long time = TIMEGETTIME();
	if (SendTime == DefSendTime) {
		FirstSendTime = time;
	}
	SendTime = time;
}

//------------------------------------------------------------------------------------
void cPacket::Set_Num_Sends(int num_sends)
{
	NumSends = num_sends;
}

/*
//------------------------------------------------------------------------------------
BYTE cPacket::Peek_Message_Type() const
{
	//
	// Note that using Get is the only valid way to read data from a packet!
	//

	cPacket temp_packet;
	temp_packet = *this;
	BYTE message_type;
	temp_packet.Get(message_type);
	temp_packet.Flush();

	return message_type;
}
*/

void cPacket::Init_Encoder(void)
{
	EncoderInit = false;
	cEncoderList::Set_Precision(BITPACK_PACKET_ID, PACKET_ID_BITS);
	cEncoderList::Set_Precision(BITPACK_PACKET_TYPE, PACKET_TYPE_BITS);
}

//------------------------------------------------------------------------------------
void cPacket::Construct_Full_Packet(cPacket & full_packet, cPacket & src_packet)
{

	full_packet.Add(src_packet.Get_Type(), BITPACK_PACKET_TYPE);
   full_packet.Add(src_packet.Get_Id(), BITPACK_PACKET_ID);
   full_packet.Add((BYTE)src_packet.Get_Sender_Id());
	full_packet.Add((USHORT)src_packet.Get_Bit_Length());

	int header_bit_length = full_packet.Get_Bit_Length();

	memcpy(
		full_packet.Get_Data() + PACKET_HEADER_SIZE,
		src_packet.Get_Data(),
		src_packet.Get_Compressed_Size_Bytes());
	unsigned int whole_bit_length = header_bit_length + src_packet.Get_Bit_Length();
	full_packet.Set_Bit_Length(whole_bit_length);

}

//------------------------------------------------------------------------------------
void cPacket::Construct_App_Packet(cPacket & packet, cPacket & full_packet)
{
	BYTE type;
	int packet_id;
	char sender_id;
	USHORT bit_size;

	full_packet.Get(type, BITPACK_PACKET_TYPE);
	full_packet.Get(packet_id, BITPACK_PACKET_ID);
	full_packet.Get(sender_id);
	full_packet.Get(bit_size);

	packet.Set_Type(type);
	packet.Set_Id(packet_id);
	packet.Set_Sender_Id(sender_id);
	packet.Set_Bit_Length(bit_size);
	packet.PFromAddressWrapper = full_packet.PFromAddressWrapper;
	memcpy(packet.Get_Data(), full_packet.Get_Data() + PACKET_HEADER_SIZE, packet.Get_Compressed_Size_Bytes());
}

	//ExecuteTime(0),
	//ReturnCode(0),
   //ExecuteTime				= source.ExecuteTime;
   //ReturnCode				= source.ReturnCode;

//------------------------------------------------------------------------------------
/*
void cPacket::Set_Execute_Time(int execute_time)
{
   ExecuteTime = execute_time;
}
*/

   /*
   if (!Is_Flushed()) {
   }
	*/
