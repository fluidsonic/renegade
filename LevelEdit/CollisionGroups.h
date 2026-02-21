#if defined(_MSC_VER)
#pragma once
#endif


#ifndef __COLLISION_GROUPS_H
#define __COLLISION_GROUPS_H


typedef enum {
	DEF_COLLISION_GROUP				= 10,		// collides with itself and mouse clicks
	EDITOR_COLLISION_GROUP,						// doesn't collide with anything (other than mouse clicks)
	MOUSE_CLICK_COLLISION_GROUP,				// collides with everything
	GAME_COLLISION_GROUP,						// collides with everything except for EDITOR_COLLISION_GROUP
	STATIC_OBJ_COLLISION_GROUP					// only collides with static objects (group 15)
} COLLISION_GROUPS;


#endif //__COLLISION_GROUPS_H

