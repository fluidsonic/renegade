package ccr.server.combat

import ccr.server.mix.IniFile
import ccr.server.mix.MixReader

// C++: ArmorWarheadManager (damage.h / damage.cpp) — manages the warhead-vs-armor damage multiplier matrix,
// shield absorption, soft armor classification, special damage types, and impervious skins.
object ArmorWarheadManager {

    // C++: SpecialDamageType enum
    const val SPECIAL_DAMAGE_TYPE_NONE = 0
    const val SPECIAL_DAMAGE_TYPE_FIRE = 1
    const val SPECIAL_DAMAGE_TYPE_CHEM = 2
    const val SPECIAL_DAMAGE_TYPE_ELECTRIC = 3
    const val SPECIAL_DAMAGE_TYPE_CNC_FIRE = 4
    const val SPECIAL_DAMAGE_TYPE_CNC_CHEM = 5
    const val SPECIAL_DAMAGE_TYPE_SUPER_FIRE = 6
    const val NUM_SPECIAL_DAMAGE_TYPES = 7

    // C++: DynamicVectorClass<StringClass> ArmorNames / WarheadNames
    private var armorNames: List<String> = emptyList()
    private var warheadNames: List<String> = emptyList()

    // C++: DynamicVectorClass<int> ArmorSaveIDs / WarheadSaveIDs
    private var armorSaveIDs: IntArray = IntArray(0)
    private var warheadSaveIDs: IntArray = IntArray(0)

    // C++: safe_float* Multipliers — flat [armor * nWarheads + warhead]
    private var multipliers: FloatArray = FloatArray(0)

    // C++: safe_float* Absorbsion — flat [armor * nWarheads + warhead]
    private var absorption: FloatArray = FloatArray(0)

    // C++: DynamicVectorClass<bool> SoftArmorTable
    private var softArmorTable: BooleanArray = BooleanArray(0)

    // Per warhead
    private var specialDamageTypes: IntArray = IntArray(0)
    private var specialDamageProbability: FloatArray = FloatArray(0)
    private var visceroidProbability: FloatArray = FloatArray(0)

    // Per special damage type
    private var specialDamageWarhead: IntArray = IntArray(NUM_SPECIAL_DAMAGE_TYPES)
    private var specialDamageDuration: FloatArray = FloatArray(NUM_SPECIAL_DAMAGE_TYPES)
    private var specialDamageScale: FloatArray = FloatArray(NUM_SPECIAL_DAMAGE_TYPES)
    private var specialDamageExplosion: Array<String> = Array(NUM_SPECIAL_DAMAGE_TYPES) { "" }

    // C++: DynamicVectorClass<ArmorType> ImperviousSkins[NUM_SPECIAL_DAMAGE_TYPES]
    private var imperviousSkins: Array<MutableList<Int>> = Array(NUM_SPECIAL_DAMAGE_TYPES) { mutableListOf() }

    // C++: Get_Num_Armor_Types()
    val numArmorTypes: Int get() = armorNames.size

    // C++: Get_Num_Warhead_Types()
    val numWarheadTypes: Int get() = warheadNames.size

