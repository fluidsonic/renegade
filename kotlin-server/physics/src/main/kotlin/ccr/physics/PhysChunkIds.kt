package ccr.physics

/**
 * Chunk IDs for physics persistence.
 *
 * Sources:
 *   - wwphysids.h  — top-level factory/subsystem IDs (CHUNKID_WWPHYS_BEGIN = 0x00020000)
 *   - phys.cpp     — PhysClass variable/model chunk IDs (local enum starting at 0x00660055)
 *   - staticphys.cpp — StaticPhysClass chunk IDs (decimal literal 14430100 = 0xDC2F94)
 *
 * NOTE: None of these values must ever change — they are embedded in persisted level files.
 */
object PhysChunkIds {

    // -------------------------------------------------------------------------
    // Sub-system chunk IDs (wwphysids.h, CHUNKID_WWPHYS_BEGIN = 0x00020000)
    // -------------------------------------------------------------------------

    /** Static scene data subsystem. */
    const val PHYSICS_CHUNKID_STATIC_DATA_SUBSYSTEM: UInt = 0x00020000u
    /** Static physics objects subsystem. */
    const val PHYSICS_CHUNKID_STATIC_OBJECTS_SUBSYSTEM: UInt = 0x00020001u
    /** Dynamic scene data subsystem. */
    const val PHYSICS_CHUNKID_DYNAMIC_DATA_SUBSYSTEM: UInt = 0x00020050u

    // -------------------------------------------------------------------------
    // Individual persist-object chunk IDs (sequential from CHUNKID_WWPHYS_BEGIN + 0x100)
    // -------------------------------------------------------------------------

    const val PHYSICS_CHUNKID_DECORATIONPHYS: UInt = 0x00020100u
    const val PHYSICS_CHUNKID_HUMANPHYS: UInt = 0x00020101u
    const val PHYSICS_CHUNKID_LIGHTPHYS: UInt = 0x00020102u
    const val PHYSICS_CHUNKID_MOTORCYCLE: UInt = 0x00020103u
    const val PHYSICS_CHUNKID_MOTORVEHICLE: UInt = 0x00020104u
    const val PHYSICS_CHUNKID_PHYS3: UInt = 0x00020105u
    const val PHYSICS_CHUNKID_PROJECTILE: UInt = 0x00020106u
    const val PHYSICS_CHUNKID_RENDEROBJPHYS: UInt = 0x00020107u
    const val PHYSICS_CHUNKID_RIGIDBODY: UInt = 0x00020108u
    const val PHYSICS_CHUNKID_STATICPHYS: UInt = 0x00020109u
    const val PHYSICS_CHUNKID_WHEELEDVEHICLE: UInt = 0x0002010Au
    const val PHYSICS_CHUNKID_STATICANIMPHYS: UInt = 0x0002010Bu
    const val PHYSICS_CHUNKID_TIMEDDECORATIONPHYS: UInt = 0x0002010Cu
    const val PHYSICS_CHUNKID_VEHICLEPHYS: UInt = 0x0002010Du
    const val PHYSICS_CHUNKID_TRACKEDVEHICLE: UInt = 0x0002010Eu
    const val PHYSICS_CHUNKID_VTOLVEHICLE: UInt = 0x0002010Fu
    const val PHYSICS_CHUNKID_WAYPATH: UInt = 0x00020110u
    const val PHYSICS_CHUNKID_WAYPOINT: UInt = 0x00020111u
    const val PHYSICS_CHUNKID_DYNAMICANIMPHYS: UInt = 0x00020112u
    const val PHYSICS_CHUNKID_SHAKEABLESTATICPHYS: UInt = 0x00020113u
    const val PHYSICS_CHUNKID_ACCESSIBLEPHYS: UInt = 0x00020114u

    // -------------------------------------------------------------------------
    // Definition object chunk IDs (sequential from CHUNKID_WWPHYS_BEGIN + 0x500)
    // -------------------------------------------------------------------------

