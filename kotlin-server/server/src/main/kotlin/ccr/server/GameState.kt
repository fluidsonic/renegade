package ccr.server

/**
 * Port of C++ cGameData game-state logic.
 * Tracks countdown timer, intermission, and game-over conditions.
 * Called every network tick from GameServer.networkTickLoop().
 */
class GameState(private val config: ServerConfig) {

    // --- Timer ---
    // timeRemainingSeconds counts down from timeLimitMinutes*60 to 0 (0 = no time limit).
    var timeRemainingSeconds: Float = if (config.timeLimitMinutes > 0) config.timeLimitMinutes * 60f else 0f
        private set

    // gameDurationSeconds tracks elapsed play time (used for MVP qualifying time).
    var gameDurationSeconds: Float = 0f
        private set

    var frameCount: Int = 0
        private set

    // --- Intermission ---
    var isIntermission: Boolean = false
        private set
    private var intermissionTimeRemaining: Float = 0f

    // --- Game-over flag (set externally by map rotation or RCON) ---
    var manualGameOver: Boolean = false

    // --- Player count (updated each tick by GameServer) ---
    var currentPlayers: Int = 0

    // Called every network tick. deltaMs = milliseconds since last tick.
    fun think(deltaMs: Long) {
        frameCount++
        val deltaSec = deltaMs / 1000f

        if (isIntermission) {
            intermissionTimeRemaining -= deltaSec
            if (intermissionTimeRemaining <= 0f) {
                isIntermission = false
                intermissionTimeRemaining = 0f
                // Caller (GameServer) handles the actual core restart
                pendingCoreRestart = true
            }
            return
        }

        // Count elapsed play time
        gameDurationSeconds += deltaSec

        // Count down the timer (only if a time limit is set)
        if (timeRemainingSeconds > 0f) {
            timeRemainingSeconds = (timeRemainingSeconds - deltaSec).coerceAtLeast(0f)
        }
    }

    /** True during intermission or when no combat mode active. */
    val isGameplayPermitted: Boolean get() = !isIntermission

    /** Pending core restart flag -- set when intermission ends. Caller must reset this. */
    var pendingCoreRestart: Boolean = false

    /**
     * Detects whether the game should end.
     * Returns (true, winType) or (false, 0).
     * winType: 1=TIME, 2=BASE_DESTRUCTION, 3=MANUAL
     */
    fun checkGameOver(isNodBaseDestroyed: Boolean, isGdiBaseDestroyed: Boolean): Pair<Boolean, Int> {
        if (manualGameOver) return Pair(true, 3)
        if (config.timeLimitMinutes > 0 && timeRemainingSeconds <= 0f && gameDurationSeconds > 0) {
            return Pair(true, 1) // TIME
        }
        if (config.baseDestructionEndsGame) {
            if (isNodBaseDestroyed) return Pair(true, 2)
            if (isGdiBaseDestroyed) return Pair(true, 2)
        }
        return Pair(false, 0)
    }

    /**
     * Starts the intermission phase.
     * C++: cGameData::Start_Intermission() in gdcnc.cpp
     */
    fun startIntermission() {
        isIntermission = true
        val secs = if (config.intermissionTimeSeconds > 0) config.intermissionTimeSeconds else 30
        intermissionTimeRemaining = secs.toFloat()
        timeRemainingSeconds = 0f
    }

    /** Resets the game state for a new match (called at core restart). */
    fun reset() {
        timeRemainingSeconds = if (config.timeLimitMinutes > 0) config.timeLimitMinutes * 60f else 0f
        gameDurationSeconds = 0f
        frameCount = 0
        isIntermission = false
        intermissionTimeRemaining = 0f
        manualGameOver = false
        pendingCoreRestart = false
    }
}
