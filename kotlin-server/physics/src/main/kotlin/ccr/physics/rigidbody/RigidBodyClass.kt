package ccr.physics.rigidbody

import ccr.math.Matrix3
import ccr.math.Matrix3D
import ccr.math.Quaternion
import ccr.math.Vector3
import ccr.physics.PhysicsConstants
import ccr.physics.moveable.MoveablePhysClass
import ccr.physics.ode.Integrator
import ccr.physics.ode.OdeSystem
import ccr.physics.ode.StateVector

// C++: RigidBodyClass in wwphys/rbody.h
abstract class RigidBodyClass : MoveablePhysClass(), OdeSystem {

    companion object {
        const val SLEEP_DELAY = 0.5f
        private const val SLEEP_VEL_THRESHOLD = 0.1f
        private const val SLEEP_ANGVEL_THRESHOLD = 0.1f
        private const val MIN_CONTACT_COUNT_FOR_SLEEP = 3
        const val MAX_CONTACT_POINTS = 10

        // Chunk IDs sourced from rbody.cpp Save()
        const val RBODY_CHUNK_MOVEABLE: Int = 0x00800900           // wraps parent MoveablePhysClass
        const val RBODY_CHUNK_VARIABLES: Int = 0x00800901           // own data

        // Micro-chunk IDs within RBODY_CHUNK_VARIABLES:
        const val RBODY_VARIABLE_ODESYSTEM_PTR: Int = 0x00
        const val RBODY_VARIABLE_IBODY: Int = 0x01
        const val RBODY_VARIABLE_IBODYINV: Int = 0x02
        const val RBODY_VARIABLE_STATE_POSITION: Int = 0x03
        const val RBODY_VARIABLE_STATE_ORIENTATION: Int = 0x04
        const val RBODY_VARIABLE_STATE_LMOMENTUM: Int = 0x05
        const val RBODY_VARIABLE_STATE_AMOMENTUM: Int = 0x06
        // 0x07 = OBSOLETE CONTACT_STIFFNESS
        // 0x08 = OBSOLETE CONTACT_DAMPING
        const val RBODY_VARIABLE_CONTACT_LENGTH: Int = 0x09
    }

    var state: RigidBodyState = RigidBodyState()

    // C++: RigidBodyClass::Get_Orientation() — returns State.Orientation
    val orientation: Quaternion get() = state.orientation

    // Derived quantities (recomputed after integration)
    var rotation: Matrix3 = Matrix3.IDENTITY
        private set
    var iInv: Matrix3 = Matrix3.IDENTITY
        private set
    var angularVelocity: Vector3 = Vector3.ZERO
        private set

    // Body-space inertia (C++: IBody, IBodyInv)
    var iBody: Matrix3 = Matrix3.IDENTITY
    var iBodyInv: Matrix3 = Matrix3.IDENTITY

    // Contact state (C++: ContactThickness, IsInContact, ContactCount, ContactPoint[], ContactNormal, StickCount)
    var contactThickness: Float = 0f
    var isInContact: Boolean = false
    val contactPoints = Array(MAX_CONTACT_POINTS) { Vector3.ZERO }
    var contactNormal: Vector3 = Vector3(0f, 0f, 1f)
    var contactCount: Int = 0
    var stickCount: Int = 0

    // Sleep (C++: GoToSleepTimer)
    var goToSleepTimer: Float = 0f

    // Timestep tracking (C++: LastTimestep)
    var lastTimestep: Float = 0f

    // Forces accumulated for current integration step
    private var accumulatedForce: Vector3 = Vector3.ZERO
    private var accumulatedTorque: Vector3 = Vector3.ZERO

    override fun asRigidBodyClass(): RigidBodyClass = this

    fun updateAuxiliaryState() {
        rotation = Matrix3.fromQuaternion(state.orientation)
        iInv = rotation * iBodyInv * rotation.transpose()
        velocity = state.linearMomentum * massInv
        angularVelocity = iInv * state.angularMomentum
        // Update transform
        val m = Matrix3D.fromQuaternion(state.orientation).withTranslation(state.position)
        transform = m
    }

    fun computeInertia(dx: Float, dy: Float, dz: Float) {
        val m = mass
        val ix = m * (dy * dy + dz * dz) / 12f
        val iy = m * (dx * dx + dz * dz) / 12f
        val iz = m * (dx * dx + dy * dy) / 12f
        iBody = Matrix3(ix, 0f, 0f, 0f, iy, 0f, 0f, 0f, iz)
        iBodyInv = Matrix3(
            if (ix > 0f) 1f / ix else 0f, 0f, 0f,
            0f, if (iy > 0f) 1f / iy else 0f, 0f,
            0f, 0f, if (iz > 0f) 1f / iz else 0f,
        )
    }

