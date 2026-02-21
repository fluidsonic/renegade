#ifndef __ANNOUNCEEVENT_H__
#define __ANNOUNCEEVENT_H__

#include "NetEvent.h"
#include "NetClassIDs.h"

enum AnnouncementEnum
	{
	ANNOUNCEMENT_PUBLIC = 0,
	ANNOUNCEMENT_TEAM,
	ANNOUNCEMENT_PRIVATE,
	};

class	CSAnnouncement :
		public cNetEvent
	{
	public:
		CSAnnouncement();
		virtual ~CSAnnouncement();

		void Init(int to_id, int announcementID, AnnouncementEnum type, int radio_cmd_id = -1);

		virtual void Export_Creation(BitStreamClass& packet);
		virtual void Import_Creation(BitStreamClass& packet);

		virtual uint32 Get_Network_Class_ID(void) const
			{return NETCLASSID_CSANNOUNCEMENT;}

	protected:
		CSAnnouncement(const CSAnnouncement&);
		const CSAnnouncement& operator=(const CSAnnouncement&);

		virtual void Act(void);

		int mToID;
		int mFromID;
		int mAnnouncementID;
		int mRadioCmdID;
		AnnouncementEnum mType;
	};


class	SCAnnouncement :
		public cNetEvent
	{
	public:
		SCAnnouncement();
		virtual ~SCAnnouncement();

		void Init(int to_id, int from_id, int announcementID, AnnouncementEnum type, int radio_cmd_id = -1);

		virtual void Export_Creation(BitStreamClass& packet);
		virtual void Import_Creation(BitStreamClass& packet);

		virtual uint32 Get_Network_Class_ID(void) const
			{return NETCLASSID_SCANNOUNCEMENT;}

	protected:
		SCAnnouncement(const SCAnnouncement&);
		const SCAnnouncement& operator=(const SCAnnouncement&);

		virtual void Act(void);
		void Set_Dirty_Bit_For_Team(DIRTY_BIT bit, int team);

		int mToID;
		int mFromID;
		int mAnnouncementID;
		int mRadioCmdID;
		AnnouncementEnum mType;
	};

#endif // __ANNOUNCEEVENT_H__