    // C++: ArmorWarheadManager::Init (damage.cpp:68-254) — loads armor.ini from the provided always archives
    fun init(alwaysMixes: List<MixReader>) {
        shutdown()

        val bytes = alwaysMixes.firstNotNullOfOrNull { it.readFile("armor.ini") }
            ?: error("armor.ini not found in any always archive — required")

        val ini = IniFile(bytes)

        // Load Armor Types — C++: Get_String(section, Get_Entry(section, i), "", buf, sizeof(buf))
        val na = ini.entryCount("Armor_Types")
        val armorNamesList = mutableListOf<String>()
        for (i in 0 until na) {
            val name = ini.getString("Armor_Types", ini.getEntry("Armor_Types", i), "")
            armorNamesList.add(name)
        }
        armorNames = armorNamesList
        armorSaveIDs = IntArray(na) { it }
        softArmorTable = BooleanArray(na)

        // Load Warhead Types
        val nw = ini.entryCount("Warhead_Types")
        val warheadNamesList = mutableListOf<String>()
        for (i in 0 until nw) {
            val name = ini.getString("Warhead_Types", ini.getEntry("Warhead_Types", i), "")
            warheadNamesList.add(name)
        }
        warheadNames = warheadNamesList
        warheadSaveIDs = IntArray(nw) { it }

        if (na == 0 || nw == 0) return

        // Load Armor Save IDs
        for (i in 0 until na) {
            armorSaveIDs[i] = ini.getInt("Armor_Save_IDs", armorNames[i], i)
        }

        // Load Warhead Save IDs
        for (i in 0 until nw) {
            warheadSaveIDs[i] = ini.getInt("Warhead_Save_IDs", warheadNames[i], i)
        }

        // Load Soft Armor Table
        val softCount = ini.entryCount("Soft_Armor")
        for (i in 0 until softCount) {
            val name = ini.getString("Soft_Armor", ini.getEntry("Soft_Armor", i), "")
            val type = getArmorType(name)
            softArmorTable[type] = true
        }

        // Load Multipliers — C++: Multipliers[armor * nWarheads + warhead]
        multipliers = FloatArray(na * nw) { 1.0f }
        for (ai in 0 until na) {
            val section = "Scale_${armorNames[ai]}"
            for (wi in 0 until nw) {
                multipliers[ai * nw + wi] = ini.getFloat(section, warheadNames[wi], 1.0f)
            }
        }

        // Load Shield Absorption — C++: Absorbsion[armor * nWarheads + warhead]
        absorption = FloatArray(na * nw)
        for (ai in 0 until na) {
            val section = "Shield_${armorNames[ai]}"
            for (wi in 0 until nw) {
                absorption[ai * nw + wi] = ini.getFloat(section, warheadNames[wi], 0.0f)
            }
        }

        // Load Warhead Special Damage
        specialDamageTypes = IntArray(nw)
        specialDamageProbability = FloatArray(nw)
        visceroidProbability = FloatArray(nw)
        for (wi in 0 until nw) {
            // C++: stricmp-based string comparison for damage type
            val typeStr = ini.getString("Special_Damage_Type", warheadNames[wi], "")
            if (typeStr.isNotEmpty()) {
                specialDamageTypes[wi] = when (typeStr.uppercase()) {
                    "FIRE" -> SPECIAL_DAMAGE_TYPE_FIRE
                    "CHEM" -> SPECIAL_DAMAGE_TYPE_CHEM
                    "CNC_FIRE" -> SPECIAL_DAMAGE_TYPE_CNC_FIRE
                    "CNC_CHEM" -> SPECIAL_DAMAGE_TYPE_CNC_CHEM
                    "ELECTRIC" -> SPECIAL_DAMAGE_TYPE_ELECTRIC
                    else -> SPECIAL_DAMAGE_TYPE_NONE
                }
            }
            specialDamageProbability[wi] =
                ini.getFloat("Special_Damage_Probability", warheadNames[wi], 0.0f)
            visceroidProbability[wi] =
                ini.getFloat("Visceroid_Probability", warheadNames[wi], 0.0f)
        }

        // Initialize per-type special damage arrays
        specialDamageWarhead = IntArray(NUM_SPECIAL_DAMAGE_TYPES)
        specialDamageDuration = FloatArray(NUM_SPECIAL_DAMAGE_TYPES)
        specialDamageScale = FloatArray(NUM_SPECIAL_DAMAGE_TYPES)
        specialDamageExplosion = Array(NUM_SPECIAL_DAMAGE_TYPES) { "" }

        // Load per-type special damage sections (FIRE through CNC_CHEM)
        // C++: SpecialDamageSectionNames array, loop FIRE..CNC_CHEM
        val specialDamageSectionNames = arrayOf(
            "Special_Damage_None",
            "Special_Damage_Fire",
            "Special_Damage_Chem",
            "Special_Damage_Electric",
            "Special_Damage_CNC_Fire",
            "Special_Damage_CNC_Chem",
        )
        for (i in SPECIAL_DAMAGE_TYPE_FIRE..SPECIAL_DAMAGE_TYPE_CNC_CHEM) {
            val section = specialDamageSectionNames[i]
            specialDamageWarhead[i] = getWarheadType(ini.getString(section, "Warhead", ""))
            specialDamageDuration[i] = ini.getFloat(section, "Duration", 0.0f)
            specialDamageScale[i] = ini.getFloat(section, "Scale", 1.0f)
            specialDamageExplosion[i] = ini.getString(section, "Explosion", "")
        }

        // Copy fire into super fire — C++: damage.cpp:220-223
        specialDamageWarhead[SPECIAL_DAMAGE_TYPE_SUPER_FIRE] = specialDamageWarhead[SPECIAL_DAMAGE_TYPE_FIRE]
        specialDamageDuration[SPECIAL_DAMAGE_TYPE_SUPER_FIRE] = specialDamageDuration[SPECIAL_DAMAGE_TYPE_FIRE]
        specialDamageScale[SPECIAL_DAMAGE_TYPE_SUPER_FIRE] = specialDamageScale[SPECIAL_DAMAGE_TYPE_FIRE]
        specialDamageExplosion[SPECIAL_DAMAGE_TYPE_SUPER_FIRE] = specialDamageExplosion[SPECIAL_DAMAGE_TYPE_FIRE]

        // Load Impervious skins — C++: ImperviousSectionNames, loop FIRE..CNC_CHEM
        imperviousSkins = Array(NUM_SPECIAL_DAMAGE_TYPES) { mutableListOf() }
        val imperviousSectionNames = arrayOf(
            "Impervious_None",
            "Impervious_Fire",
            "Impervious_Chem",
            "Impervious_Electric",
            "Impervious_CNC_Fire",
            "Impervious_CNC_Chem",
        )
        for (damage in SPECIAL_DAMAGE_TYPE_FIRE..SPECIAL_DAMAGE_TYPE_CNC_CHEM) {
            val section = imperviousSectionNames[damage]
            val count = ini.entryCount(section)
            for (entry in 0 until count) {
                val name = ini.getString(section, ini.getEntry(section, entry), "")
                if (name.isNotEmpty()) {
                    imperviousSkins[damage].add(getArmorType(name))
                }
            }
        }

        println("[ARMOR] loaded $na armor types × $nw warhead types")
    }

