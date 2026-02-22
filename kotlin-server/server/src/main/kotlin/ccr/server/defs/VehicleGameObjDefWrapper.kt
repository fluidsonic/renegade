package ccr.server.defs

/**
 * Wraps [VehicleGameObjDef] (a data class) as a [DefinitionClass] subclass so it can
 * be stored and retrieved by ID from [ccr.server.level.DefinitionRegistry].
 */
class VehicleGameObjDefWrapper(
    name: String,
    id: UInt,
    chunkId: UInt,
    val vehicleDef: VehicleGameObjDef,
) : DefinitionClass(name, id, chunkId)
