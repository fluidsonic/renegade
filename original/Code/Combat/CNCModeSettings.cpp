#include "global.h"
#include "CNCModeSettings.h"
#include "CombatChunkID.h"
#include "PersistFactory.h"
#include "DefinitionFactory.h"
#include "SimpleDefinitionFactory.h"
#include "PlayerType.h"
CNCModeSettingsDef* CNCModeSettingsDef::_mInstance = NULL;

// Factories
SimplePersistFactoryClass<CNCModeSettingsDef, CHUNKID_GLOBAL_SETTINGS_DEF_CNCMODE> _CNCModeSettingsDefPersistFactory;
DECLARE_DEFINITION_FACTORY(CNCModeSettingsDef, CLASSID_GLOBAL_SETTINGS_DEF_CNCMODE, "C&C Mode Settings") _CNCModeSettingsDefDefFactory;

CNCModeSettingsDef::CNCModeSettingsDef(void) :
		AnnouncementInterval(30)
	{
	//assert(_mInstance == NULL);
	_mInstance = this;

	for (int team = 0; team < NUM_TEAMS; ++team)
		{
		mPowerOfflineID[team] = 0;
		mPurchaseCanceledID[team] = 0;
		mInsufficientFundsID[team] = 0;
		mConstructingID[team] = 0;
		mUnitReadyID[team] = 0;
		mIonBeaconDeployedID[team] = 0;
		mIonBeaconDisarmedID[team] = 0;
		mIonBeaconWarningID[team] = 0;
		mNukeBeaconDeployedID[team] = 0;
		mNukeBeaconDisarmedID[team] = 0;
		mNukeBeaconWarningID[team] = 0;
		}

	memset(mRadioCmds, 0, sizeof(mRadioCmds));

	
	}

CNCModeSettingsDef::~CNCModeSettingsDef(void)
	{
	_mInstance = NULL;
	}

uint32_t CNCModeSettingsDef::Get_Class_ID(void) const
	{ 
	return CLASSID_GLOBAL_SETTINGS_DEF_CNCMODE; 
	}

const PersistFactoryClass& CNCModeSettingsDef::Get_Factory(void) const
	{ 
	return _CNCModeSettingsDefPersistFactory; 
	}

PersistClass* CNCModeSettingsDef::Create(void) const
	{
	return NULL;
	}