    // ======================== OdeSystem ========================

    override fun getState(out: StateVector) {
        state.toVector(out)
    }

    override fun setState(state: StateVector, startIndex: Int): Int {
        this.state = RigidBodyState.fromVector(state, startIndex)
        return startIndex + RigidBodyState.FLOAT_COUNT
    }

    override fun computeDerivatives(t: Float, testState: StateVector?, dydt: StateVector, startIndex: Int): Int {
        // If testState is provided, temporarily load it to compute forces
        val savedState = state
        if (testState != null) {
            state = RigidBodyState.fromVector(testState, startIndex)
            updateAuxiliaryState()
        }

        // Compute forces
        accumulatedForce = Vector3.ZERO
        accumulatedTorque = Vector3.ZERO
        computeForceAndTorque()

        var idx = startIndex
        // dPosition/dt = velocity
        dydt[idx++] = velocity.x
        dydt[idx++] = velocity.y
        dydt[idx++] = velocity.z
        // dOrientation/dt = 0.5 * omega (x) q
        val omega = Quaternion(angularVelocity.x, angularVelocity.y, angularVelocity.z, 0f)
        val dq = omega * state.orientation
        dydt[idx++] = dq.x * 0.5f
        dydt[idx++] = dq.y * 0.5f
        dydt[idx++] = dq.z * 0.5f
        dydt[idx++] = dq.w * 0.5f
        // dLinearMomentum/dt = force
        dydt[idx++] = accumulatedForce.x
        dydt[idx++] = accumulatedForce.y
        dydt[idx++] = accumulatedForce.z
        // dAngularMomentum/dt = torque
        dydt[idx++] = accumulatedTorque.x
        dydt[idx++] = accumulatedTorque.y
        dydt[idx++] = accumulatedTorque.z

        // Restore state if we temporarily changed it
        if (testState != null) {
            state = savedState
            updateAuxiliaryState()
        }

        return idx
    }

    // ======================== FORCES ========================

    open fun computeForceAndTorque() {
        // Gravity
        val gravity = PhysicsConstants.gravityAcceleration * (mass * gravScale)
        accumulatedForce = accumulatedForce + gravity

        // Linear damping
        accumulatedForce = accumulatedForce - velocity * PhysicsConstants.linearDamping

        // Angular damping
        accumulatedTorque = accumulatedTorque - angularVelocity * PhysicsConstants.angularDamping
    }

    protected fun addForce(f: Vector3) {
        accumulatedForce = accumulatedForce + f
    }

    protected fun addTorque(t: Vector3) {
        accumulatedTorque = accumulatedTorque + t
    }

    // ======================== INTEGRATION ========================

    fun integrate(dt: Float) {
        Integrator.midpointIntegrate(this, 0f, dt)
        // Normalize orientation quaternion
        state = state.copy(orientation = state.orientation.normalized())
        updateAuxiliaryState()
        lastTimestep = dt
    }

    // ======================== IMPULSES ========================

    fun applyImpulse(impulse: Vector3) {
        state = state.copy(linearMomentum = state.linearMomentum + impulse)
        isAsleep = false
        goToSleepTimer = 0f
    }

    fun applyImpulse(impulse: Vector3, worldPos: Vector3) {
        state = state.copy(
            linearMomentum = state.linearMomentum + impulse,
            angularMomentum = state.angularMomentum + (worldPos - state.position).cross(impulse),
        )
        isAsleep = false
        goToSleepTimer = 0f
    }

    fun computePointVelocity(worldPos: Vector3): Vector3 =
        velocity + angularVelocity.cross(worldPos - state.position)

    // ======================== TIMESTEP ========================

    override fun timestep(dt: Float) {
        if (isAsleep || isDisabled) return
        clearContacts()
        integrate(dt)

        // Sleep logic
        if (velocity.length() < SLEEP_VEL_THRESHOLD &&
            angularVelocity.length() < SLEEP_ANGVEL_THRESHOLD &&
            contactCount >= MIN_CONTACT_COUNT_FOR_SLEEP
        ) {
            goToSleepTimer += dt
            if (goToSleepTimer >= SLEEP_DELAY) {
                isAsleep = true
            }
        } else {
            goToSleepTimer = 0f
        }
    }

    fun clearContacts() {
        contactCount = 0
    }

    fun addContact(point: Vector3, normal: Vector3) {
        if (contactCount < contactPoints.size) {
            contactPoints[contactCount] = point
            contactNormal = normal
            contactCount++
        }
    }
}
