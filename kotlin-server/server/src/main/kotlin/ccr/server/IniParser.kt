package ccr.server

/**
 * Minimal read-only INI parser.
 * Supports sections `[Name]`, `key=value` pairs, and `;`/`#` comments.
 */
class IniParser(private val sections: Map<String, Map<String, String>>) {

    fun getString(section: String, key: String, default: String = ""): String =
        sections[section]?.get(key) ?: default

    fun getInt(section: String, key: String, default: Int = 0): Int =
        sections[section]?.get(key)?.trim()?.toIntOrNull() ?: default

    /**
     * Matches C++ Get_Bool: "yes"/"true"/"1" → true (case-insensitive).
     */
    fun getBool(section: String, key: String, default: Boolean = false): Boolean {
        val value = sections[section]?.get(key)?.trim()?.lowercase() ?: return default
        return value == "yes" || value == "true" || value == "1"
    }

    companion object {
        fun parse(text: String): IniParser {
            val sections = mutableMapOf<String, MutableMap<String, String>>()
            var current = ""
            for (raw in text.lines()) {
                val line = raw.trim()
                    .let { it.substringBefore(';').substringBefore('#').trim() }
                if (line.isEmpty()) continue
                if (line.startsWith('[') && line.endsWith(']')) {
                    current = line.substring(1, line.length - 1).trim()
                    sections.getOrPut(current) { mutableMapOf() }
                } else {
                    val eq = line.indexOf('=')
                    if (eq > 0) {
                        val key = line.substring(0, eq).trim()
                        val value = line.substring(eq + 1).trim()
                        sections.getOrPut(current) { mutableMapOf() }[key] = value
                    }
                }
            }
            return IniParser(sections)
        }
    }
}
