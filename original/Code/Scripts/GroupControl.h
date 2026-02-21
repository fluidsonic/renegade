#ifndef _GROUPCONTROL_H_
#define _GROUPCONTROL_H_

#include "scripts.h"
#include "slist.h"

class Group;

class GroupController
	{
	public:
		// Retrieve pointer to the GroupController
		static GroupController* Instance(void);

		GroupController();
		~GroupController();

		// Find a Group with the specified name.
		Group* FindGroup(const char* groupName);

		// Add a GameObject to the specified Group.
		bool AddToGroup(const char* groupName, GameObject* object);

		// Remove a GameObject from the specified Group,
		void RemoveFromGroup(const char* groupName, GameObject* object);

		// Send a Group a custom event.
		void SendGroupCustomEvent(const char* groupName, GameObject* from,
			int command, int data);

	protected:
		// Create a Group with the specified name.
		Group* FindOrCreateGroup(const char* groupName);

	private:
		// Instance pointer to GroupController
		static GroupController* _mInstance;

		// List of groups
		SList<Group> mGroups;
	};

#endif // _GROUPCONTROL_H_
