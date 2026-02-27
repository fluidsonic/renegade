package ccr.server.net

import ccr.net.bitstream.BitStream
import ccr.net.replication.NetworkObject
import ccr.net.replication.NetworkObjectFactory
import ccr.net.replication.NetworkObjectFactoryManager

// C++: NetworkGameObjectFactoryClass (basegameobj.cpp:54-106)
// Factory for all BaseGameObj-derived objects (networkClassId = NETCLASSID_GAMEOBJ = 1000).
// Writes/reads definitionId so the client can instantiate the correct game object subtype.
class NetworkGameObjectFactory : NetworkObjectFactory {
    override val classId: Int = 1000  // NETCLASSID_GAMEOBJ

    // C++: Prep_Packet — writes definition ID before Export_Creation so client knows which type to create
    override fun prepPacket(obj: NetworkObject, packet: BitStream) {
        val gameObj = obj as BaseGameObj
        packet.addInt(gameObj.definitionId)
    }

    // C++: Create — reads definitionId and creates via DefinitionMgrClass (client-side only)
    // Server-only mode: never called on server
    override fun create(packet: BitStream): NetworkObject? {
        packet.getInt()  // consume definitionId from stream
        return null       // client-side creation not implemented
    }
}

// C++: SimpleNetworkObjectFactoryClass<T, CLASS_ID>
// Stateless factory for event and non-game-object NetworkObjects (no Prep_Packet needed).
// prepPacket writes nothing; create is server-only no-op.
class SimpleNetworkObjectFactory(override val classId: Int) : NetworkObjectFactory {
    override fun create(packet: BitStream): NetworkObject? = null  // server-only: never called
    // prepPacket inherited from interface — default no-op body
}

// C++: static factory instances registered in constructors (one per NetworkObjectClass subclass)
// Kotlin equivalent: explicit registration called at server startup.
object NetworkObjectFactories {
    private var registered = false

    // Call once at server startup to register all S→C factory instances.
    // C++: done automatically via static constructor chains; here done explicitly.
    fun register() {
        if (registered) return
        registered = true

        // classId=1000: game objects — factory writes/reads definitionId
        NetworkObjectFactoryManager.register(NetworkGameObjectFactory())

        // classId=0: no-op factory for static/singleton objects (BaseControllerClass, ServerFps, etc.)
        // In C++, classId=0 objects have no Prep_Packet — this matches that behavior.
        NetworkObjectFactoryManager.register(SimpleNetworkObjectFactory(0))

        // classId=1001–1038: all event/object classIds — no extra packet data (no Prep_Packet)
        // Registers both S→C (1001–1016) and C→S (1017–1038) classIds.
        for (classId in 1001..1038) {
            NetworkObjectFactoryManager.register(SimpleNetworkObjectFactory(classId))
        }
    }
}
