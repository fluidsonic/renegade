package ccr.server.net

import ccr.net.bitstream.BitStream
import ccr.net.replication.NetworkObject

// C++: cTeam (team.h / team.cpp) — classId = NETCLASSID_TEAM = 1010
// S->C object holding team stats: kills, deaths, score.
// Export_Creation: teamNumber (int)
// Export_Rare:     kills (int), deaths (int)
// Export_Occasional: score (float)
// Export_Frequent:   (empty)
class Team(
    val teamNumber: Int,
    var kills: Int = 0,
    var deaths: Int = 0,
    var score: Float = 0f,
) : NetworkObject() {
    override val networkClassId: Int = 1010  // NETCLASSID_TEAM

    override fun delete() {}

    // C++: cTeam::Export_Creation — writes TeamNumber as int
    override fun exportCreation(packet: BitStream) {
        packet.addInt(teamNumber)
    }

    override fun importCreation(packet: BitStream) {}

    // C++: cTeam::Export_Rare — writes Kills, Deaths as ints
    override fun exportRare(packet: BitStream) {
        packet.addInt(kills)
        packet.addInt(deaths)
    }

    override fun importRare(packet: BitStream) {}

    // C++: cTeam::Export_Occasional — writes Score as float
    override fun exportOccasional(packet: BitStream) {
        packet.addFloat(score)
    }

    override fun importOccasional(packet: BitStream) {}

    // C++: cTeam::Export_Frequent — empty
}
