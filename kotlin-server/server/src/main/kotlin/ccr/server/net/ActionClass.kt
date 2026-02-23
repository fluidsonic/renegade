package ccr.server.net

// C++: ActionClass (action.h / action.cpp) — minimal server port
class ActionClass {

    // C++: ActionCodeClass* ActionCode — current executing action
    private var actionCode: ActionCodeClass? = null

    // C++: ActionParamsStruct& Get_Parameters()
    fun getParameters(): ActionParamsStruct = ActionParamsStruct()

    // C++: void Begin_Hibernation()
    fun beginHibernation() {
        actionCode?.beginHibernation()
    }

    // C++: void End_Hibernation()
    fun endHibernation() {
        actionCode?.endHibernation()
    }

    // C++: bool Is_Animating() — used in PhysicalGameObj::postThink
    fun isAnimating(): Boolean = false
}

// C++: ActionParamsStruct (actionparams.h) — minimal port
class ActionParamsStruct {
    var observerId: Int = 0
    var priority: Int = 0
    var waypathId: Int = 0
}

// C++: ActionCodeClass — base class for action implementations
abstract class ActionCodeClass {
    open fun beginHibernation() {}
    open fun endHibernation() {}
}
