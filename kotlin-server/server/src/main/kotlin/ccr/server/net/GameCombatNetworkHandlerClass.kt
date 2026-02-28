package ccr.server.net

import ccr.server.Network

// C++: GameCombatNetworkHandlerClass (nethandler.h / nethandler.cpp)
// The combat lib calls these commando application-level methods.
class GameCombatNetworkHandlerClass(private val network: Network) : CombatNetworkHandlerClass {

    // C++: GameCombatNetworkHandlerClass::Can_Damage
    override fun canDamage(damager: ArmedGameObj?, victim: PhysicalGameObj?): Boolean {
        if (!network.gameState.isGameplayPermitted) return false

        val smartVictim = victim as? SmartGameObj
        if (smartVictim != null && smartVictim.hasPlayer()) {
            val player = network.god.playersByHost[smartVictim.controlOwner]
            if (player != null && player.invulnerable) return false
        }

        return true
    }

    // C++: GameCombatNetworkHandlerClass::Get_Damage_Factor
    override fun getDamageFactor(damager: ArmedGameObj?, victim: PhysicalGameObj?): Float {
        val smartVictim = victim as? SmartGameObj
        if (smartVictim != null && smartVictim.hasPlayer()) {
            val player = network.god.playersByHost[smartVictim.controlOwner]
            if (player != null && player.damageScaleFactor < 100) {
                return player.damageScaleFactor / 100.0f
            }
        }
        return 1.0f
    }

    // C++: GameCombatNetworkHandlerClass::On_Soldier_Kill
    override fun onSoldierKill(killer: SoldierGameObj, victim: SoldierGameObj) {
        val killerId = killer.controlOwner
        val victimId = victim.controlOwner

        if (killer !== victim) {
            // C++: p_soldier->Has_Player() → Find_Player(killer_id) → Increment_Kills()
            var killerPlayer: Player? = null
            if (killer.hasPlayer()) {
                killerPlayer = network.god.playersByHost[killerId]
                killerPlayer?.incrementKills()
            }

            // C++: p_victim->Has_Player() → Find_Player(victim_id)
            var victimPlayer: Player? = null
            if (victim.hasPlayer()) {
                victimPlayer = network.god.playersByHost[victimId]
            }

            // C++: if (p_soldier_player != NULL && p_victim_player != NULL) — send PlayerKill event
            if (killerPlayer != null && victimPlayer != null) {
                val killerPlayerId = network.god.playerNetIds[killerId] ?: killerId
                val victimPlayerId = network.god.playerNetIds[victimId] ?: victimId
                val event = PlayerKill(killerId = killerPlayerId, victimId = victimPlayerId)
                for (clientId in network.god.playerInGame) {
                    val clientHost = network.connectionManager.getHost(clientId) ?: continue
                    network.serverSendPacket(clientHost) { bs ->
                        NetworkObjectPacketWriter.writeCreation(bs, event, event.networkId)
                    }
                }
            }

            // C++: if (p_soldier->Is_Team_Player()) → Find_Team → Increment_Kills()
            if (killer.isTeamPlayer()) {
                findTeam(killer.getPlayerType())?.incrementKills()
            }
        }
    }

    // C++: GameCombatNetworkHandlerClass::On_Soldier_Death
    override fun onSoldierDeath(soldier: SoldierGameObj) {
        // C++: if (p_soldier->Has_Player()) → Find_Player → Increment_Deaths()
        if (soldier.hasPlayer()) {
            val player = network.god.playersByHost[soldier.controlOwner]
            player?.incrementDeaths()
        }

        // C++: if (p_soldier->Is_Team_Player()) → Find_Team → Increment_Deaths()
        if (soldier.isTeamPlayer()) {
            findTeam(soldier.getPlayerType())?.incrementDeaths()
        }
    }

    // C++: GameCombatNetworkHandlerClass::Is_Gameplay_Permitted
    override fun isGameplayPermitted(): Boolean = network.gameState.isGameplayPermitted

    // C++: cTeamManager::Find_Team
    private fun findTeam(playerType: Int): Team? = when (playerType) {
        PLAYERTYPE_NOD -> network.teamNod
        PLAYERTYPE_GDI -> network.teamGdi
        else -> null
    }
}
