//
// Filename:     god.h
// Author:       Tom Spencer-Smith
// Date:         Dec 1998
// Description:  
//
//-----------------------------------------------------------------------------

#ifndef GOD_H
#define GOD_H

#include "inventory.h"

class SoldierGameObj;
class cPlayer;
class Vector3;
class ChunkSaveClass;
class ChunkLoadClass;
class WideStringClass;

//-----------------------------------------------------------------------------
class cGod
{
	public:
		static void					Think(void);
		static cPlayer *			Create_Player(int client_id, const WideStringClass & name,
														int team_choice, bool is_invulnerable = false);

		static void					Create_Ai_Player(void);
		static void					Create_Grunt(Vector3 & pos);

		static void					Reset( void );
		static void					Star_Killed( void );
		static void					Respawn( void );
		static void					Restart( void );
		static void					Load_Game( void );
		static void					Mission_Failed( void );
		static void					Exit( void );

		static	bool				Save(ChunkSaveClass & csave);
		static	bool				Load(ChunkLoadClass & cload);

		static	void				Store_Inventory( SoldierGameObj * );
		static	void				Restore_Inventory( SoldierGameObj * );
		static	void				Reset_Inventory( void );

	private:
		static SoldierGameObj * Create_Commando(int client_id, int player_type);
		static SoldierGameObj * Create_Commando(cPlayer * p_player);

		static	int				State;

		static	InventoryClass	LevelStartInventory;
};

//-----------------------------------------------------------------------------
#endif // GOD_H

