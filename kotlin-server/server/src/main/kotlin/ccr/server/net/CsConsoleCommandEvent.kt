package ccr.server.net

import ccr.net.bitstream.BitStream
import ccr.server.Network

// C++: cCsConsoleCommandEvent — networkClassId = NETCLASSID_CSCONSOLECOMMANDEVENT = 1035
// Client→Server event carrying a console command string from the client.
// Wire format (BIT_CREATION):
//   Command (terminatedString, ASCII, permitEmpty=false)
class CsConsoleCommandEvent(
    var command: String = "",
) : NetEvent() {
    override val networkClassId: Int = 1035

    override fun exportCreation(packet: BitStream) {
        packet.addTerminatedString(command)
    }

    override fun importCreation(packet: BitStream) {
        command = packet.getTerminatedString()
        actIfWiredUp()
    }

    override fun act() {
        println("[GAME] CSCONSOLECOMMANDEVENT command=$command (no console dispatch)")
        setDeletePending()
    }
}
