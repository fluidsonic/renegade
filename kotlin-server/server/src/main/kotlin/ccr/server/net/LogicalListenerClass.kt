package ccr.server.net

import ccr.math.Vector3

// C++: LogicalListenerClass (WWAudio/LogicalListener.h) — minimal server stub
// On server, logical sound listeners are no-ops (no audio system).
class LogicalListenerClass {

    // C++: void Add_To_Scene(bool start_playing = true)
    fun addToScene(startPlaying: Boolean = true) {
        // server-side stub — no audio scene
    }

    // C++: void Remove_From_Scene()
    fun removeFromScene() {
        // server-side stub — no audio scene
    }

    // C++: void Set_Position(const Vector3&)
    var position: Vector3 = Vector3(0f, 0f, 0f)
}
