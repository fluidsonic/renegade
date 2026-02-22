#pragma once

#include "global.h"

#include "scripts.h"
#include "vector.h"

// Group event identifiers
typedef enum
	{
	GROUP_MEMBER_DAMAGED = 1,
	GROUP_MEMBER_KILLED,
	GROUP_MEMBER_HEARD,
	GROUP_MEMBER_SAW,
	} GroupEvent;

// Group event information.
//
// A pointer to this structure is sent as a data parameter when the custom
// event SCMD_GROUP_EVENT is sent to an object.
typedef struct GroupEventInfoTag
	{
	const char* GroupName;
	GroupEvent Event;

	union
		{
		GameObject* Object;
		const CombatSound* Sound;
		};
	} GroupEventInfo;

class GroupController;

class Group
	{
	public:
		// Retrieve Team name
		const char* GetName(void) const;

		// Add a GameObject to the team.
		void AddMember(GameObject* object);

		// Remove a GameObject from the team.
		void RemoveMember(GameObject* object);

		// Check if a GameObject is a member of the team.
		bool IsMember(GameObject* object) const;

		// Get the number of members in the team.
		int MemberCount(void) const;

		// Get a team member.
		GameObject* GetMember(int index);

		// Send a command to all the members of the team.
		void SendCustomEvent(GameObject* from, int event, int data);

	protected:
		// Only the TeamController can create and delete Teams
		friend class GroupController;
		Group(const char* name);
		~Group();

	private:
		char mName[64];
		DynamicVectorClass<GameObject*> mMembers;
	};
