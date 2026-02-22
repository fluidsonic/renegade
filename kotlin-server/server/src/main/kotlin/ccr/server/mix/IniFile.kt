package ccr.server.mix

// C++: INIClass (wwlib/ini.cpp) — section-based key=value config file.
// Used by ArmorWarheadManager to load armor.ini from always.dbs.
class IniFile(bytes: ByteArray) {

    // sections: lowercase-name → (ordered key list, lowercase-key → original-value map)
    private val sections = mutableMapOf<String, Pair<MutableList<String>, MutableMap<String, String>>>()

    init {
        var currentSection: Pair<MutableList<String>, MutableMap<String, String>>? = null
        for (raw in bytes.toString(Charsets.ISO_8859_1).lines()) {
            val line = raw.trim().let { if (';' in it) it.substringBefore(';').trim() else it }
            if (line.isEmpty()) continue
            if (line.startsWith('[') && line.endsWith(']')) {
                val name = line.substring(1, line.length - 1).lowercase()
                currentSection = sections.getOrPut(name) { mutableListOf<String>() to mutableMapOf() }
            } else if ('=' in line && currentSection != null) {
                val key = line.substringBefore('=').trim()
                val value = line.substringAfter('=').trim()
                if (key.isNotEmpty() && key.lowercase() !in currentSection.second) {
                    currentSection.first.add(key)
                    currentSection.second[key.lowercase()] = value
                }
            }
        }
    }

    fun getString(section: String, key: String, default: String): String =
        sections[section.lowercase()]?.second?.get(key.lowercase()) ?: default

    fun getInt(section: String, key: String, default: Int): Int =
        getString(section, key, "").toIntOrNull() ?: default

    fun getFloat(section: String, key: String, default: Float): Float =
        getString(section, key, "").toFloatOrNull() ?: default

    fun entryCount(section: String): Int =
        sections[section.lowercase()]?.first?.size ?: 0

    // Returns the original-case key name at [index] in [section].
    fun getEntry(section: String, index: Int): String =
        sections[section.lowercase()]?.first?.getOrNull(index) ?: ""
}
