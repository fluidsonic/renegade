package ccr.server

import ccr.server.net.BaseControllerClass

/**
 * Session-scoped container for all "global" game state.
 * Passed explicitly to subsystems that need it — avoids singletons, enables testing.
 * Port of the pattern used by C++ global accessors (The_Game(), GameObjManager, etc.).
 */
class GameContext(
    val config: ServerConfig,
    val gameObjManager: GameObjManager,
    val gameState: GameState,
) {
    // C++: BaseControllerClass::CurrentBases[PLAYERTYPE] — indexed 0=NOD, 1=GDI
    val baseControllers: Array<BaseControllerClass?> = arrayOfNulls(2)

    fun findBase(playerType: Int): BaseControllerClass? = baseControllers.getOrNull(playerType)

    // C++: CombatManager::I_Am_Server() — always true for this server
    val isServer: Boolean = true

    // C++: CombatManager::Is_Gameplay_Permitted()
    val isGameplayPermitted: Boolean get() = gameState.isGameplayPermitted

    // C++: TimeManager::Get_Frame_Seconds() — set at top of each game loop tick
    var frameDeltaSeconds: Float = 0f
}
