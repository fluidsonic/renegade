package ccr.server.net

import ccr.server.defs.DefinitionClass
import ccr.server.level.DefinitionRegistry

// C++: DefinitionMgrClass (wwsaveload/definitionmgr.h) — server-side registry
object DefinitionMgrClass {
    // C++: DefinitionMgrClass::Find_Definition(id) — delegates to the loaded level registry
    var registry: DefinitionRegistry? = null

    fun findDefinition(id: Int): DefinitionClass? = registry?.findById(id.toUInt())
    fun findTypedDefinition(name: String, classId: Int): DefinitionClass? = registry?.findByName(name)
    fun findAmmoDefinition(id: Int): Any? = null
    fun findAmmoDefinitionByName(name: String): Any? = null
    fun getFirst(classId: Int): Any? = null
    fun getNext(def: Any?, classId: Int): Any? = null
}
