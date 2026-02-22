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
}
