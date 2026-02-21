package ccr.net.bitstream

// C++: wwbitpack/bitpackids.h
// Encoder type IDs for quantized bit encoding.
// Values match the enum order in bitpackids.h (0-indexed).

const val BITPACK_WORLD_POSITION_X              = 0
const val BITPACK_WORLD_POSITION_Y              = 1
const val BITPACK_WORLD_POSITION_Z              = 2

const val BITPACK_ONE_TIME_BOOLEAN_BITS         = 3
const val BITPACK_CONTINUOUS_BOOLEAN_BITS       = 4
const val BITPACK_ANALOG_VALUES                 = 5

const val BITPACK_HEALTH                        = 6
const val BITPACK_SHIELD_STRENGTH               = 7
const val BITPACK_SHIELD_TYPE                   = 8

const val BITPACK_CTF_TEAM_FLAG                 = 9

const val BITPACK_HUMAN_STATE                   = 10
const val BITPACK_HUMAN_SUB_STATE               = 11

const val BITPACK_VEHICLE_VELOCITY              = 12
const val BITPACK_VEHICLE_ANGULAR_VELOCITY      = 13
const val BITPACK_VEHICLE_QUATERNION            = 14
const val BITPACK_VEHICLE_LOCK_TIMER            = 15

const val BITPACK_DOOR_STATE                    = 16
const val BITPACK_ELEVATOR_STATE                = 17
const val BITPACK_ELEVATOR_TOP_DOOR_STATE       = 18
const val BITPACK_ELEVATOR_BOTTOM_DOOR_STATE    = 19

const val BITPACK_BUILDING_RADIUS               = 20
const val BITPACK_BUILDING_STATE                = 21

const val BITPACK_CONTROL_MOVES_CS              = 22
const val BITPACK_CONTROL_MOVES_SC              = 23

const val BITPACK_PACKET_TYPE                   = 24
const val BITPACK_PACKET_ID                     = 25
