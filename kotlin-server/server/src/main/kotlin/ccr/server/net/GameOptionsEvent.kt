package ccr.server.net

import ccr.net.bitstream.BitStream

// C++: cGameOptionsEvent (gameoptionsevent.cpp) — networkClassId = NETCLASSID_GAMEOPTIONSEVENT = 1008
// Export_Creation: tier1 + tier2 + timeRemaining(float) + hostedGameNumber + modCrc + mapCrc
// Sent once on client connection to describe the game being hosted.
class GameOptionsEvent(private val gameData: GameData) : NetEvent() {

    override val networkClassId: Int = 1008

    // C++: cGameOptionsEvent::Export_Creation (gameoptionsevent.cpp:62-85)
    override fun exportCreation(packet: BitStream) {
        gameData.exportTier1(packet)
        gameData.exportTier2(packet)
        // Event-specific fields (gameoptionsevent.cpp):
        packet.addFloat(gameData.timeRemainingSeconds)  // TimeRemainingSeconds (float — C++ sends float here)
        packet.addInt(gameData.hostedGameNumber)         // HostedGameNumber
        packet.addInt(gameData.modNameCrc)               // ModNameCRC (repeated)
        packet.addInt(gameData.mapNameCrc)               // MapNameCRC (repeated)
    }
}
