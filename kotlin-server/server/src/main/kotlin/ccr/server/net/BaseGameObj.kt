package ccr.server.net

import ccr.net.bitstream.BitStream
import ccr.net.replication.NetworkObject

// C++: BaseGameObjClass (basegameobj.cpp)
// Base for all network game objects. networkClassId = NETCLASSID_GAMEOBJ = 1000.
// NetworkGameObjectFactoryClass::Prep_Packet writes definitionId before Export_Creation.
abstract class BaseGameObj(val definitionId: Int) : NetworkObject() {

    override val networkClassId: Int = 1000
    override fun delete() {}

    init {
        setObjectDirtyBit(BIT_CREATION, true)
    }

    // C++: BaseGameObj::Think — base impl; subclasses override to add behaviour
    open fun think(deltaSeconds: Float) {}

    // C++: BaseGameObj::Post_Think — default no-op
    open fun postThink() {}
}
