#include "global.h"
#include "scripts.h"
#include "groupcontrol.h"
#include "group.h"
#include "customevents.h"
#include "dprint.h"

DECLARE_SCRIPT(MXX_Group_Member_DEL, "GroupName:string")
	{
	const char* mGroupName;

	// Add the object to the group when it is created.
	void Created(GameObject* owner)
		{
		mGroupName = Get_Parameter("GroupName");
		assert(mGroupName != NULL);

		GroupController* controller = GroupController::Instance();
		assert(controller != NULL);
		controller->AddToGroup(mGroupName, owner);
		}

	
	// Remove the object from the group when it is destroyed.
	void Destroyed(GameObject* owner)
		{
		GroupController* controller = GroupController::Instance();
		assert(controller != NULL);
		controller->RemoveFromGroup(mGroupName, owner);
		}

	// Notify group that a member was killed.
	void Killed(GameObject* owner, GameObject* killer)
		{
		GroupController* controller = GroupController::Instance();
		assert(controller != NULL);

		GroupEventInfo info;
		info.GroupName = mGroupName;
		info.Event = GROUP_MEMBER_KILLED;
		info.Object = killer;

		Group* group = controller->FindGroup(mGroupName);
		assert(group != NULL);
		group->SendCustomEvent(owner, SCMD_GROUP_EVENT, (int)&info);
		}

	
	// Notify group that a member was damaged.
	void Damaged(GameObject* owner, GameObject* damager)
		{
		GroupController* controller = GroupController::Instance();
		assert(controller != NULL);

		GroupEventInfo info;
		info.GroupName = mGroupName;
		info.Event = GROUP_MEMBER_DAMAGED;
		info.Object = damager;

		Group* group = controller->FindGroup(mGroupName);
		assert(group != NULL);
		group->SendCustomEvent(owner, SCMD_GROUP_EVENT, (int)&info);
		}

	// Notify group that a member heard a sound.
	void Sound_Heard(GameObject* owner, const CombatSound& sound)
		{
		GroupController* controller = GroupController::Instance();
		assert(controller != NULL);

		GroupEventInfo info;
		info.GroupName = mGroupName;
		info.Event = GROUP_MEMBER_HEARD;
		info.Sound = &sound;

		Group* group = controller->FindGroup(mGroupName);
		assert(group != NULL);
		group->SendCustomEvent(owner, SCMD_GROUP_EVENT, (int)&info);
		}

	// Notify group that a member saw the enemy.
	void Enemy_Seen(GameObject* owner, GameObject* enemy)
		{
		GroupController* controller = GroupController::Instance();
		assert(controller != NULL);

		GroupEventInfo info;
		info.GroupName = mGroupName;
		info.Event = GROUP_MEMBER_SAW;
		info.Object = enemy;

		Group* group = controller->FindGroup(mGroupName);
		assert(group != NULL);
		group->SendCustomEvent(owner, SCMD_GROUP_EVENT, (int)&info);
		}

	};
