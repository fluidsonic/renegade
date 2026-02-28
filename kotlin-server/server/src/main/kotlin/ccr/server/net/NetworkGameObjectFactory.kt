package ccr.server.net

import ccr.net.bitstream.BitStream
import ccr.net.replication.NetworkObject
import ccr.net.replication.NetworkObjectFactory
import ccr.net.replication.NetworkObjectFactoryManager
import ccr.server.NetClassIds

// C++: NetworkGameObjectFactoryClass (basegameobj.cpp:54-106)
// Factory for all BaseGameObj-derived objects (networkClassId = NETCLASSID_GAMEOBJ = 1000).
// Writes/reads definitionId so the client can instantiate the correct game object subtype.
class NetworkGameObjectFactory : NetworkObjectFactory {
    override val classId: Int = NetClassIds.NETCLASSID_GAMEOBJ

    // C++: Prep_Packet — writes definition ID before Export_Creation so client knows which type to create
    override fun prepPacket(obj: NetworkObject, packet: BitStream) {
        val gameObj = obj as BaseGameObj
        packet.addInt(gameObj.definitionId)
    }

    // C++: Create — reads definitionId and creates via DefinitionMgrClass (client-side only)
    // Server-only mode: never called on server
    override fun create(packet: BitStream): NetworkObject? {
        val definitionId = packet.getInt()
        // FIXME: look up definition by ID and create the appropriate game object instance
        throw NotImplementedError("NetworkGameObjectFactory.create: no definition found for definitionId=$definitionId")
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

        // S→C event classIds (NETCLASSID_SCTEXTOBJ..NETCLASSID_SCANNOUNCEMENT):
        // server never creates from incoming — keep no-op factories.
        for (classId in NetClassIds.NETCLASSID_SCTEXTOBJ..NetClassIds.NETCLASSID_SCANNOUNCEMENT) {
            NetworkObjectFactoryManager.register(SimpleNetworkObjectFactory(classId))
        }

        // C→S event/object factories — one per netclassids.h constant, no offset
        NetworkObjectFactoryManager.register(CsEventFactory(NetClassIds.NETCLASSID_CLIENTCONTROL)         { ClientControl() })
        NetworkObjectFactoryManager.register(CsEventFactory(NetClassIds.NETCLASSID_CSTEXTOBJ)             { CsTextObj() })
        NetworkObjectFactoryManager.register(CsEventFactory(NetClassIds.NETCLASSID_SUICIDEEVENT)          { SuicideEvent() })
        NetworkObjectFactoryManager.register(CsEventFactory(NetClassIds.NETCLASSID_CHANGETEAMEVENT)       { ChangeTeamEvent() })
        NetworkObjectFactoryManager.register(CsEventFactory(NetClassIds.NETCLASSID_MONEYEVENT)            { MoneyEvent() })
        NetworkObjectFactoryManager.register(CsEventFactory(NetClassIds.NETCLASSID_WARPEVENT)             { WarpEvent() })
        NetworkObjectFactoryManager.register(CsEventFactory(NetClassIds.NETCLASSID_PURCHASEREQUESTEVENT)  { PurchaseRequestEvent() })
        NetworkObjectFactoryManager.register(CsEventFactory(NetClassIds.NETCLASSID_CLIENTGOODBYEEVENT)    { ClientGoodbyeEvent() })
        NetworkObjectFactoryManager.register(CsEventFactory(NetClassIds.NETCLASSID_BIOEVENT)              { BioEvent() })
        NetworkObjectFactoryManager.register(CsEventFactory(NetClassIds.NETCLASSID_LOADINGEVENT)          { LoadingEvent() })
        NetworkObjectFactoryManager.register(CsEventFactory(NetClassIds.NETCLASSID_GODMODEEVENT)          { GodModeEvent() })
        NetworkObjectFactoryManager.register(CsEventFactory(NetClassIds.NETCLASSID_VIPMODEEVENT)          { VipModeEvent() })
        NetworkObjectFactoryManager.register(CsEventFactory(NetClassIds.NETCLASSID_SCOREEVENT)            { ScoreEvent() })
        NetworkObjectFactoryManager.register(CsEventFactory(NetClassIds.NETCLASSID_CLIENTBBOEVENT)        { ClientBboEvent() })
        NetworkObjectFactoryManager.register(CsEventFactory(NetClassIds.NETCLASSID_CLIENTFPS)             { ClientFps() })
        NetworkObjectFactoryManager.register(CsEventFactory(NetClassIds.NETCLASSID_CSPINGREQUESTEVENT)    { CsPingRequestEvent() })
        NetworkObjectFactoryManager.register(CsEventFactory(NetClassIds.NETCLASSID_CSDAMAGEEVENT)         { CsDamageEvent() })
        NetworkObjectFactoryManager.register(CsEventFactory(NetClassIds.NETCLASSID_REQUESTKILLEVENT)      { RequestKillEvent() })
        NetworkObjectFactoryManager.register(CsEventFactory(NetClassIds.NETCLASSID_CSCONSOLECOMMANDEVENT) { CsConsoleCommandEvent() })
        NetworkObjectFactoryManager.register(CsEventFactory(NetClassIds.NETCLASSID_CSHINT)                { CsHint() })
        NetworkObjectFactoryManager.register(CsEventFactory(NetClassIds.NETCLASSID_CSANNOUNCEMENT)        { CsAnnouncement() })
        NetworkObjectFactoryManager.register(CsEventFactory(NetClassIds.NETCLASSID_DONATEEVENT)           { DonateEvent() })
    }
}
