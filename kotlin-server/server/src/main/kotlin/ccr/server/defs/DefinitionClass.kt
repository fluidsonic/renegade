package ccr.server.defs

/**
 * Base class mirroring the C++ DefinitionClass (wwsaveload/definition.h).
 *
 * Fields [name] and [id] correspond to m_Name and m_ID parsed from
 * DefinitionClass::Save_Variables micro-chunks (VARID 0x01 and 0x03).
 * [classId] is the factory/persist class ID from the outer chunk wrapper.
 */
open class DefinitionClass(
    val name: String,
    val id: UInt,
    val classId: UInt,
)
