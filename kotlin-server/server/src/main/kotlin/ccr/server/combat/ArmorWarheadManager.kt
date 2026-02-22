package ccr.server.combat

import ccr.server.mix.IniFile

// C++: ArmorWarheadManager (damage.cpp) — manages the warhead-vs-armor damage multiplier matrix.
// Table is a flat Float array: multipliers[armorIndex * numWarheads + warheadIndex]
// Save IDs (integers stored in defs/packets) map to array indices via Armor_Save_IDs / Warhead_Save_IDs.
object ArmorWarheadManager {

    private var multipliers: FloatArray = FloatArray(0) { 1.0f }
    private var numWarheads: Int = 1
    // save ID → array index
    private var armorIdToIndex   = mutableMapOf<Int, Int>()
    private var warheadIdToIndex = mutableMapOf<Int, Int>()

    // C++: ArmorWarheadManager::Init — parse armor.ini bytes.
    fun init(bytes: ByteArray) {
        val ini = IniFile(bytes)

        val armorNames   = (0 until ini.entryCount("Armor_Types"))  .map { ini.getEntry("Armor_Types",   it) }
        val warheadNames = (0 until ini.entryCount("Warhead_Types")).map { ini.getEntry("Warhead_Types", it) }
        val na = armorNames.size
        val nw = warheadNames.size
        if (na == 0 || nw == 0) return

        numWarheads = nw
        multipliers = FloatArray(na * nw) { 1.0f }

        // Build save-ID → index maps
        armorIdToIndex.clear()
        for (name in armorNames) {
            val saveId = ini.getInt("Armor_Save_IDs", name, -1)
            if (saveId >= 0) armorIdToIndex[saveId] = armorNames.indexOf(name)
        }
        warheadIdToIndex.clear()
        for (name in warheadNames) {
            val saveId = ini.getInt("Warhead_Save_IDs", name, -1)
            if (saveId >= 0) warheadIdToIndex[saveId] = warheadNames.indexOf(name)
        }

        // Fill multiplier table from Scale_<ArmorName> sections
        for ((ai, armorName) in armorNames.withIndex()) {
            for ((wi, warheadName) in warheadNames.withIndex()) {
                multipliers[ai * nw + wi] = ini.getFloat("Scale_$armorName", warheadName, 1.0f)
            }
        }

        println("[ARMOR] loaded $na armor types × $nw warhead types")
    }

    // warheadSaveId / armorSaveId are the integer IDs stored in defs (AmmoDefinitionClass.warhead,
    // DamageableGameObj shieldType / skinSaveId). Returns 1.0 if either ID is unknown.
    fun getDamageMultiplier(warheadSaveId: Int, armorSaveId: Int): Float {
        val ai = armorIdToIndex[armorSaveId]    ?: return 1.0f
        val wi = warheadIdToIndex[warheadSaveId] ?: return 1.0f
        return multipliers[ai * numWarheads + wi]
    }

    fun scaleDamage(rawDamage: Float, warheadSaveId: Int, armorSaveId: Int): Float =
        rawDamage * getDamageMultiplier(warheadSaveId, armorSaveId)
}