    // C++: ArmorWarheadManager::Shutdown
    fun shutdown() {
        armorNames = emptyList()
        warheadNames = emptyList()
        armorSaveIDs = IntArray(0)
        warheadSaveIDs = IntArray(0)
        multipliers = FloatArray(0)
        absorption = FloatArray(0)
        softArmorTable = BooleanArray(0)
        specialDamageTypes = IntArray(0)
        specialDamageProbability = FloatArray(0)
        visceroidProbability = FloatArray(0)
        specialDamageWarhead = IntArray(NUM_SPECIAL_DAMAGE_TYPES)
        specialDamageDuration = FloatArray(NUM_SPECIAL_DAMAGE_TYPES)
        specialDamageScale = FloatArray(NUM_SPECIAL_DAMAGE_TYPES)
        specialDamageExplosion = Array(NUM_SPECIAL_DAMAGE_TYPES) { "" }
        imperviousSkins = Array(NUM_SPECIAL_DAMAGE_TYPES) { mutableListOf() }
    }

    // C++: Get_Armor_Type — case-insensitive name lookup, 0 if not found
    fun getArmorType(name: String): Int {
        for (i in armorNames.indices) {
            if (armorNames[i].equals(name, ignoreCase = true)) return i
        }
        return 0
    }

    // C++: Get_Warhead_Type — case-insensitive name lookup, 0 if not found
    fun getWarheadType(name: String): Int {
        for (i in warheadNames.indices) {
            if (warheadNames[i].equals(name, ignoreCase = true)) return i
        }
        return 0
    }

