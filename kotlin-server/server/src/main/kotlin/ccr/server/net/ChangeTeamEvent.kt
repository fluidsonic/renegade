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
        actIfWiredUp()
    }

    override fun act() {
        if (!network.config.isTeamChangingAllowed) {
            println("[GAME] CHANGETEAMEVENT from senderId=$senderId: team changing is disabled, ignored")
            setDeletePending(); return
        }
        val currentTeam = network.god.playerTeams[senderId] ?: 0
        val newTeam = if (currentTeam == 0) 1 else 0
        network.god.playerTeams[senderId] = newTeam
        network.god.playersByHost[senderId]?.team = newTeam
        println("[GAME] CHANGETEAMEVENT from senderId=$senderId: ${if (currentTeam == 0) "NOD" else "GDI"} → ${if (newTeam == 0) "NOD" else "GDI"}")
        // Kill existing soldier so god.think() respawns with the new team
        network.god.deleteSoldier(senderId)
        val host = network.connectionManager.getHost(senderId)
        if (host != null) network.sendPlayerRareUpdate(host, senderId)
        setDeletePending()
    }
}