// Save/load constants
enum
	{
	CHUNKID_PARENT =	803001812,
	CHUNKID_VARIABLES,

	VARID_DEF_ANNOUNCEMENT_INTERVAL = 1,
	VARID_DEF_NOD_POWER_OFFLINE_ID,
	VARID_DEF_GDI_POWER_OFFLINE_ID,
	VARID_DEF_NOD_PURCHASE_CANCELED_ID,
	VARID_DEF_GDI_PURCHASE_CANCELED_ID,
	VARID_DEF_NOD_INSUFFICIENT_FUNDS_ID,
	VARID_DEF_GDI_INSUFFICIENT_FUNDS_ID,
	VARID_DEF_NOD_UNIT_READY_ID,
	VARID_DEF_GDI_UNIT_READY_ID,

	VARID_DEF_RADIO_CMD_01,
	VARID_DEF_RADIO_CMD_02,
	VARID_DEF_RADIO_CMD_03,
	VARID_DEF_RADIO_CMD_04,
	VARID_DEF_RADIO_CMD_05,
	VARID_DEF_RADIO_CMD_06,
	VARID_DEF_RADIO_CMD_07,
	VARID_DEF_RADIO_CMD_08,
	VARID_DEF_RADIO_CMD_09,
	VARID_DEF_RADIO_CMD_10,
	VARID_DEF_RADIO_CMD_11,
	VARID_DEF_RADIO_CMD_12,
	VARID_DEF_RADIO_CMD_13,
	VARID_DEF_RADIO_CMD_14,
	VARID_DEF_RADIO_CMD_15,
	VARID_DEF_RADIO_CMD_16,
	VARID_DEF_RADIO_CMD_17,
	VARID_DEF_RADIO_CMD_18,
	VARID_DEF_RADIO_CMD_19,
	VARID_DEF_RADIO_CMD_20,
	VARID_DEF_RADIO_CMD_21,
	VARID_DEF_RADIO_CMD_22,
	VARID_DEF_RADIO_CMD_23,
	VARID_DEF_RADIO_CMD_24,
	VARID_DEF_RADIO_CMD_25,
	VARID_DEF_RADIO_CMD_26,
	VARID_DEF_RADIO_CMD_27,
	VARID_DEF_RADIO_CMD_28,
	VARID_DEF_RADIO_CMD_29,
	VARID_DEF_RADIO_CMD_30,

	VARID_DEF_NOD_ION_BEACON_DEPLOYED_ID,
	VARID_DEF_GDI_ION_BEACON_DEPLOYED_ID,
	VARID_DEF_NOD_ION_BEACON_DISARMED_ID,
	VARID_DEF_GDI_ION_BEACON_DISARMED_ID,
	VARID_DEF_NOD_ION_BEACON_WARNING_ID,
	VARID_DEF_GDI_ION_BEACON_WARNING_ID,

	VARID_DEF_NOD_NUKE_BEACON_DEPLOYED_ID,
	VARID_DEF_GDI_NUKE_BEACON_DEPLOYED_ID,
	VARID_DEF_NOD_NUKE_BEACON_DISARMED_ID,
	VARID_DEF_GDI_NUKE_BEACON_DISARMED_ID,
	VARID_DEF_NOD_NUKE_BEACON_WARNING_ID,
	VARID_DEF_GDI_NUKE_BEACON_WARNING_ID,

	VARID_DEF_RADIO_ICON_01,
	VARID_DEF_RADIO_ICON_02,
	VARID_DEF_RADIO_ICON_03,
	VARID_DEF_RADIO_ICON_04,
	VARID_DEF_RADIO_ICON_05,
	VARID_DEF_RADIO_ICON_06,
	VARID_DEF_RADIO_ICON_07,
	VARID_DEF_RADIO_ICON_08,
	VARID_DEF_RADIO_ICON_09,
	VARID_DEF_RADIO_ICON_10,
	VARID_DEF_RADIO_ICON_11,
	VARID_DEF_RADIO_ICON_12,
	VARID_DEF_RADIO_ICON_13,
	VARID_DEF_RADIO_ICON_14,
	VARID_DEF_RADIO_ICON_15,
	VARID_DEF_RADIO_ICON_16,
	VARID_DEF_RADIO_ICON_17,
	VARID_DEF_RADIO_ICON_18,
	VARID_DEF_RADIO_ICON_19,
	VARID_DEF_RADIO_ICON_20,
	VARID_DEF_RADIO_ICON_21,
	VARID_DEF_RADIO_ICON_22,
	VARID_DEF_RADIO_ICON_23,
	VARID_DEF_RADIO_ICON_24,
	VARID_DEF_RADIO_ICON_25,
	VARID_DEF_RADIO_ICON_26,
	VARID_DEF_RADIO_ICON_27,
	VARID_DEF_RADIO_ICON_28,
	VARID_DEF_RADIO_ICON_29,
	VARID_DEF_RADIO_ICON_30,

	};

