package ccr.server.net

import ccr.net.bitstream.BitStream
import ccr.server.Network

// C++: cChangeTeamEvent — networkClassId = NETCLASSID_CHANGETEAMEVENT = 1020
// Client→Server event sent when a player requests a team change.
// Wire format (BIT_CREATION):
//   SenderId (int)
class ChangeTeamEvent(
    var senderId: Int = 0,
) : NetEvent() {
    override val networkClassId: Int = 1020

    override fun exportCreation(packet: BitStream) {
        packet.addInt(senderId)
    }

    override fun importCreation(packet: BitStream) {
        senderId = packet.getInt()
    }

    override fun act(server: Network, rhostId: Int) {
        if (!server.config.isTeamChangingAllowed) {
            println("[GAME] CHANGETEAMEVENT from rhostId=$rhostId: team changing is disabled, ignored")
            setDeletePending(); return
        }
        val currentTeam = server.god.playerTeams[rhostId] ?: 0
        val newTeam = if (currentTeam == 0) 1 else 0
        server.god.playerTeams[rhostId] = newTeam
        server.god.playersByHost[rhostId]?.team = newTeam
        println("[GAME] CHANGETEAMEVENT from rhostId=$rhostId: ${if (currentTeam == 0) "NOD" else "GDI"} → ${if (newTeam == 0) "NOD" else "GDI"}")
        // Kill existing soldier so god.think() respawns with the new team
        server.god.deleteSoldier(rhostId)
        val host = server.connectionManager.getHost(rhostId)
        if (host != null) server.sendPlayerRareUpdate(host, rhostId)
        setDeletePending()
    }
}
