#ifndef TEAM_H
#define TEAM_H

#include "vector3.h"
#include "soldier.h"
#include "widestring.h"

const int MAX_TEAMNAME_SIZE = 10; //including NULL

class cPacket;

//------------------------------------------------------------------------------------
//
// Holds data about a team
//
class cTeam : public NetworkObjectClass {

   public:
      friend class cTeamManager; // so that only cTeamManager can call ~cTeam

		cTeam(void);

		virtual uint32_t		Get_Network_Class_ID(void) const					{return NETCLASSID_TEAM;}
		virtual void		Delete(void)											{delete this;}

		//
      // Server only
      //
      void					Increment_Kills(void);
      void					Increment_Deaths(void);
      void					Increment_Score(float increment);
      //void					Increment_Money(int increment);

      //
      // Client only
      //
      void					Set_Kills(int new_kills);
      void					Set_Deaths(int new_deaths);
      void					Set_Score(float new_score);
      //void					Set_Money(int new_money);

      //
      // Client or server
      //
	   void					Init(int team_number);
		void					Init_Team_Name(void);
      WideStringClass	Get_Name(void) const									{return Name;}
      int					Get_Id(void) const									{return TeamNumber;}
      void					Reset(void);
      int					Tally_Size(void) const;
      int					Get_Kills(void) const								{return Kills;}
      int					Get_Deaths(void) const								{return Deaths;}
      float					Get_Score(void) const								{return Score;}
      //int					Get_Money(void) const								{return Money;}
      float					Get_Kill_To_Death_Ratio(void) const;
      void					Get_Team_String(int rank, WideStringClass & string) const;
		Vector3				Get_Color(void) const;

      int					Tally_Money(void) const;

		//
		//	Server-to-client data importing/exporting
		//
		virtual void		Import_Creation(BitStreamClass &packet);
		virtual void		Import_Rare(BitStreamClass &packet);
		virtual void		Import_Occasional(BitStreamClass &packet);
		virtual void		Import_Frequent(BitStreamClass &packet);

		virtual void		Export_Creation(BitStreamClass &packet);
		virtual void		Export_Rare(BitStreamClass &packet);
		virtual void		Export_Occasional(BitStreamClass &packet);
		virtual void		Export_Frequent(BitStreamClass &packet);

   private:
	   ~cTeam(void); // only the cTeamManager can destroy...

      WideStringClass	Name;
      int					Kills;
      int					Deaths;
      float					Score;
      int					Money;
      int					TeamNumber;
};

#endif	//	TEAM_H

