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

// C→S typed factory — creates actual instances for server-side processing.
// Registered at wire classId (header + 1) because the Renegade client sends +1 offset.
class CsEventFactory(override val classId: Int, private val supplier: () -> NetworkObject) : NetworkObjectFactory {
    override fun create(packet: BitStream): NetworkObject = supplier()
}

// C++: static factory instances registered in constructors (one per NetworkObjectClass subclass)
// Kotlin equivalent: explicit registration called at server startup.
object NetworkObjectFactories {
    private var registered = false

    // Call once at server startup to register all factory instances.
    // C++: done automatically via static constructor chains; here done explicitly.
    fun register() {
        if (registered) return
        registered = true

        // classId=1000: game objects — factory writes/reads definitionId
        NetworkObjectFactoryManager.register(NetworkGameObjectFactory())

        // classId=0: no-op factory for static/singleton objects (BaseControllerClass, ServerFps, etc.)
        // In C++, classId=0 objects have no Prep_Packet — this matches that behavior.
        NetworkObjectFactoryManager.register(SimpleNetworkObjectFactory(0))

        // S→C event classIds (1001–1016): server never creates from incoming — keep no-op factories
        for (classId in 1001..1016) {
            NetworkObjectFactoryManager.register(SimpleNetworkObjectFactory(classId))
        }

        // C→S event/object factories — wire classId = header + 1
        // The Renegade client sends classId at +1 offset from the netclassids.h constant.
        NetworkObjectFactoryManager.register(CsEventFactory(1018) { ClientControl() })
        NetworkObjectFactoryManager.register(CsEventFactory(1019) { CsTextObj() })
        NetworkObjectFactoryManager.register(CsEventFactory(1020) { SuicideEvent() })
        NetworkObjectFactoryManager.register(CsEventFactory(1021) { ChangeTeamEvent() })
        NetworkObjectFactoryManager.register(CsEventFactory(1022) { MoneyEvent() })
        // 1023 = WarpEvent (C→S, header=1022, wire=1023 — currently unhandled, register no-op)
        NetworkObjectFactoryManager.register(SimpleNetworkObjectFactory(1023))
        NetworkObjectFactoryManager.register(CsEventFactory(1024) { PurchaseRequestEvent() })
        NetworkObjectFactoryManager.register(CsEventFactory(1025) { ClientGoodbyeEvent() })
        NetworkObjectFactoryManager.register(CsEventFactory(1026) { BioEvent() })
        NetworkObjectFactoryManager.register(CsEventFactory(1027) { LoadingEvent() })
        NetworkObjectFactoryManager.register(CsEventFactory(1028) { GodModeEvent() })
        NetworkObjectFactoryManager.register(CsEventFactory(1029) { VipModeEvent() })
        NetworkObjectFactoryManager.register(CsEventFactory(1030) { ScoreEvent() })
        NetworkObjectFactoryManager.register(CsEventFactory(1031) { ClientBboEvent() })
        NetworkObjectFactoryManager.register(CsEventFactory(1032) { ClientFps() })
        NetworkObjectFactoryManager.register(CsEventFactory(1033) { CsPingRequestEvent() })
        NetworkObjectFactoryManager.register(CsEventFactory(1034) { CsDamageEvent() })
        NetworkObjectFactoryManager.register(CsEventFactory(1035) { RequestKillEvent() })
        NetworkObjectFactoryManager.register(CsEventFactory(1036) { CsConsoleCommandEvent() })
        NetworkObjectFactoryManager.register(CsEventFactory(1037) { CsHint() })
        NetworkObjectFactoryManager.register(CsEventFactory(1038) { CsAnnouncement() })
        NetworkObjectFactoryManager.register(CsEventFactory(1039) { DonateEvent() })
    }
}
