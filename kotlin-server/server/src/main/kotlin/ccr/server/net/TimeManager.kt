package ccr.server.net

// C++: TimeManager (timemanager.h / timemanager.cpp)
// Global time tracking — frame seconds set at top of each game loop tick.
object TimeManager {

    // C++: static float FrameSeconds
    private var frameSeconds: Float = 0f

    // C++: float Get_Frame_Seconds()
    fun getFrameSeconds(): Float = frameSeconds

    // C++: void Set_Frame_Seconds(float seconds)
    fun setFrameSeconds(seconds: Float) { frameSeconds = seconds }
}
