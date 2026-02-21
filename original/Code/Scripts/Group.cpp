#include "group.h"

/******************************************************************************
*
* NAME
*     Group::Group
*
* DESCRIPTION
*     Group Constructor
*
* INPUTS
*     Name - Name of team.
*
* RESULTS
*     NONE
*
******************************************************************************/

Group::Group(const char* name)
	{
	mName[0] = '\0';

	// Make copy of team name
	assert(name != NULL);

	if (name != NULL)
		{
		strncpy(mName, name, sizeof(mName));
		mName[sizeof(mName) - 1] = '\0';
		}
	}

/******************************************************************************
*
* NAME
*     Group::~Group
*
* DESCRIPTION
*     Group Destructor
*
* INPUTS
*     NONE
*
* RESULTS
*     NONE
*
******************************************************************************/

Group::~Group()
	{
	}

/******************************************************************************
*
* NAME
*     Group::GetName
*
* DESCRIPTION
*     Retrieve name of Group.
*
* INPUTS
*     NONE
*
* RESULTS
*     Name - Name of team
*
******************************************************************************/

const char* Group::GetName(void) const
	{
	return mName;
	}

/******************************************************************************
*
* NAME
*     Group::AddMember
*
* DESCRIPTION
*     Add a game object to the Group.
*
* INPUTS
*     GameObject - Pointer to game object to add.
*
* RESULTS
*     NONE
*
******************************************************************************/

void Group::AddMember(GameObject* object)
	{
	assert(object != NULL);

	// Add the object if it is not already a member of the team.
	if (IsMember(object) == false)
		{
		mMembers.Add(object);
		}
	}

/******************************************************************************
*
* NAME
*     Group::RemoveMember
*
* DESCRIPTION
*     Remove a game object from the Group.
*
* INPUTS
*     GameObject - Pointer to game object to remove.
*
* RESULTS
*     NONE
*
******************************************************************************/

void Group::RemoveMember(GameObject* object)
	{
	assert(object != NULL);
	mMembers.Delete(object);
	}

/******************************************************************************
*
* NAME
*     Group::IsMember
*
* DESCRIPTION
*     Check if a game object is member of the Group.
*
* INPUTS
*     GameObjectID objectID
*
* RESULTS
*     bool
*
******************************************************************************/

bool Group::IsMember(GameObject* object) const
	{
	assert(object != NULL);

	for (int index = 0; index < mMembers.Count(); index++)
		{
		if (mMembers[index] == object)
			{
			return true;
			}
		}

	return false;
	}

/******************************************************************************
*
* NAME
*     MemberCount
*
* DESCRIPTION
*     Get the number of members in the Group.
*
* INPUTS
*     NONE
*
* RESULTS
*     Count - Member count
*
******************************************************************************/

int Group::MemberCount(void) const
	{
	return mMembers.Count();
	}

/******************************************************************************
*
* NAME
*     Group::GetMember
*
* DESCRIPTION
*     Get a Group member.
*
* INPUTS
*     int index
*
* RESULTS
*     GameObject*
*
******************************************************************************/

GameObject* Group::GetMember(int index)
	{
	assert(index >= 0);
	assert(index < MemberCount());

	return mMembers[index];
	}

/******************************************************************************
*
* NAME
*     Group::SendCommand
*
* DESCRIPTION
*     Send a command to all the members of the Group.
*
* INPUTS
*     Command - Command to send to Group members.
*     Data    - Command specific data parameter.
*
* RESULTS
*     NONE
*
******************************************************************************/

void Group::SendCustomEvent(GameObject* from, int event, int data)
	{
	for (int index = 0; index < mMembers.Count(); index++)
		{
		GameObject* object = mMembers[index];
		assert(object != NULL);
		Commands->Send_Custom_Event(from, object, event, data);
		}
	}
