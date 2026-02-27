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
    // @JvmName avoids clash with fun setKills() / fun setDeaths() below
    @get:JvmName("killsField") @set:JvmName("setKillsField")
    var kills: Int = 0,
    @get:JvmName("deathsField") @set:JvmName("setDeathsField")
    var deaths: Int = 0,
    var money: Float = 0f,
) : NetworkObject() {

    override val networkClassId: Int = 1011  // NETCLASSID_PLAYER

    override fun delete() {}  // transient, no cleanup

    // C++: cPlayer::LadderPoints — default 0; sent as int in Export_Rare
    // @JvmName avoids clash with fun setLadderPoints() below
    @get:JvmName("ladderPointsField") @set:JvmName("setLadderPointsField")
    var ladderPoints: Int = 0

    // C++: cPlayer::DamageScaleFactor — default 100 (percentage, 100 = full damage)
    // @JvmName avoids clash with fun setDamageScaleFactor() below
    @get:JvmName("damageScaleFactorField") @set:JvmName("setDamageScaleFactorField")
    var damageScaleFactor: Int = 100

    // C++: cPlayer::Ping — default -1 (unknown)
    var ping: Int = -1

    // C++: cPlayer::Invulnerable (public cBoolean field) — default false
    var invulnerable: Boolean = false

    // C++: cPlayer::WolRank — default -1 (unranked)
    var wolRank: Int = -1

    // C++: cPlayer::NumWolGames — default 0
    var numWolGames: Int = 0

    init {
        setObjectDirtyBit(BIT_CREATION, true)
    }

    // C++: cPlayer::Export_Creation (player.cpp:913) — writes only Name as a wide string.
    override fun exportCreation(packet: BitStream) {
        packet.addWideString(name)  // Add_Wide_Terminated_String
    }

    // C++: cPlayer::Export_Rare (player.cpp:931) — writes Id, ladder, team, damage scale, ping, flags.
    // No call to NetworkObjectClass::Export_Rare (it is empty).
    override fun exportRare(packet: BitStream) {
        packet.addInt(id)                // Id
        packet.addInt(ladderPoints)      // LadderPoints
        packet.addInt(team)              // PlayerType (0=NOD, 1=GDI)
        packet.addInt(damageScaleFactor) // DamageScaleFactor (int, default 100)
        packet.addInt(ping)              // Ping
        packet.addBool(isInGame)         // IsInGame
        packet.addBool(invulnerable)     // Invulnerable
        packet.addBool(isActive)         // IsActive
        packet.addInt(wolRank)           // WolRank
        packet.addInt(numWolGames)       // NumWolGames
    }

    // C++: cPlayer::Export_Occasional (player.cpp:971) — calls PlayerDataClass::Export_Occasional
    // (Score, Money) then writes Kills and Deaths.
    override fun exportOccasional(packet: BitStream) {
        packet.addFloat(score)  // Score   (PlayerDataClass::Export_Occasional)
        packet.addFloat(money)  // Money   (PlayerDataClass::Export_Occasional)
        packet.addInt(kills)    // Kills
        packet.addInt(deaths)   // Deaths
    }

    // C++: cPlayer::Set_Ladder_Points — marks BIT_RARE
    fun setLadderPoints(points: Int) {
        ladderPoints = points
        setObjectDirtyBit(NetworkObject.BIT_RARE, true)
    }

    // C++: cPlayer::Set_Damage_Scale_Factor — marks BIT_RARE
    fun setDamageScaleFactor(factor: Int) {
        damageScaleFactor = factor
        setObjectDirtyBit(NetworkObject.BIT_RARE, true)
    }

    // C++: cPlayer::Set_Is_In_Game — marks BIT_RARE
    fun setIsInGame(state: Boolean) {
        isInGame = state
        setObjectDirtyBit(NetworkObject.BIT_RARE, true)
    }

    // C++: cPlayer::Set_Is_Active — marks BIT_RARE
    fun setIsActive(flag: Boolean) {
        isActive = flag
        setObjectDirtyBit(NetworkObject.BIT_RARE, true)
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

    // C++: cPlayer::Set_Kills — marks BIT_OCCASIONAL
    fun setKills(newKills: Int) {
        kills = newKills
        setObjectDirtyBit(NetworkObject.BIT_OCCASIONAL, true)
    }

    // C++: cPlayer::Increment_Kills
    fun incrementKills() {
        setKills(kills + 1)
    }

    // C++: cPlayer::Set_Deaths — marks BIT_OCCASIONAL
    fun setDeaths(newDeaths: Int) {
        deaths = newDeaths
        setObjectDirtyBit(NetworkObject.BIT_OCCASIONAL, true)
    }

    // C++: cPlayer::Increment_Deaths
    fun incrementDeaths() {
        setDeaths(deaths + 1)
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
