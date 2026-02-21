package ccr.net.protocol

// C++: REFUSAL_CODE enum in wwnet/connect.h
enum class RefusalCode(val id: Int) {
    CLIENT_ACCEPTED(0),     // not a refusal; used internally
    GAME_FULL(1),
    BAD_PASSWORD(2),
    VERSION_MISMATCH(3),
    PLAYER_EXISTS(4),
    BY_APPLICATION(5),      // rejected by game-level ApplicationAcceptanceHandler
    ;

    companion object {
        private val byId = entries.associateBy { it.id }
        fun fromId(id: Int): RefusalCode = checkNotNull(byId[id]) { "Unknown refusal code: $id" }
    }
}
