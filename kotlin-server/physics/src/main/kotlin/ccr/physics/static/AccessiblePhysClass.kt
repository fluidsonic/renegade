package ccr.physics.static

/**
 * C++: AccessiblePhysClass (wwphys/accessiblephys.h/.cpp)
 *
 * A static animated physics object that supports lock-code based access control.
 * Parent of DoorPhysClass and ElevatorPhysClass.
 */
open class AccessiblePhysClass : StaticAnimPhysClass() {

    /** C++: int LockCode — 0 means unlocked, non-zero is a lock code requiring a matching key. */
    var lockCode: Int = 0

    /** C++: bool Can_Unlock(int key_mask) — returns true if the key mask matches the lock code. */
    fun canUnlock(keyMask: Int): Boolean = ((1 shl lockCode) and keyMask) != 0

    override fun asAccessiblePhysClass(): AccessiblePhysClass? = this

    companion object {
        // Chunk IDs from accessiblephys.cpp
        const val CHUNKID_PARENT: Int = 0x10311235
        const val CHUNKID_VARIABLES: Int = 0x10311236  // CHUNKID_PARENT + 1

        const val VARID_LOCKCODE: Int = 1
    }
}
