package ccr.net

// C++: SysTimeClass in wwlib/systimer.h / systimer.cpp
// Wraps the system millisecond timer and handles 32-bit wrap-around.
// TIMEGETTIME macro expands to SystemTime.get()

// C++: extern SysTimeClass SystemTime
val SystemTime = SysTimeClass()

// C++: #define TIMEGETTIME SystemTime.Get
fun timeGetTime(): UInt = SystemTime.get()

class SysTimeClass {

    // C++: unsigned long StartTime
    private var startTime: UInt = 0u

    // C++: unsigned long WrapAdd
    private var wrapAdd: UInt = 0u

    // C++: Get() — ms since first call; handles wrap-around
    fun get(): UInt {
        val time = System.currentTimeMillis().toUInt()
        return if (time > startTime) {
            time - startTime
        } else {
            // C++: timer wrapped around
            time + wrapAdd
        }
    }

    // C++: operator() and operator unsigned long — both delegate to Get()
    operator fun invoke(): UInt = get()

    // C++: Reset() — resets start time and wrap add
    fun reset() {
        startTime = System.currentTimeMillis().toUInt()
        wrapAdd = 0u - startTime
    }

    // C++: Is_Getting_Late() — true if near 32-bit overflow
    fun isGettingLate(): Boolean = get() > 0x6fffffffu

    init {
        reset()
    }
}
