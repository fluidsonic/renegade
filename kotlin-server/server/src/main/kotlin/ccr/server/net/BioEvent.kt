package ccr.server.net

import ccr.net.bitstream.BitStream
import ccr.net.flow.FlowController
import ccr.net.replication.NetworkObjectManager
import ccr.server.Network

// C++: cBioEvent (Commando/bioevent.h) — networkClassId = NETCLASSID_BIOEVENT = 1025
// C→S event object for transmitting initial player bio data on join.
// The server imports (reads) this; the client exports (writes) it.
//
// Wire format (Export_Creation):
//   Add(SenderId: int)
//   Add_Wide_Terminated_String(Nickname)    — UTF-16LE, length-prefixed (USHORT + chars as USHORT)
//   Add(TeamChoice: int)                    — 0=NOD, 1=GDI, -1=random
//   Add(ClanID: unsigned int)               — stored as int (same 32 bits)
//   Add_Terminated_String(MapName, false)   — ASCII, length-prefixed (USHORT + chars as BYTE)
class BioEvent(
    var senderId: Int = 0,
    var nickname: String = "",
    var teamChoice: Int = 0,
    var clanId: Int = 0,
    var mapName: String = "",
) : NetEvent() {
    override val networkClassId: Int = 1025

    override fun exportCreation(packet: BitStream) {
        packet.addInt(senderId)
        packet.addWideString(nickname)
        packet.addInt(teamChoice)
        packet.addInt(clanId)
        packet.addTerminatedString(mapName)
    }

    override fun importCreation(packet: BitStream) {
        senderId = packet.getInt()
        nickname = packet.getWideString(permitEmpty = true)
        teamChoice = packet.getInt()
        clanId = packet.getInt()
        mapName = packet.getTerminatedString()
        actIfWiredUp()
    }

    override fun act() {
        if (senderId !in network.god.playerInGame) {
            println("[GAME] BIOEVENT from senderId=$senderId → entering game (post-load)")
            network.god.playerInGame.add(senderId)
            network.flowControllers[senderId] = FlowController()

            // Mark all registered objects dirty for this new client.
            // C++: Tell_Client_About_Dynamic_Objects sets per-client dirty bits for all objects.
            // tellClientAboutDynamicObjects() will send everything on the next tick.
            NetworkObjectManager.restoreDirtyBits(senderId)

            // Teams were already sent in connectionHandler — clear their dirty bits
            network.teamNod.setObjectDirtyBits(senderId, 0)
            network.teamGdi.setObjectDirtyBits(senderId, 0)

            // Create player — sets BIT_CREATION for all clients via setObjectDirtyBit
            val host = network.connectionManager.getHost(senderId) ?: run { setDeletePending(); return }
            val nickname = network.playerNicknames.remove(host.address) ?: "Player$senderId"
            network.god.createPlayer(senderId, nickname)

            // Store player IP for server-side tracking (not sent over network)
            val ipBytes = host.address.address.address
            if (ipBytes.size == 4) {
                val ipInt = ((ipBytes[0].toInt() and 0xFF) shl 24) or
                            ((ipBytes[1].toInt() and 0xFF) shl 16) or
                            ((ipBytes[2].toInt() and 0xFF) shl 8) or
                            (ipBytes[3].toInt() and 0xFF)
                network.god.playersByHost[senderId]?.ipAddress = ipInt
            }

            // One-shot event to signal the client that gameplay can proceed
            network.sendGameDataUpdateEvent(network.connectionManager.getHost(senderId)!!)
            // Soldier spawning happens via god.think() on the next tick
            // All object creation packets sent by tellClientAboutDynamicObjects() on the next tick
        }
        setDeletePending()
    }
}