    // C++: Get_Armor_Name
    fun getArmorName(type: Int): String = armorNames[type]

    // C++: Get_Warhead_Name
    fun getWarheadName(type: Int): String = warheadNames[type]

    // C++: Get_Armor_Save_ID
    fun getArmorSaveID(type: Int): Int = armorSaveIDs[type]

    // C++: Find_Armor_Save_ID — linear search, 0 if not found
    fun findArmorSaveID(id: Int): Int {
        for (i in armorSaveIDs.indices) {
            if (armorSaveIDs[i] == id) return i
        }
        return 0
    }

    // C++: Get_Warhead_Save_ID
    fun getWarheadSaveID(type: Int): Int = warheadSaveIDs[type]

    // C++: Find_Warhead_Save_ID — linear search, 0 if not found
    fun findWarheadSaveID(id: Int): Int {
        for (i in warheadSaveIDs.indices) {
            if (warheadSaveIDs[i] == id) return i
        }
        return 0
    }

    // C++: Get_Damage_Multiplier(ArmorType armor, WarheadType warhead) — takes type indices
    fun getDamageMultiplier(armor: Int, warhead: Int): Float {
        val nw = warheadNames.size
        if (nw == 0) return 1.0f
        return multipliers[armor * nw + warhead]
    }

    // C++: Get_Shield_Absorbsion(ArmorType armor, WarheadType warhead) — takes type indices
    fun getShieldAbsorption(armor: Int, warhead: Int): Float {
        val nw = warheadNames.size
        if (nw == 0) return 0.0f
        return absorption[armor * nw + warhead]
    }

    // C++: Is_Armor_Soft
    fun isArmorSoft(armor: Int): Boolean {
        if (armor < 0 || armor >= softArmorTable.size) return false
        return softArmorTable[armor]
    }

    // C++: Get_Special_Damage_Type(WarheadType)
    fun getSpecialDamageType(warhead: Int): Int = specialDamageTypes[warhead]

    // C++: Get_Special_Damage_Probability(WarheadType)
    fun getSpecialDamageProbability(warhead: Int): Float = specialDamageProbability[warhead]

    // C++: Get_Special_Damage_Warhead(SpecialDamageType)
    fun getSpecialDamageWarhead(type: Int): Int = specialDamageWarhead[type]

    // C++: Get_Special_Damage_Duration(SpecialDamageType)
    fun getSpecialDamageDuration(type: Int): Float = specialDamageDuration[type]

    // C++: Get_Special_Damage_Scale(SpecialDamageType)
    fun getSpecialDamageScale(type: Int): Float = specialDamageScale[type]

    // C++: Get_Special_Damage_Explosion(SpecialDamageType)
    fun getSpecialDamageExplosion(type: Int): String = specialDamageExplosion[type]

    // C++: Get_Visceroid_Probability(WarheadType)
    fun getVisceroidProbability(warhead: Int): Float = visceroidProbability[warhead]

    // C++: Is_Skin_Impervious(SpecialDamageType, ArmorType)
    fun isSkinImpervious(type: Int, skin: Int): Boolean = imperviousSkins[type].contains(skin)

    // Backward-compatible save-ID based multiplier lookup
    fun getDamageMultiplierBySaveId(warheadSaveId: Int, armorSaveId: Int): Float {
        if (armorNames.isEmpty()) return 1.0f
        val ai = findArmorSaveID(armorSaveId)
        val wi = findWarheadSaveID(warheadSaveId)
        return getDamageMultiplier(ai, wi)
    }

    // Backward-compatible: scale raw damage by warhead/armor save ID multiplier
    fun scaleDamage(rawDamage: Float, warheadSaveId: Int, armorSaveId: Int): Float =
        rawDamage * getDamageMultiplierBySaveId(warheadSaveId, armorSaveId)
}
