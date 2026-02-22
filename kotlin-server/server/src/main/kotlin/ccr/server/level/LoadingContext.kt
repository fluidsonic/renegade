package ccr.server.level

class LoadingContext {
    private val warnings = mutableListOf<String>()
    private val errors   = mutableListOf<String>()

    fun info(msg: String) {
        println("[LEVEL] $msg")
    }
    fun warn(msg: String) {
        warnings += msg
        println("[LEVEL-WARN] $msg")
    }
    fun error(msg: String) {
        errors += msg
        System.err.println("[LEVEL-ERROR] $msg")
    }

    val warningCount: Int  get() = warnings.size
    val errorCount: Int    get() = errors.size
    val hasErrors: Boolean get() = errors.isNotEmpty()

    fun summary(): String =
        "${warningCount} warning(s), ${errorCount} error(s)"
}
