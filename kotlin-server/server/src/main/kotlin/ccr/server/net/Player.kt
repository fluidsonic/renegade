package ccr.server.net

import ccr.net.bitstream.BitStream
import ccr.net.replication.NetworkObject

// C++: cPlayer (player.cpp) — networkClassId = NETCLASSID_PLAYER = 1011
// Directly inherits NetworkObjectClass (NOT in the game object hierarchy).
// Holds player identity, team, score, and connection state.
class Player(
    val id: Int,
    val name: String,
    var team: Int,
    var isInGame: Boolean,
    var isActive: Boolean = true,
    var score: Float = 0f,
    var kills: Int = 0,
    var deaths: Int = 0,
    var money: Float = 0f,
) : NetworkObject() {

    override val networkClassId: Int = 1011  // NETCLASSID_PLAYER

    override fun delete() {}  // transient, no cleanup

    // C++: cPlayer::Export_Creation (player.cpp:877) — writes only Name as a wide string.
    override fun exportCreation(packet: BitStream) {
        packet.addWideString(name)  // Add_Wide_Terminated_String
    }

    // C++: cPlayer::Export_Rare (player.cpp:895) — writes Id, ladder, team, damage scale, ping, flags.
    // No call to NetworkObjectClass::Export_Rare (it is empty).
    override fun exportRare(packet: BitStream) {
        packet.addInt(id)           // Id
        packet.addInt(0)            // LadderPoints
        packet.addInt(team)         // PlayerType (0=NOD, 1=GDI)
        packet.addInt(1)            // DamageScaleFactor (int, not float — default 1)
        packet.addInt(0)            // Ping
        packet.addBool(isInGame)    // IsInGame
        packet.addBool(false)       // Invulnerable
        packet.addBool(isActive)    // IsActive
        packet.addInt(0)            // WolRank
        packet.addInt(0)            // NumWolGames
    }

    // C++: cPlayer::Export_Occasional (player.cpp:935) — calls PlayerDataClass::Export_Occasional
    // (Score, Money) then writes Kills and Deaths.
    override fun exportOccasional(packet: BitStream) {
        packet.addFloat(score)  // Score   (PlayerDataClass::Export_Occasional)
        packet.addFloat(money)  // Money   (PlayerDataClass::Export_Occasional)
        packet.addInt(kills)    // Kills
        packet.addInt(deaths)   // Deaths
    }

    fun resetStats() {
        score = 0f
        kills = 0
        deaths = 0
        money = 0f
    }

    fun incrementScore(amount: Float) {
        score += amount
        setObjectDirtyBit(NetworkObject.BIT_OCCASIONAL, true)
    }

    fun incrementKills() {
        kills++
        setObjectDirtyBit(NetworkObject.BIT_OCCASIONAL, true)
    }

    fun incrementDeaths() {
        deaths++
        setObjectDirtyBit(NetworkObject.BIT_OCCASIONAL, true)
    }

    fun addMoney(amount: Float) {
        money += amount
        setObjectDirtyBit(NetworkObject.BIT_OCCASIONAL, true)
    }

    fun replaceMoney(amount: Float) {
        money = amount
        setObjectDirtyBit(NetworkObject.BIT_OCCASIONAL, true)
    }

    // C++: cPlayer has no Export_Frequent override — NetworkObjectClass's empty default is used.
}