bool CNCModeSettingsDef::Save(ChunkSaveClass& csave)
	{
	csave.Begin_Chunk(CHUNKID_PARENT);
		DefinitionClass::Save(csave);
	csave.End_Chunk();

	csave.Begin_Chunk(CHUNKID_VARIABLES);
		WRITE_MICRO_CHUNK(csave, VARID_DEF_ANNOUNCEMENT_INTERVAL, AnnouncementInterval);
		
		WRITE_MICRO_CHUNK(csave, VARID_DEF_NOD_POWER_OFFLINE_ID, mPowerOfflineID[0]);
		WRITE_MICRO_CHUNK(csave, VARID_DEF_GDI_POWER_OFFLINE_ID, mPowerOfflineID[1]);
		
		WRITE_MICRO_CHUNK(csave, VARID_DEF_NOD_PURCHASE_CANCELED_ID, mPurchaseCanceledID[0]);
		WRITE_MICRO_CHUNK(csave, VARID_DEF_GDI_PURCHASE_CANCELED_ID, mPurchaseCanceledID[1]);
		
		WRITE_MICRO_CHUNK(csave, VARID_DEF_NOD_INSUFFICIENT_FUNDS_ID, mInsufficientFundsID[0]);
		WRITE_MICRO_CHUNK(csave, VARID_DEF_GDI_INSUFFICIENT_FUNDS_ID, mInsufficientFundsID[1]);

		WRITE_MICRO_CHUNK(csave, VARID_DEF_NOD_UNIT_READY_ID, mUnitReadyID[0]);
		WRITE_MICRO_CHUNK(csave, VARID_DEF_GDI_UNIT_READY_ID, mUnitReadyID[1]);

		for (int radioIndex = 0; radioIndex < 30; ++radioIndex)
			{
			WRITE_MICRO_CHUNK(csave, VARID_DEF_RADIO_CMD_01 + radioIndex, mRadioCmds[radioIndex]);
			WRITE_MICRO_CHUNK_WWSTRING(csave, VARID_DEF_RADIO_ICON_01 + radioIndex, mRadioCmdIcons[radioIndex]);			
			}

		WRITE_MICRO_CHUNK(csave, VARID_DEF_NOD_ION_BEACON_DEPLOYED_ID, mIonBeaconDeployedID[0]);
		WRITE_MICRO_CHUNK(csave, VARID_DEF_GDI_ION_BEACON_DEPLOYED_ID, mIonBeaconDeployedID[1]);
		WRITE_MICRO_CHUNK(csave, VARID_DEF_NOD_ION_BEACON_DISARMED_ID, mIonBeaconDisarmedID[0]);
		WRITE_MICRO_CHUNK(csave, VARID_DEF_GDI_ION_BEACON_DISARMED_ID, mIonBeaconDisarmedID[1]);
		WRITE_MICRO_CHUNK(csave, VARID_DEF_NOD_ION_BEACON_WARNING_ID, mIonBeaconWarningID[0]);
		WRITE_MICRO_CHUNK(csave, VARID_DEF_GDI_ION_BEACON_WARNING_ID, mIonBeaconWarningID[1]);

		WRITE_MICRO_CHUNK(csave, VARID_DEF_NOD_NUKE_BEACON_DEPLOYED_ID, mNukeBeaconDeployedID[0]);
		WRITE_MICRO_CHUNK(csave, VARID_DEF_GDI_NUKE_BEACON_DEPLOYED_ID, mNukeBeaconDeployedID[1]);
		WRITE_MICRO_CHUNK(csave, VARID_DEF_NOD_NUKE_BEACON_DISARMED_ID, mNukeBeaconDisarmedID[0]);
		WRITE_MICRO_CHUNK(csave, VARID_DEF_GDI_NUKE_BEACON_DISARMED_ID, mNukeBeaconDisarmedID[1]);
		WRITE_MICRO_CHUNK(csave, VARID_DEF_NOD_NUKE_BEACON_WARNING_ID, mNukeBeaconWarningID[0]);
		WRITE_MICRO_CHUNK(csave, VARID_DEF_GDI_NUKE_BEACON_WARNING_ID, mNukeBeaconWarningID[1]);

	csave.End_Chunk();

	return true;
	}

