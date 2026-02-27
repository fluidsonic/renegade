package ccr.server.net

import ccr.net.bitstream.BitStream

// C++: cConsoleCommandEvent — networkClassId = NETCLASSID_CONSOLECOMMANDEVENT = 1005
// BIT_CREATION: [Command: terminatedString]
// Note: The C++ Add_Terminated_String uses permitEmpty=false
class ConsoleCommandEvent(
    val command: String,
) : NetEvent() {
    override val networkClassId: Int = 1005

    override fun exportCreation(packet: BitStream) {
        setDeletePending()  // C++: one-shot event — Export_Creation calls Set_Delete_Pending()
        packet.addTerminatedString(command)
    }
}
