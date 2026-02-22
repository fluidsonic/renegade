#pragma once

#include "global.h"

#include "building.h"

////////////////////////////////////////////////////////////////
//	Forward delcarations
////////////////////////////////////////////////////////////////
class BaseControllerClass;
class VehicleGameObj;
class SoldierGameObj;

////////////////////////////////////////////////////////////////
//
//	VehicleFactoryGameObjDef
//
////////////////////////////////////////////////////////////////
class VehicleFactoryGameObjDef : public BuildingGameObjDef
{
public:
	
	////////////////////////////////////////////////////////////////
	//	Friends
	////////////////////////////////////////////////////////////////
	friend class VehicleFactoryGameObj;

	////////////////////////////////////////////////////////////////
	//	Public constructors/destructors
	////////////////////////////////////////////////////////////////
	VehicleFactoryGameObjDef (void);
	~VehicleFactoryGameObjDef (void);

	////////////////////////////////////////////////////////////////
	//	Public methods
	////////////////////////////////////////////////////////////////
	uint32_t								Get_Class_ID (void) const;
	PersistClass *						Create (void) const;
	bool									Save (ChunkSaveClass &csave);
	bool									Load (ChunkLoadClass &cload);
	const PersistFactoryClass &	Get_Factory (void) const;

	int									Get_Pad_Clearing_Warhead(void) const { return PadClearingWarhead; }
	float									Get_Total_Building_Time(void) const { return TotalBuildingTime; }

	////////////////////////////////////////////////////////////////
	//	Editable support
	////////////////////////////////////////////////////////////////
	DECLARE_EDITABLE (VehicleFactoryGameObjDef, BuildingGameObjDef);

protected:

	////////////////////////////////////////////////////////////////
	//	Protected methods
	////////////////////////////////////////////////////////////////
	void					Load_Variables (ChunkLoadClass &cload);

	////////////////////////////////////////////////////////////////
	//	Protected member data
	////////////////////////////////////////////////////////////////
	int					PadClearingWarhead;  // warhead used to destroy objects blocking construction area
	float					TotalBuildingTime;	// total time for slowest vehicle to be constructed and driven out.
};

////////////////////////////////////////////////////////////////
//
//	VehicleFactoryGameObj
//
////////////////////////////////////////////////////////////////
class VehicleFactoryGameObj : public BuildingGameObj
{
public:

	////////////////////////////////////////////////////////////////
	//	Public constructors/destructors
	////////////////////////////////////////////////////////////////
	VehicleFactoryGameObj (void);
	~VehicleFactoryGameObj (void);

	////////////////////////////////////////////////////////////////
	//	Public methods
	////////////////////////////////////////////////////////////////

	//
	// Definition support
	//
	virtual	void							Init( void );
	void										Init (const VehicleFactoryGameObjDef & definition);
	const VehicleFactoryGameObjDef &	Get_Definition (void) const;

	//
	// RTTI
	//
	VehicleFactoryGameObj *			As_VehicleFactoryGameObj (void)	{ return this; }

	//
	// Persist support
	//
	bool									Save (ChunkSaveClass &csave);
	bool									Load (ChunkLoadClass &cload);
	const	PersistFactoryClass &	Get_Factory (void) const;

	//
	//	From BuildingGameObj
	//
	void					CnC_Initialize (BaseControllerClass *base);
	void					On_Destroyed (void);
	
	//
	//	GameObj methods
	//
	void					Think (void);

	//
	//	Factory state access
	//
	bool					Is_Available (void) const	{ return (IsBusy == false) && (IsDestroyed == false); }
	bool					Is_Available_For_Purchase (void) const;
	bool					Is_Busy (void) const			{ return IsBusy; }
	int					Get_Team_Vehicle_Count(void) const; 

	//
	//	Factory access
	//
	bool					Request_Vehicle (int defintion_id, float generation_time,SoldierGameObj * player = NULL);

	//
	//	Creation transform access
	//
	const Matrix3D &	Get_Creation_TM (void)						{ return CreationTM; }
	void					Set_Creation_TM (const Matrix3D &tm)	{ CreationTM = tm; }

	//
	//	Network object support
	//
	void					Import_Rare (BitStreamClass &packet);
	void					Export_Rare (BitStreamClass &packet);

	//
	// Vehicle limits
	// 
	static void			Set_Max_Vehicles_Per_Team(int max)		{ assert(max > 0); MaxVehiclesPerTeam = max; }
	static int			Get_Max_Vehicles_Per_Team(void)			{ return MaxVehiclesPerTeam; }

protected:

	////////////////////////////////////////////////////////////////
	//	Protected methods
	////////////////////////////////////////////////////////////////
	void					Load_Variables (ChunkLoadClass &cload);
	virtual void		Destroy_Blocking_Objects (void);
	void					Deliver_Vehicle(void);
	VehicleGameObj *	Create_Vehicle (void);
	virtual void		Begin_Generation (void)	{};
	virtual void		On_Generation_Complete (void);

	////////////////////////////////////////////////////////////////
	//	Protected member data
	////////////////////////////////////////////////////////////////
	GameObjReference	Vehicle;
	Matrix3D				CreationTM;
	OBBoxClass			GeneratingRegion;
	float					GenerationTime;
	int					GeneratingVehicleID;
	bool					IsBusy;
	GameObjReference	Purchaser;
	int					LastDeliveryPath;
	float					EndTimer;

	static int			MaxVehiclesPerTeam;
};