bool CNCModeSettingsDef::Load(ChunkLoadClass& cload)
	{
	while (cload.Open_Chunk())
		{
		switch (cload.Cur_Chunk_ID())
			{
			case CHUNKID_PARENT:
				DefinitionClass::Load(cload);
				break;
								
			case CHUNKID_VARIABLES:
				while (cload.Open_Micro_Chunk())
					{
					switch (cload.Cur_Micro_Chunk_ID())
						{
						READ_MICRO_CHUNK(cload, VARID_DEF_ANNOUNCEMENT_INTERVAL, AnnouncementInterval);

						READ_MICRO_CHUNK(cload, VARID_DEF_NOD_POWER_OFFLINE_ID, mPowerOfflineID[0]);
						READ_MICRO_CHUNK(cload, VARID_DEF_GDI_POWER_OFFLINE_ID, mPowerOfflineID[1]);
						READ_MICRO_CHUNK(cload, VARID_DEF_NOD_PURCHASE_CANCELED_ID, mPurchaseCanceledID[0]);
						READ_MICRO_CHUNK(cload, VARID_DEF_GDI_PURCHASE_CANCELED_ID, mPurchaseCanceledID[1]);
						READ_MICRO_CHUNK(cload, VARID_DEF_NOD_INSUFFICIENT_FUNDS_ID, mInsufficientFundsID[0]);
						READ_MICRO_CHUNK(cload, VARID_DEF_GDI_INSUFFICIENT_FUNDS_ID, mInsufficientFundsID[1]);
						READ_MICRO_CHUNK(cload, VARID_DEF_NOD_UNIT_READY_ID, mUnitReadyID[0]);
						READ_MICRO_CHUNK(cload, VARID_DEF_GDI_UNIT_READY_ID, mUnitReadyID[1]);

						READ_MICRO_CHUNK(cload, VARID_DEF_RADIO_CMD_01, mRadioCmds[0]);
						READ_MICRO_CHUNK(cload, VARID_DEF_RADIO_CMD_02, mRadioCmds[1]);
						READ_MICRO_CHUNK(cload, VARID_DEF_RADIO_CMD_03, mRadioCmds[2]);
						READ_MICRO_CHUNK(cload, VARID_DEF_RADIO_CMD_04, mRadioCmds[3]);
						READ_MICRO_CHUNK(cload, VARID_DEF_RADIO_CMD_05, mRadioCmds[4]);
						READ_MICRO_CHUNK(cload, VARID_DEF_RADIO_CMD_06, mRadioCmds[5]);
						READ_MICRO_CHUNK(cload, VARID_DEF_RADIO_CMD_07, mRadioCmds[6]);
						READ_MICRO_CHUNK(cload, VARID_DEF_RADIO_CMD_08, mRadioCmds[7]);
						READ_MICRO_CHUNK(cload, VARID_DEF_RADIO_CMD_09, mRadioCmds[8]);
						READ_MICRO_CHUNK(cload, VARID_DEF_RADIO_CMD_10, mRadioCmds[9]);
						READ_MICRO_CHUNK(cload, VARID_DEF_RADIO_CMD_11, mRadioCmds[10]);
						READ_MICRO_CHUNK(cload, VARID_DEF_RADIO_CMD_12, mRadioCmds[11]);
						READ_MICRO_CHUNK(cload, VARID_DEF_RADIO_CMD_13, mRadioCmds[12]);
						READ_MICRO_CHUNK(cload, VARID_DEF_RADIO_CMD_14, mRadioCmds[13]);
						READ_MICRO_CHUNK(cload, VARID_DEF_RADIO_CMD_15, mRadioCmds[14]);
						READ_MICRO_CHUNK(cload, VARID_DEF_RADIO_CMD_16, mRadioCmds[15]);
						READ_MICRO_CHUNK(cload, VARID_DEF_RADIO_CMD_17, mRadioCmds[16]);
						READ_MICRO_CHUNK(cload, VARID_DEF_RADIO_CMD_18, mRadioCmds[17]);
						READ_MICRO_CHUNK(cload, VARID_DEF_RADIO_CMD_19, mRadioCmds[18]);
						READ_MICRO_CHUNK(cload, VARID_DEF_RADIO_CMD_20, mRadioCmds[19]);
						READ_MICRO_CHUNK(cload, VARID_DEF_RADIO_CMD_21, mRadioCmds[20]);
						READ_MICRO_CHUNK(cload, VARID_DEF_RADIO_CMD_22, mRadioCmds[21]);
						READ_MICRO_CHUNK(cload, VARID_DEF_RADIO_CMD_23, mRadioCmds[22]);
						READ_MICRO_CHUNK(cload, VARID_DEF_RADIO_CMD_24, mRadioCmds[23]);
						READ_MICRO_CHUNK(cload, VARID_DEF_RADIO_CMD_25, mRadioCmds[24]);
						READ_MICRO_CHUNK(cload, VARID_DEF_RADIO_CMD_26, mRadioCmds[25]);
						READ_MICRO_CHUNK(cload, VARID_DEF_RADIO_CMD_27, mRadioCmds[26]);
						READ_MICRO_CHUNK(cload, VARID_DEF_RADIO_CMD_28, mRadioCmds[27]);
						READ_MICRO_CHUNK(cload, VARID_DEF_RADIO_CMD_29, mRadioCmds[28]);
						READ_MICRO_CHUNK(cload, VARID_DEF_RADIO_CMD_30, mRadioCmds[29]);

						READ_MICRO_CHUNK_WWSTRING(cload, VARID_DEF_RADIO_ICON_01, mRadioCmdIcons[0]);
						READ_MICRO_CHUNK_WWSTRING(cload, VARID_DEF_RADIO_ICON_02, mRadioCmdIcons[1]);
						READ_MICRO_CHUNK_WWSTRING(cload, VARID_DEF_RADIO_ICON_03, mRadioCmdIcons[2]);
						READ_MICRO_CHUNK_WWSTRING(cload, VARID_DEF_RADIO_ICON_04, mRadioCmdIcons[3]);
						READ_MICRO_CHUNK_WWSTRING(cload, VARID_DEF_RADIO_ICON_05, mRadioCmdIcons[4]);
						READ_MICRO_CHUNK_WWSTRING(cload, VARID_DEF_RADIO_ICON_06, mRadioCmdIcons[5]);
						READ_MICRO_CHUNK_WWSTRING(cload, VARID_DEF_RADIO_ICON_07, mRadioCmdIcons[6]);
						READ_MICRO_CHUNK_WWSTRING(cload, VARID_DEF_RADIO_ICON_08, mRadioCmdIcons[7]);
						READ_MICRO_CHUNK_WWSTRING(cload, VARID_DEF_RADIO_ICON_09, mRadioCmdIcons[8]);
						READ_MICRO_CHUNK_WWSTRING(cload, VARID_DEF_RADIO_ICON_10, mRadioCmdIcons[9]);
						READ_MICRO_CHUNK_WWSTRING(cload, VARID_DEF_RADIO_ICON_11, mRadioCmdIcons[10]);
						READ_MICRO_CHUNK_WWSTRING(cload, VARID_DEF_RADIO_ICON_12, mRadioCmdIcons[11]);
						READ_MICRO_CHUNK_WWSTRING(cload, VARID_DEF_RADIO_ICON_13, mRadioCmdIcons[12]);
						READ_MICRO_CHUNK_WWSTRING(cload, VARID_DEF_RADIO_ICON_14, mRadioCmdIcons[13]);
						READ_MICRO_CHUNK_WWSTRING(cload, VARID_DEF_RADIO_ICON_15, mRadioCmdIcons[14]);
						READ_MICRO_CHUNK_WWSTRING(cload, VARID_DEF_RADIO_ICON_16, mRadioCmdIcons[15]);
						READ_MICRO_CHUNK_WWSTRING(cload, VARID_DEF_RADIO_ICON_17, mRadioCmdIcons[16]);
						READ_MICRO_CHUNK_WWSTRING(cload, VARID_DEF_RADIO_ICON_18, mRadioCmdIcons[17]);
						READ_MICRO_CHUNK_WWSTRING(cload, VARID_DEF_RADIO_ICON_19, mRadioCmdIcons[18]);
						READ_MICRO_CHUNK_WWSTRING(cload, VARID_DEF_RADIO_ICON_20, mRadioCmdIcons[19]);
						READ_MICRO_CHUNK_WWSTRING(cload, VARID_DEF_RADIO_ICON_21, mRadioCmdIcons[20]);
						READ_MICRO_CHUNK_WWSTRING(cload, VARID_DEF_RADIO_ICON_22, mRadioCmdIcons[21]);
						READ_MICRO_CHUNK_WWSTRING(cload, VARID_DEF_RADIO_ICON_23, mRadioCmdIcons[22]);
						READ_MICRO_CHUNK_WWSTRING(cload, VARID_DEF_RADIO_ICON_24, mRadioCmdIcons[23]);
						READ_MICRO_CHUNK_WWSTRING(cload, VARID_DEF_RADIO_ICON_25, mRadioCmdIcons[24]);
						READ_MICRO_CHUNK_WWSTRING(cload, VARID_DEF_RADIO_ICON_26, mRadioCmdIcons[25]);
						READ_MICRO_CHUNK_WWSTRING(cload, VARID_DEF_RADIO_ICON_27, mRadioCmdIcons[26]);
						READ_MICRO_CHUNK_WWSTRING(cload, VARID_DEF_RADIO_ICON_28, mRadioCmdIcons[27]);
						READ_MICRO_CHUNK_WWSTRING(cload, VARID_DEF_RADIO_ICON_29, mRadioCmdIcons[28]);
						READ_MICRO_CHUNK_WWSTRING(cload, VARID_DEF_RADIO_ICON_30, mRadioCmdIcons[29]);
						
						READ_MICRO_CHUNK(cload, VARID_DEF_NOD_ION_BEACON_DEPLOYED_ID, mIonBeaconDeployedID[0]);
						READ_MICRO_CHUNK(cload, VARID_DEF_GDI_ION_BEACON_DEPLOYED_ID, mIonBeaconDeployedID[1]);
						READ_MICRO_CHUNK(cload, VARID_DEF_NOD_ION_BEACON_DISARMED_ID, mIonBeaconDisarmedID[0]);
						READ_MICRO_CHUNK(cload, VARID_DEF_GDI_ION_BEACON_DISARMED_ID, mIonBeaconDisarmedID[1]);
						READ_MICRO_CHUNK(cload, VARID_DEF_NOD_ION_BEACON_WARNING_ID, mIonBeaconWarningID[0]);
						READ_MICRO_CHUNK(cload, VARID_DEF_GDI_ION_BEACON_WARNING_ID, mIonBeaconWarningID[1]);

						READ_MICRO_CHUNK(cload, VARID_DEF_NOD_NUKE_BEACON_DEPLOYED_ID, mNukeBeaconDeployedID[0]);
						READ_MICRO_CHUNK(cload, VARID_DEF_GDI_NUKE_BEACON_DEPLOYED_ID, mNukeBeaconDeployedID[1]);
						READ_MICRO_CHUNK(cload, VARID_DEF_NOD_NUKE_BEACON_DISARMED_ID, mNukeBeaconDisarmedID[0]);
						READ_MICRO_CHUNK(cload, VARID_DEF_GDI_NUKE_BEACON_DISARMED_ID, mNukeBeaconDisarmedID[1]);
						READ_MICRO_CHUNK(cload, VARID_DEF_NOD_NUKE_BEACON_WARNING_ID, mNukeBeaconWarningID[0]);
						READ_MICRO_CHUNK(cload, VARID_DEF_GDI_NUKE_BEACON_WARNING_ID, mNukeBeaconWarningID[1]);

						default:
							Debug_Say(("Unhandled Micro Chunk:%d File:%s Line:%d\r\n",cload.Cur_Micro_Chunk_ID (),__FILE__,__LINE__));
							break;
						}

					cload.Close_Micro_Chunk();
					}
				break;

			default:
				Debug_Say(("Unhandled Chunk:%d File:%s Line:%d\r\n",cload.Cur_Chunk_ID(),__FILE__,__LINE__));
				break;
			}

		cload.Close_Chunk();
		}

	return true;
	}

