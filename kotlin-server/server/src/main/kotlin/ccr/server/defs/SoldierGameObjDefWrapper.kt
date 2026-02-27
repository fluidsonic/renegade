package ccr.server.defs

/**
 * Wraps [SoldierGameObjDef] (a data class) as a [DefinitionClass] subclass so it can
 * be stored and retrieved by ID from [ccr.server.level.DefinitionRegistry].
 */
class SoldierGameObjDefWrapper(
    name: String,
    id: UInt,
    chunkId: UInt,
    val soldierDef: SoldierGameObjDef,
) : BaseGameObjDef(name, id, chunkId)
