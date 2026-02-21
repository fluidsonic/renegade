package ccr.physics.rigidbody

import ccr.math.Quaternion
import ccr.math.Vector3
import ccr.physics.ode.StateVector
import kotlin.math.abs
import kotlin.test.*

class RigidBodyStateTest {

    @Test
    fun toVectorFromVectorRoundTrip() {
        val state = RigidBodyState(
            position = Vector3(1f, 2f, 3f),
            orientation = Quaternion(0.1f, 0.2f, 0.3f, 0.9f).normalized(),
            linearMomentum = Vector3(4f, 5f, 6f),
            angularMomentum = Vector3(7f, 8f, 9f),
        )
        val sv = StateVector()
        state.toVector(sv)
        assertEquals(13, sv.count)
        val restored = RigidBodyState.fromVector(sv, 0)
        assertTrue(abs(restored.position.x - state.position.x) < 1e-5f)
        assertTrue(abs(restored.position.y - state.position.y) < 1e-5f)
        assertTrue(abs(restored.position.z - state.position.z) < 1e-5f)
        assertTrue(abs(restored.linearMomentum.x - state.linearMomentum.x) < 1e-5f)
        assertTrue(abs(restored.angularMomentum.z - state.angularMomentum.z) < 1e-5f)
    }

    @Test
    fun lerpAtZeroReturnsFirst() {
        val a = RigidBodyState(position = Vector3(0f, 0f, 0f))
        val b = RigidBodyState(position = Vector3(10f, 10f, 10f))
        val result = a.lerp(b, 0f)
        assertEquals(0f, result.position.x)
    }

    @Test
    fun lerpAtOneReturnsSecond() {
        val a = RigidBodyState(position = Vector3(0f, 0f, 0f))
        val b = RigidBodyState(position = Vector3(10f, 10f, 10f))
        val result = a.lerp(b, 1f)
        assertTrue(abs(result.position.x - 10f) < 1e-5f)
    }

    @Test
    fun lerpAtHalfReturnsMidpoint() {
        val a = RigidBodyState(
            position = Vector3(0f, 0f, 0f),
            linearMomentum = Vector3(0f, 0f, 0f),
        )
        val b = RigidBodyState(
            position = Vector3(10f, 0f, 0f),
            linearMomentum = Vector3(20f, 0f, 0f),
        )
        val result = a.lerp(b, 0.5f)
        assertTrue(abs(result.position.x - 5f) < 1e-5f)
        assertTrue(abs(result.linearMomentum.x - 10f) < 1e-5f)
    }
}
