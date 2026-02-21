
#ifndef __WAYPATHPOSITION_H
#define __WAYPATHPOSITION_H


////////////////////////////////////////////////////////////////
//
//	WaypathPositionClass
//
////////////////////////////////////////////////////////////////
class WaypathPositionClass
{
public:

	////////////////////////////////////////////////////////////////
	//	Public constructors/destructors
	////////////////////////////////////////////////////////////////
	WaypathPositionClass (void)
		:	WaypathID (0),
			WaypointIndex (0),
			Percent (0)					{}
	~WaypathPositionClass (void)	{}

	////////////////////////////////////////////////////////////////
	//	Public methods
	////////////////////////////////////////////////////////////////
	
	//
	//	Accessors
	//
	void			Set_Waypath_ID (int id)				{ WaypathID = id; }
	int			Get_Waypath_ID (void) const		{ return WaypathID; }

	void			Set_Waypoint_Index (int index)	{ WaypointIndex = index; }
	int			Get_Waypoint_Index (void) const	{ return WaypointIndex; }

	void			Set_Percent (float percent)		{ Percent = percent; }
	float			Get_Percent (void) const			{ return Percent; }

private:

	////////////////////////////////////////////////////////////////
	//	Private member data
	////////////////////////////////////////////////////////////////
	int		WaypathID;
	int		WaypointIndex;
	float		Percent;
};


#endif //__WAYPATHPOSITION_H
