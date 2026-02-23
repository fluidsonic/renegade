package ccr.server.net

import ccr.math.Vector3

// C++: LogicalSoundClass (WWAudio/LogicalSound.h)
// Represents a logical sound that affects gameplay.
class LogicalSoundClass {

    // C++: uint32 m_TypeMask
    var typeMask: Int = 0

    // C++: Vector3 m_Position
    var position: Vector3 = Vector3()

    // C++: RefCountClass* m_UserObj — stores creator reference
    var userObj: ScriptableGameObj? = null

    // C++: uint32 Get_Type_Mask()
    fun getTypeMask(): Int = typeMask

    // C++: Vector3 Get_Position()
    fun getPosition(): Vector3 = position

    // C++: RefCountClass* Peek_User_Obj()
    fun peekUserObj(): ScriptableGameObj? = userObj
}
