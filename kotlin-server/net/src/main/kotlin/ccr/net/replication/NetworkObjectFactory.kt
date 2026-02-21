package ccr.net.replication

import ccr.net.bitstream.BitStream

// C++: NetworkObjectFactoryClass / NetworkObjectFactoryMgrClass in wwnet/networkobjectfactory.h
// Factories act as virtual constructors: given a class ID, create the right NetworkObject.

// C++: NetworkObjectFactoryClass — interface for object creation
interface NetworkObjectFactory {
    // C++: Get_Class_ID — identifies which class this factory creates
    val classId: Int

    // C++: Create(packet) — creates a new object, reading initial data from the packet
    fun create(packet: BitStream): NetworkObject?

    // C++: Prep_Packet — optional: prepare the packet before sending to a client
    fun prepPacket(obj: NetworkObject, packet: BitStream) {}
}

// C++: NetworkObjectFactoryMgrClass — registry of all factories
object NetworkObjectFactoryManager {
    private val factories = mutableMapOf<Int, NetworkObjectFactory>()

    // C++: Registers a factory (done automatically in C++ via constructor chain)
    fun register(factory: NetworkObjectFactory) {
        factories[factory.classId] = factory
    }

    fun unregister(classId: Int) {
        factories.remove(classId)
    }

    // C++: Find_Factory / Create — find the factory for classId and create an object
    fun create(classId: Int, packet: BitStream): NetworkObject? {
        return factories[classId]?.create(packet)
    }

    fun getFactory(classId: Int): NetworkObjectFactory? = factories[classId]
}
