// WWOnline types stub - Westwood Online (dead service) type stubs for compilation
#pragma once
#ifndef WWONLINE_TYPES_H
#define WWONLINE_TYPES_H

#include "RefPtr.h"
#include "WaitCondition.h"
#include "windef.h"
#include "winnt.h"
#include <Notify.h>
#include "widestring.h"

namespace WWOnline {

// Forward declare everything needed
struct UserData;
struct ChannelData;
struct IRCServerData;
struct LadderData;
struct LoginInfo;
struct PageMessage;
struct ChatMessage;
struct GameOptionsMessage;
struct Product;
struct SquadData;

typedef int LadderType;
static const LadderType LadderType_Clan = 0;
static const LadderType LadderType_Team = 1;

static const int LANGCODE_CHINESE  = 0;
static const int LANGCODE_KOREAN   = 1;
static const int LANGCODE_JAPANESE = 2;

enum USERLOCATION {
    USERLOCATION_UNKNOWN    = -1,
    USERLOCATION_OFFLINE    = 0,
    USERLOCATION_NO_CHANNEL = 1,
    USERLOCATION_IN_CHANNEL = 2,
    USERLOCATION_HIDING     = 3,
};

enum PageSendStatus {
    PAGESEND_SENT = 0,
    PAGESEND_ERROR,
    PAGESEND_OFFLINE,
    PAGESEND_HIDING,
    PAGE_OK = PAGESEND_SENT,
    PAGE_FAILED = PAGESEND_ERROR
};

// ConnectionStatus: matches original WOLSession.h enum values
enum ConnectionStatus {
    ConnectionDisconnected = 0,
    ConnectionDisconnecting,
    ConnectionConnecting,
    ConnectionConnected
};

// Error constants
static const int CHAT_E_NOT_OPER    = -1;
static const int CHAT_E_BANNED      = -2;
static const int CHAT_E_MUSTPATCH   = -3;

// User detail request flags
static const unsigned long REQUEST_LOCALE    = 0x01;
static const unsigned long REQUEST_SQUADINFO = 0x02;

class ServerError {
public:
    const wchar_t* GetDescription(void) const { return L""; }
    int GetErrorCode(void) const { return 0; }
};

// Stub data structures
struct UserData {
    char Name[64];
    int Location;
    int Flags;
    UserData() : Location(0), Flags(0) { Name[0] = 0; }
    WideStringClass GetName(void) const { return WideStringClass(L""); }
    int GetLocation(void) const { return Location; }
    int GetFlags(void) const { return Flags; }
    unsigned long GetSquadID(void) const { return 0; }
    int GetLocale(void) const { return 0; }
    // Ladder info
    RefPtr<LadderData> GetTeamLadder(void) const { return RefPtr<LadderData>(); }
    RefPtr<LadderData> GetClanLadder(void) const { return RefPtr<LadderData>(); }
    // Squad
    RefPtr<SquadData> GetSquad(void) const { return RefPtr<SquadData>(); }
    // Channel (where user is)
    RefPtr<ChannelData> GetChannel(void) const { return RefPtr<ChannelData>(); }
    // Factory
    static RefPtr<UserData> Create(const wchar_t* name) { return RefPtr<UserData>(); }
};

// Channel mode flags (subset of IRC modes)
static const int CHAN_MODE_SECRET      = 0x0001;
static const int CHAN_MODE_INVITEONLY  = 0x0002;
static const int CHAN_MODE_BAN         = 0x0004;
static const int CHAN_MODE_KEY         = 0x0008;
static const int CHAN_MODE_LIMIT       = 0x0010;

struct ChannelData {
    char Name[128];
    wchar_t WName[128];
    char ExtraInfo[64];
    char Topic[128];
    int UserCount;
    int MinUsers;
    int MaxUsers;
    int Type;
    int Flags;
    bool Tournament;
    ChannelData() : UserCount(0), MinUsers(0), MaxUsers(0), Type(0), Flags(0), Tournament(false) {
        Name[0] = 0; WName[0] = 0; ExtraInfo[0] = 0; Topic[0] = 0;
    }
    const wchar_t* GetName(void) const { return WName; }
    int  GetUserCount(void) const { return UserCount; }
    int  GetMinUsers(void) const { return MinUsers; }
    int  GetMaxUsers(void) const { return MaxUsers; }
    int  GetCurrentUsers(void) const { return UserCount; }
    bool CanUserJoin(const RefPtr<UserData>& user) const { return false; }
    int  GetType(void) const { return Type; }
    int  GetFlags(void) const { return Flags; }
    bool IsPassworded(void) const { return false; }
    bool GetTournament(void) const { return Tournament; }
    const char* GetExtraInfo(void) const { return ExtraInfo[0] ? ExtraInfo : nullptr; }
    const char* GetTopic(void) const { return Topic[0] ? Topic : nullptr; }
    void SetMinMaxUsers(int minU, int maxU) { MinUsers = minU; MaxUsers = maxU; }
    void SetTournament(unsigned int t) { Tournament = (t != 0); }
    void SetExtraInfo(const char* info) { if (info) { strncpy(ExtraInfo, info, 63); ExtraInfo[63]=0; } else ExtraInfo[0]=0; }
    void SetTopic(const char* topic) { if (topic) { strncpy(Topic, topic, 127); Topic[127]=0; } else Topic[0]=0; }
};

struct IRCServerData {
    char Address[128];
    int Port;
    int LangCode;
    float Longitude;
    float Lattitude;
    IRCServerData() : Port(0), LangCode(0), Longitude(0.0f), Lattitude(0.0f) { Address[0] = 0; }
    const char*    GetName(void) const { return ""; }
    bool HasLanguageCode(int code) const { return false; }
    bool HasLanguageCode(void) const { return false; }
    bool MatchesLanguageCode(void) const { return false; }
    int  GetLangCode(void) const { return LangCode; }
    float GetLongitude(void) const { return Longitude; }
    float GetLattitude(void) const { return Lattitude; }
};

struct LadderData {
    int Rank;
    int Wins;
    int Losses;
    unsigned int Points;
    unsigned int Reserved2;
    LadderData() : Rank(0), Wins(0), Losses(0), Points(0), Reserved2(0) {}
    unsigned int GetPoints(void) const { return Points; }
    unsigned int GetReserved2(void) const { return Reserved2; }
};

struct LoginInfoList {
    int  size(void) const { return 0; }
    RefPtr<LoginInfo> operator[](int) const { return RefPtr<LoginInfo>(); }
};

struct LoginInfo {
    char Nick[64];
    char Password[64];
    bool mIsPasswordEncrypted;
    LoginInfo() : mIsPasswordEncrypted(false) { Nick[0] = 0; Password[0] = 0; }
    static RefPtr<LoginInfo> Create(const wchar_t* name, const wchar_t* pw, bool encrypted) { return RefPtr<LoginInfo>(); }
    static RefPtr<LoginInfo> Create(const char* name, const char* pw, bool encrypted) { return RefPtr<LoginInfo>(); }
    static RefPtr<LoginInfo> Find(const wchar_t* name) { return RefPtr<LoginInfo>(); }
    static const LoginInfoList& GetList(void) { static LoginInfoList l; return l; }
    bool IsStored(void) const { return false; }
    bool IsPasswordEncrypted(void) const { return mIsPasswordEncrypted; }
    void SetPassword(const wchar_t* pw, bool encrypted = false) { mIsPasswordEncrypted = encrypted; }
    void Forget(bool purge = false) {}
    void Remember(bool store) {}
    WideStringClass GetNickname(void) const { return WideStringClass(L""); }
    const wchar_t* GetPassword(void) const { return L""; }
    int GetLocale(void) const { return 0; }
    void SetLocale(int locale) {}
};

struct PageMessage {
    wchar_t From[64];
    wchar_t Message[512];
    PageMessage() { From[0] = 0; Message[0] = 0; }
    PageMessage(const wchar_t* from, const wchar_t* msg) {
        if (from) { wcsncpy(From, from, 63); From[63]=0; } else From[0]=0;
        if (msg)  { wcsncpy(Message, msg, 511); Message[511]=0; } else Message[0]=0;
    }
    const wchar_t* GetPagersName(void) const { return From; }
    const wchar_t* GetPageMessage(void) const { return Message; }
};

struct ChatMessage {
    const wchar_t* Sender;
    const wchar_t* Text;
    bool Private;
    bool Action;
    ChatMessage() : Sender(nullptr), Text(nullptr), Private(false), Action(false) {}
    ChatMessage(const wchar_t* sender, const wchar_t* text, bool isPrivate = false, bool isAction = false)
        : Sender(sender), Text(text), Private(isPrivate), Action(isAction) {}
    bool IsSenderMyself() const { return false; }
    bool IsSenderChannelOwner() const { return false; }
    bool IsSenderSquelched(void) const { return false; }
    bool IsSenderInClan(unsigned long clanID) const { return false; }
    const wchar_t* GetSendersName() const { return Sender ? Sender : L""; }
    const wchar_t* GetText() const { return Text ? Text : L""; }
    const wchar_t* GetMessage() const { return Text ? Text : L""; }
    bool IsPrivate() const { return Private; }
    bool IsAction() const { return Action; }
};

struct GameOptionsMessage {
    char Data[256];
    GameOptionsMessage() { Data[0] = 0; }
    const wchar_t* GetSendersName(void) const { return L""; }
    const char*    GetOptions(void) const { return Data; }
};

struct Product {
    int ID;
    char Name[64];
    Product() : ID(0) { Name[0] = 0; }
    static RefPtr<Product> Current(void) { return RefPtr<Product>(); }
    unsigned long GetSKU(void) const { return 0; }
    unsigned long GetGameCode(void) const { return 0; }
    const wchar_t* GetChannelPassword(void) const { return L""; }
};

struct SquadData {
    int ID;
    char Name[64];
    char Abbr[16];
    SquadData() : ID(0) { Name[0] = 0; Abbr[0] = 0; }
    const char* GetAbbr(void) const { return Abbr; }
};

// PingProfile: array of 8 ping times (one per region)
typedef struct { unsigned char Pings[8]; } PingProfile;
inline const PingProfile& GetLocalPingProfile(void) { static PingProfile p; return p; }
inline long ComparePingProfile(const PingProfile& p1, const PingProfile& p2) { return 0; }
inline int  EncodePingProfile(const PingProfile& pings, char* buffer) { if (buffer) buffer[0]=0; return 0; }
inline void DecodePingProfile(const char* buffer, PingProfile& pings) {}

// Download stub
struct Download {
    const wchar_t* GetStatusText() const { return L""; }
    const wchar_t* GetErrorText()  const { return L""; }
    int  GetStatusCode(void) const { return 0; }
    int  GetErrorCode(void) const  { return 0; }
    void GetProgress(int& read, int& size, int& elapsed, int& remaining) const {
        read=0; size=0; elapsed=0; remaining=0;
    }
};

// List types - stub vector-like containers
#include <vector>
typedef std::vector<RefPtr<Download>> DownloadList;
typedef std::vector<RefPtr<UserData>>    UserList;
typedef std::vector<RefPtr<ChannelData>> ChannelList;
typedef std::vector<RefPtr<IRCServerData>> IRCServerList;
typedef std::vector<ChatMessage>  ChatMessageList;
typedef std::vector<PageMessage>  PageMessageList;
typedef void* MGLServerList;
struct PingServerData {
    float Longitude;
    float Lattitude;
    int   PingTime;
    PingServerData() : Longitude(0.0f), Lattitude(0.0f), PingTime(-1) {}
    float GetLongitude(void) const { return Longitude; }
    float GetLattitude(void) const { return Lattitude; }
    int   GetPingTime(void)  const { return PingTime; }
};
struct PingServerList {
    int size() const { return 0; }
    RefPtr<PingServerData> operator[](int) const { return RefPtr<PingServerData>(); }
};
typedef void* LadderList;

typedef void (*DownloadWaitCallback)(struct DownloadEvent& event, unsigned long userdata);

// Wait subtypes
class DownloadWait : public WaitCondition {
public:
    static RefPtr<DownloadWait> Create(const DownloadList& files) { return RefPtr<DownloadWait>(); }
    bool IsValid() const { return false; }
    void SetCallback(DownloadWaitCallback cb, unsigned long data) {}
    void WaitBeginning(void) {}
    const wchar_t* GetResultText() const { return L""; }
};

// Event types (stubs)
struct UserEvent {
    enum Event { Error=0, NewData, Join, Leave, Located, Kicked, Banned, Locale, SquadInfo, LadderInfo };
    Event GetEvent(void) const { return Join; }
    RefPtr<UserData> Subject(void) const { return RefPtr<UserData>(); }
};
struct UserIPEvent {};
// ChannelStatus - global enum in WWOnline namespace
enum ChannelStatus {
    ChannelLeft = 0,
    ChannelJoined,
    ChannelCreated,
    ChannelNotFound,
    ChannelExists,
    ChannelBadPassword,
    ChannelFull,
    ChannelBanned,
    ChannelKicked,
    ChannelNewData,
    ChannelLeaving
};

struct ChannelEvent {
    ChannelStatus GetStatus(void) const { return ChannelLeft; }
    const RefPtr<ChannelData>& Subject(void) const { static RefPtr<ChannelData> dummy; return dummy; }
};
struct ChannelListEvent {
    enum Event { Error = -1, NewList, Update, Remove };
    Event GetEvent(void) const { return NewList; }
    int   GetChannelType(void) const { return 0; }
    const ChannelList& Subject(void) const { static ChannelList dummy; return dummy; }
};
struct BuddyEvent {
    enum Event { Error=-1, NewList, Update, Remove, Added, Deleted };
    Event GetEvent(void) const { return NewList; }
    const UserList& Subject(void) const { static UserList dummy; return dummy; }
};
struct LadderInfoEvent {
    const LadderData& GetWOLLadder() const { static LadderData d; return d; }
};
struct DownloadEvent {
    enum Type { DOWNLOAD_BEGIN=0, DOWNLOAD_END, DOWNLOAD_PROGRESS,
                DOWNLOAD_STATUS, DOWNLOAD_STOPPED, DOWNLOAD_ERROR };
    Type GetEvent() const { return DOWNLOAD_BEGIN; }
    RefPtr<Download> GetDownload() const { return RefPtr<Download>(); }
};
struct GameStartEvent {};
struct MessageOfTheDayEvent {
    WideStringClass& Subject(void) const { static WideStringClass dummy; return dummy; }
};
struct SquadEvent {};
struct ChatObject {
    void RequestGameEnd() {}
};

// Main Session stub - inherits from Notifier for all event types the game uses
class Session :
    public Notifier<UserEvent>,
    public Notifier<UserIPEvent>,
    public Notifier<UserList>,
    public Notifier<ChannelEvent>,
    public Notifier<ChannelListEvent>,
    public Notifier<LadderInfoEvent>,
    public Notifier<GameStartEvent>,
    public Notifier<ServerError>,
    public Notifier<ConnectionStatus>,
    public Notifier<GameOptionsMessage>,
    public Notifier<ChatMessage>,
    public Notifier<SquadEvent>,
    public Notifier<MessageOfTheDayEvent>,
    public Notifier<DownloadEvent>,
    public Notifier<BuddyEvent>,
    public Notifier<PageMessage>,
    public Notifier<PageSendStatus>
{
public:
    Session() {}
    virtual ~Session() {}

    // Factory
    static RefPtr<Session> GetInstance(bool create = true) { return RefPtr<Session>(); }

    // Connection
    RefPtr<WaitCondition> ConnectClient(void) { return RefPtr<WaitCondition>(); }
    RefPtr<WaitCondition> Disconnect(void)    { return RefPtr<WaitCondition>(); }
    bool IsConnected(void) const { return false; }
    bool IsAutoLoginAllowed(void) const { return false; }
    bool IsStoreLoginAllowed(void) const { return false; }
    ConnectionStatus GetConnectionStatus(void) const { return ConnectionDisconnected; }
    bool AllowFindPage(bool allowFind, bool allowPage) { return false; }
    bool SetBadLanguageFilter(bool enabled) { return false; }

    // User
    RefPtr<UserData> GetCurrentUser(void) const { return RefPtr<UserData>(); }
    bool IsCurrentUser(const UserData* u) const { return false; }
    const UserList& GetUserList(void) const { static UserList dummy; return dummy; }
    RefPtr<UserData> FindUser(const wchar_t* name) const { return RefPtr<UserData>(); }
    RefPtr<WaitCondition> RequestUserDetails(const wchar_t* name) { return RefPtr<WaitCondition>(); }
    RefPtr<WaitCondition> RequestUserDetails(const RefPtr<UserData>& user, unsigned long flags) { return RefPtr<WaitCondition>(); }
    RefPtr<WaitCondition> RequestLocateUser(const wchar_t* name) { return RefPtr<WaitCondition>(); }
    RefPtr<WaitCondition> RequestLocateUser(const RefPtr<UserData>& user) { return RefPtr<WaitCondition>(); }
    RefPtr<WaitCondition> SquelchUser(const wchar_t* name, bool squelch) { return RefPtr<WaitCondition>(); }
    RefPtr<WaitCondition> SquelchUser(const RefPtr<UserData>& user, bool squelch) { return RefPtr<WaitCondition>(); }
    RefPtr<WaitCondition> KickUser(const wchar_t* name) { return RefPtr<WaitCondition>(); }
    RefPtr<WaitCondition> BanUser(const wchar_t* name) { return RefPtr<WaitCondition>(); }
    RefPtr<WaitCondition> BanUser(const wchar_t* name, bool ban) { return RefPtr<WaitCondition>(); }
    RefPtr<WaitCondition> PageUser(const wchar_t* name, const wchar_t* msg) { return RefPtr<WaitCondition>(); }

    // Login
    RefPtr<LoginInfo> GetCurrentLogin(void) const { return RefPtr<LoginInfo>(); }
    RefPtr<WaitCondition> LoginServer(const RefPtr<IRCServerData>& server, const RefPtr<LoginInfo>& login) { return RefPtr<WaitCondition>(); }
    RefPtr<WaitCondition> Logout(void) { return RefPtr<WaitCondition>(); }

    // Channel
    RefPtr<ChannelData> GetCurrentChannel(void) const { return RefPtr<ChannelData>(); }
    const ChannelList& GetChannelList(void) const { static ChannelList dummy; return dummy; }
    RefPtr<WaitCondition> JoinChannel(const ChannelData* ch, const wchar_t* pw = nullptr) { return RefPtr<WaitCondition>(); }
    RefPtr<WaitCondition> JoinChannel(const RefPtr<ChannelData>& ch, const wchar_t* pw = nullptr) { return RefPtr<WaitCondition>(); }
    // JoinChannel by name (used for QuickMatch and joining by game name)
    RefPtr<WaitCondition> JoinChannel(const wchar_t* name, const wchar_t* pw, int gameType) { return RefPtr<WaitCondition>(); }
    RefPtr<WaitCondition> LeaveChannel(void) { return RefPtr<WaitCondition>(); }
    RefPtr<WaitCondition> CreateChannel(const ChannelData* ch) { return RefPtr<WaitCondition>(); }
    RefPtr<WaitCondition> CreateChannel(const wchar_t* name, const wchar_t* pw, int flags) { return RefPtr<WaitCondition>(); }
    RefPtr<WaitCondition> RequestChannelList(void) { return RefPtr<WaitCondition>(); }
    RefPtr<WaitCondition> RequestChannelList(int type, bool flag) { return RefPtr<WaitCondition>(); }
    RefPtr<WaitCondition> RequestGameChannelList(void) { return RefPtr<WaitCondition>(); }
    RefPtr<WaitCondition> GetNewChatChannelList(void) { return RefPtr<WaitCondition>(); }
    RefPtr<ChannelData> FindChatChannel(const wchar_t* name) { return RefPtr<ChannelData>(); }
    ConnectionStatus GetChannelStatus(void) const { return ConnectionDisconnected; }
    const wchar_t* GetChannelPassword(void) const { return L""; }
    RefPtr<WaitCondition> SendPublicMessage(const wchar_t* msg) { return RefPtr<WaitCondition>(); }
    RefPtr<WaitCondition> SendPublicMessage(const char* msg) { return RefPtr<WaitCondition>(); }
    RefPtr<WaitCondition> SendPublicAction(const wchar_t* msg) { return RefPtr<WaitCondition>(); }
    RefPtr<WaitCondition> SendPrivateMessage(const wchar_t* to, const wchar_t* msg) { return RefPtr<WaitCondition>(); }
    RefPtr<WaitCondition> SendPrivateMessage(const UserList& users, const wchar_t* msg) { return RefPtr<WaitCondition>(); }
    RefPtr<WaitCondition> SendPrivateAction(const wchar_t* to, const wchar_t* msg) { return RefPtr<WaitCondition>(); }
    RefPtr<WaitCondition> SendPrivateAction(const UserList& users, const wchar_t* msg) { return RefPtr<WaitCondition>(); }
    RefPtr<WaitCondition> SendPrivateGameOptions(const wchar_t* to, const char* opts) { return RefPtr<WaitCondition>(); }
    RefPtr<WaitCondition> SendChannelTopic(const wchar_t* topic) { return RefPtr<WaitCondition>(); }
    RefPtr<WaitCondition> SendChannelExtraInfo(const wchar_t* info) { return RefPtr<WaitCondition>(); }
    void SetAutoRequestFlags(unsigned int flags) {}
    void EnableProgressiveChannelList(bool enable) {}

    // Server
    RefPtr<IRCServerData> GetCurrentServer(void) const { return RefPtr<IRCServerData>(); }
    const IRCServerList& GetIRCServerList(void) const { static IRCServerList dummy; return dummy; }
    RefPtr<WaitCondition> GetNewServerList(void) { return RefPtr<WaitCondition>(); }
    RefPtr<WaitCondition> RequestServerList(void) { return RefPtr<WaitCondition>(); }

    // Buddy list
    const UserList& GetBuddyList(void) const { static UserList dummy; return dummy; }
    RefPtr<UserData> FindBuddy(const wchar_t* name) const { return RefPtr<UserData>(); }
    RefPtr<WaitCondition> RequestBuddyList(void) { return RefPtr<WaitCondition>(); }
    RefPtr<WaitCondition> AddBuddy(const wchar_t* name) { return RefPtr<WaitCondition>(); }
    RefPtr<WaitCondition> RemoveBuddy(const wchar_t* name) { return RefPtr<WaitCondition>(); }

    // Ping
    unsigned int GetPendingPingCount(void) const { return 0; }
    const PingServerList& GetPingServerList(void) const { static PingServerList d; return d; }
    void EnablePinging(bool enable) {}

    // Ladder
    RefPtr<WaitCondition> RequestLadderInfo(LadderType type) { return RefPtr<WaitCondition>(); }
    RefPtr<WaitCondition> RequestLadderInfo(const wchar_t* name, LadderType type) { return RefPtr<WaitCondition>(); }

    // Squad
    RefPtr<WaitCondition> RequestSquadInfoByID(int id) { return RefPtr<WaitCondition>(); }
    RefPtr<WaitCondition> RequestSquadInfoByMemberName(const wchar_t* name) { return RefPtr<WaitCondition>(); }

    // Locale (templated so caller can pass std::vector<WideStringClass> or void*)
    template<typename T>
    void GetLocaleStrings(T& out) {}

    // Download
    RefPtr<DownloadWait> GetDownloadWait(void) const { return RefPtr<DownloadWait>(); }
    const DownloadList&  GetPatchDownloadList(void) const { static DownloadList d; return d; }

    // Chat object (game management)
    ChatObject* GetChatObject(void) { static ChatObject c; return &c; }

    // Process/Think
    void Process(void) {}
    void Think(void) {}
    void Reset(void) {}

    // Game results (dead WOL service)
    RefPtr<WaitCondition> SendGameResults(const unsigned char* packet, unsigned long size) { return RefPtr<WaitCondition>(); }

    // Insider / server time stubs (WOL dead)
    bool IsCurrUserInsider(void) const { return false; }
    time_t GetServerTime(void) const { return 0; }

    // Mangler / NAT stubs
    const MGLServerList& GetManglerServerList(void) const { static MGLServerList dummy = nullptr; return dummy; }
    RefPtr<WaitCondition> RequestUserIP(const wchar_t* name) { return RefPtr<WaitCondition>(); }
};

} // namespace WWOnline

// -----------------------------------------------------------------------
// WOL namespace - legacy Westwood Online C-style structs
namespace WOL {

typedef int Locale;
static const Locale LOC_UNKNOWN = 0;

struct User {
    char login[64];
    char name[64];
    unsigned long ip;
    int flags;
    Locale locale;
    User() : ip(0), flags(0), locale(LOC_UNKNOWN) { login[0]=0; name[0]=0; }
};

struct Channel {
    char name[128];
    int users;
    int currentUsers;
    int maxUsers;
    Channel() : users(0), currentUsers(0), maxUsers(0) { name[0]=0; }
};

struct Server {
    char connlabel[64];
    char name[64];
    int timezone;
    float longitude;
    float lattitude;
    Server() : timezone(0), longitude(0), lattitude(0) { connlabel[0]=0; name[0]=0; }
};

struct Ladder {
    int rank;
    int wins;
    int losses;
    Ladder() : rank(0), wins(0), losses(0) {}
};

} // namespace WOL

#endif // WWONLINE_TYPES_H