int CNCModeSettingsDef::Get_Power_Offline_Report(int team) const
	{
	if (team >= 0 && team < NUM_TEAMS)
		{
		return mPowerOfflineID[team];
		}

	return 0;
	}

int CNCModeSettingsDef::Get_Purchase_Canceled_Report(int team) const
	{
	if (team >= 0 && team < NUM_TEAMS)
		{
		return mPurchaseCanceledID[team];
		}

	return 0;
	}

int CNCModeSettingsDef::Get_Insufficient_Funds_Report(int team) const
	{
	if (team >= 0 && team < NUM_TEAMS)
		{
		return mInsufficientFundsID[team];
		}

	return 0;
	}

int CNCModeSettingsDef::Get_Constructing_Report(int team) const
	{
	if (team >= 0 && team < NUM_TEAMS)
		{
		return mConstructingID[team];
		}

	return 0;
	}

int CNCModeSettingsDef::Get_Unit_Ready_Report(int team) const
	{
	if (team >= 0 && team < NUM_TEAMS)
		{
		return mUnitReadyID[team];
		}

	return 0;
	}

int CNCModeSettingsDef::Get_Ion_Beacon_Deployed_Report(int team) const
	{
	if (team >= 0 && team < NUM_TEAMS)
		{
		return mIonBeaconDeployedID[team];
		}

	return 0;
	}

