package ccr.server.level

import ccr.server.defs.DefinitionClass

/**
 * Registry mapping definition IDs and names to typed DefinitionClass instances.
 * Not thread-safe — populate sequentially, then read concurrently.
 */
class DefinitionRegistry {
    private val byId   = mutableMapOf<UInt, DefinitionClass>()
    private val byName = mutableMapOf<String, DefinitionClass>()

    fun register(def: DefinitionClass) {
        byId[def.id] = def
        byName[def.name] = def
    }

    fun findById(id: UInt): DefinitionClass? = byId[id]
    fun findByName(name: String): DefinitionClass? = byName[name]
    fun all(): Collection<DefinitionClass> = byId.values

    val size: Int get() = byId.size
    val isEmpty: Boolean get() = byId.isEmpty()
}
