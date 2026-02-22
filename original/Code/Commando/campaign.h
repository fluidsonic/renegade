#pragma once

#include "global.h"


class	ChunkSaveClass;
class	ChunkLoadClass;

/*
** CampaignManager is responsible for controlling the flow of missions and sub-states through
** the playing of a campaign.  It also is the main entry point for single mission, and multi-play sessions.
*/
class	CampaignManager {

public:
	static	void	Init( void );
	static	void	Shutdown( void );

	static	bool	Save(ChunkSaveClass & csave);
	static	bool	Load(ChunkLoadClass &cload);

	static	void	Start_Campaign( int difficulty );
	static	void	Continue( bool success = true );

	static	void	Reset();

	// Backdrop Descriptions
	static	int	Get_Backdrop_Description_Count( void );
	static	const char * Get_Backdrop_Description( int index );

	static	void	Select_Backdrop_Number( int state_number );
	static	void	Select_Backdrop_Number_By_MP_Type( int type );

	static	void	Replay_Level( const char * mission_name, int difficulty );

private:
	static	int	State;
	static	int	BackdropIndex;
};