    const val PHYSICS_CHUNKID_DECOPHYSDEF: UInt = 0x00020500u
    const val PHYSICS_CHUNKID_HUMANPHYSDEF: UInt = 0x00020501u
    const val PHYSICS_CHUNKID_LIGHTPHYSDEF: UInt = 0x00020502u
    const val PHYSICS_CHUNKID_MOTORCYCLEDEF: UInt = 0x00020503u
    const val PHYSICS_CHUNKID_MOTORVEHICLEDEF: UInt = 0x00020504u
    const val PHYSICS_CHUNKID_PHYS3DEF: UInt = 0x00020505u
    const val PHYSICS_CHUNKID_PROJECTILEDEF: UInt = 0x00020506u
    const val PHYSICS_CHUNKID_RIGIDBODYDEF: UInt = 0x00020507u
    const val PHYSICS_CHUNKID_STATICPHYSDEF: UInt = 0x00020508u
    const val PHYSICS_CHUNKID_WHEELEDVEHICLEDEF: UInt = 0x00020509u
    const val PHYSICS_CHUNKID_STATICANIMPHYSDEF: UInt = 0x0002050Au
    const val PHYSICS_CHUNKID_TIMEDDECOPHYSDEF: UInt = 0x0002050Bu
    const val PHYSICS_CHUNKID_VEHICLEPHYSDEF: UInt = 0x0002050Cu
    const val PHYSICS_CHUNKID_TRACKEDVEHICLEDEF: UInt = 0x0002050Du
    const val PHYSICS_CHUNKID_VTOLVEHICLEDEF: UInt = 0x0002050Eu
    const val PHYSICS_CHUNKID_DYNAMICANIMPHYSDEF: UInt = 0x0002050Fu
    const val PHYSICS_CHUNKID_SHAKEABLESTATICPHYSDEF: UInt = 0x00020510u
    const val PHYSICS_CHUNKID_ACCESSIBLEPHYSDEF: UInt = 0x00020511u

    // -------------------------------------------------------------------------
    // External persist-object chunk IDs (CHUNKID_WWPHYS_BEGIN + 0xA00)
    // -------------------------------------------------------------------------

    const val PHYSICS_CHUNKID_DOORPHYS: UInt = 0x00020A00u
    const val PHYSICS_CHUNKID_ELEVATORPHYS: UInt = 0x00020A01u
    const val PHYSICS_CHUNKID_DAMAGEABLESTATICPHYS: UInt = 0x00020A02u
    const val PHYSICS_CHUNKID_BUILDINGAGGREGATE: UInt = 0x00020A03u

    // -------------------------------------------------------------------------
    // External persist-object def chunk IDs (CHUNKID_WWPHYS_BEGIN + 0xC00)
    // -------------------------------------------------------------------------

    const val PHYSICS_CHUNKID_DOORPHYSDEF: UInt = 0x00020C00u
    const val PHYSICS_CHUNKID_ELEVATORPHYSDEF: UInt = 0x00020C01u
    const val PHYSICS_CHUNKID_DAMAGEABLESTATICPHYSDEF: UInt = 0x00020C02u
    const val PHYSICS_CHUNKID_BUILDINGAGGREGATEDEF: UInt = 0x00020C03u

    // -------------------------------------------------------------------------
    // PhysClass base chunk IDs (phys.cpp, local enum starting at 0x00660055)
    // -------------------------------------------------------------------------

    /** PhysClass variable chunk (flags, name, instanceId, definition ID, etc.). */
    const val PHYS_CHUNK_VARIABLES: UInt = 0x00660055u
    /** PhysClass model chunk (contains the render object sub-chunk). */
    const val PHYS_CHUNK_MODEL: UInt = 0x00660056u

    // PhysClass micro-chunk IDs (PHYS_VARIABLE_* in phys.cpp)
    const val PHYS_VARIABLE_CULLABLE_PTR: Int = 0x00
    const val PHYS_VARIABLE_WIDGETUSER_PTR: Int = 0x01
    const val PHYS_VARIABLE_EDITABLE_PTR: Int = 0x02
    const val PHYS_VARIABLE_FLAGS: Int = 0x03
    const val PHYS_VARIABLE_NAME: Int = 0x04
    const val PHYS_VARIABLE_OBSERVER: Int = 0x05
    const val PHYS_VARIABLE_DEFID: Int = 0x06
    const val PHYS_VARIABLE_INSTANCEID: Int = 0x07

    // -------------------------------------------------------------------------
    // StaticPhysClass chunk IDs (staticphys.cpp, decimal 14430100 = 0xDC2F94)
    // -------------------------------------------------------------------------

    /** StaticPhysClass parent PhysClass chunk wrapper. */
    const val STATICPHYS_CHUNK_PHYS: UInt = 0xDC2F94u
    /** StaticPhysClass own variables chunk (visObjectId, visSectorId). */
    const val STATICPHYS_CHUNK_VARIABLES: UInt = 0xDC2F95u
}