int CNCModeSettingsDef::Get_Ion_Beacon_Disarmed_Report(int team) const
	{
	if (team >= 0 && team < NUM_TEAMS)
		{
		return mIonBeaconDisarmedID[team];
		}

	return 0;
	}

int CNCModeSettingsDef::Get_Ion_Beacon_Warning_Report(int team) const
	{
	if (team >= 0 && team < NUM_TEAMS)
		{
		return mIonBeaconWarningID[team];
		}

	return 0;
	}

int CNCModeSettingsDef::Get_Nuke_Beacon_Deployed_Report(int team) const
	{
	if (team >= 0 && team < NUM_TEAMS)
		{
		return mNukeBeaconDeployedID[team];
		}

	return 0;
	}

int CNCModeSettingsDef::Get_Nuke_Beacon_Disarmed_Report(int team) const
	{
	if (team >= 0 && team < NUM_TEAMS)
		{
		return mNukeBeaconDisarmedID[team];
		}

	return 0;
	}

int CNCModeSettingsDef::Get_Nuke_Beacon_Warning_Report(int team) const
	{
	if (team >= 0 && team < NUM_TEAMS)
		{
		return mNukeBeaconWarningID[team];
		}

	return 0;
	}

int CNCModeSettingsDef::Get_Radio_Command(int num) const
	{
	return mRadioCmds[num];
	}

const char *CNCModeSettingsDef::Get_Radio_Command_Emot_Icon(int num) const
	{
	const char *retval = NULL;
	if (num >= 0 && num < 30)
		{
		retval = mRadioCmdIcons[num];
		}
	return retval;
	}
