#pragma once
#ifndef NATTER_H
#define NATTER_H

// natter.h stub - WOL NAT traversal removed (dead service)
#include "always.h"
#include "nataddr.h"
#include "wwstring.h"
#include "widestring.h"

#define PORT_BASE_MIN   1024
#define PORT_BASE_MAX   4095
#define PORT_POOL_MIN   4096
#define PORT_POOL_MAX   65535
#define OPTIONS_STAGING_BUFFER_SIZE 512

// Forward declarations
class SocketHandlerClass;
class RegistryClass;
class cPacket;
namespace WOL { struct User; }

class WOLNATInterfaceClass {
public:
    WOLNATInterfaceClass(void) : PortBase(1024), ForcePort(0),
        RegExternalIP(0), RegExternalPort(0), ChatExternalIP(0),
        ServiceSocketHandler(nullptr), IsServer(false) {}
    ~WOLNATInterfaceClass(void) {}

    void Init(void) {}
    void Shutdown(void) {}
    void Service(void) {}
    void Set_Service_Socket_Handler(SocketHandlerClass* socket) { (void)socket; }
    void Service_Receive_Queue(SocketHandlerClass* socket) { (void)socket; }

    void Get_Current_Server_ConnData(char* buffer, int size) { if (buffer && size > 0) buffer[0] = 0; }

    void Send_Private_Game_Options(WOL::User* user, char* options) { (void)user; (void)options; }
    bool Get_Private_Game_Options(WOL::User* user, char* options, int len) { (void)user; if (options && len > 0) options[0] = 0; return false; }

    char* Get_Silly_String(WideStringClass* silly_string, char* buffer, int buffer_size) { (void)silly_string; if (buffer && buffer_size > 0) buffer[0] = 0; return buffer; }

    bool Get_Packet(char* packet_buffer, int buffer_size, IPAddressClass& address) { (void)packet_buffer; (void)buffer_size; (void)address; return false; }
    void Intercept_Game_Packet(cPacket& packet) { (void)packet; }
    bool Send_Game_Format_Packet_To(IPAddressClass* address, char* payload, int payload_size, SocketHandlerClass* socket_handler = nullptr) { (void)address; (void)payload; (void)payload_size; (void)socket_handler; return false; }

    bool Get_My_Name(char* namebuf) { if (namebuf) namebuf[0] = 0; return false; }
    bool Am_I_Server(void) { return IsServer; }
    void Set_Server(bool is_server) { IsServer = is_server; }
    void Set_Server_Negotiated_Address(IPAddressClass* server_address) { (void)server_address; }
    int Get_Num_Mangler_Servers(void) { return 0; }
    unsigned short Get_Mangler_Port_By_Index(int index) { (void)index; return 0; }
    bool Get_Mangler_Name_By_Index(int index, char* mangler_name) { (void)index; if (mangler_name) mangler_name[0] = 0; return false; }
    unsigned long Get_Local_Address(void) { return 0; }
    void Tell_Server_That_Client_Is_In_Channel(void) {}
    bool Is_NAT_Thread_Busy(void) { return false; }

    unsigned short Get_Next_Client_Port(void) { return PortBase++; }
    unsigned short Get_Port_As_Server(void) { return 4001; }
    unsigned short Get_Port_As_Server_Client(void) { return 4002; }
    unsigned short Get_Force_Port(void) { return ForcePort; }

    void Get_Config(RegistryClass* reg, int& port_number, bool& send_delay) { (void)reg; port_number = 0; send_delay = false; }
    void Set_Config(RegistryClass* reg, int port_number, bool send_delay) { (void)reg; (void)port_number; (void)send_delay; }
    void Save_Firewall_Info_To_Registry(void) {}
    unsigned long Get_Reg_External_IP(void) { return RegExternalIP; }
    unsigned long Get_Reg_External_Port(void) { return RegExternalPort; }
    void Get_Compact_Log(StringClass& log_string) { (void)log_string; }
    unsigned long Get_Chat_External_IP(void) { return ChatExternalIP; }

    enum {
        OPTION_ERROR = 'a',
        OPTION_INVITE_PORT_NEGOTIATION,
        OPTION_ACCEPT_PORT_NEGOTIATION_INVITATION,
        OPTION_PORT_NOTIFICATION,
        OPTION_CONNECTION_RESULT,
        OPTION_QUEUE_STATE,
        OPTION_ABORT_NEGOTIATION,
        OPTION_CLIENT_IN_CHANNEL
    };

    typedef struct tPrivateGameOptions {
        char NATOptionsPrefix[4];
        char Option;
        union tOptionData {
            struct InvitationStruct {
                char LocalIP[9]; char LocalPort[5]; char ExternalIP[9];
                char FirewallType[9]; char Queued[5];
            } Invitation;
            struct AcceptStruct {
                char LocalIP[9]; char LocalPort[5]; char ExternalIP[9];
                char FirewallType[9];
            } Accept;
            struct PortStruct {
                char MangledPort[5]; char Name[64];
            } Port;
            struct ConnectionResultStruct {
                char Result[2]; char Port[5]; char Name[64];
            } ConnectionResult;
            struct QueueStateStruct { char Position[3]; } QueueState;
            struct QuitTalkingStruct { char Nothing; } QuitTalking;
            struct ClientInChannelStruct { char Name[64]; } ClientInChannel;
        } OptionData;
    } PrivateGameOptionsStruct;

private:
    unsigned short PortBase;
    unsigned short ForcePort;
    unsigned long RegExternalIP;
    unsigned short RegExternalPort;
    unsigned long ChatExternalIP;
    SocketHandlerClass* ServiceSocketHandler;
    bool IsServer;
};

extern WOLNATInterfaceClass WOLNATInterface;

#endif // NATTER_H
