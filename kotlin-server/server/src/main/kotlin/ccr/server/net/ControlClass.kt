package ccr.server.net

import ccr.net.bitstream.BitStream

// C++: ControlClass (control.h / control.cpp)
class ControlClass {

    enum class BooleanControl(val bit: Int) {
        // C++: BOOLEAN_ONE_TIME_FIRST = BOOLEAN_JUMP
        JUMP(0),
        WEAPON_NEXT(1),
        WEAPON_PREV(2),
        WEAPON_RELOAD(3),
        WEAPON_USE(4),
        DIVE_FORWARD(5),
        DIVE_BACKWARD(6),
        DIVE_LEFT(7),
        DIVE_RIGHT(8),
        ACTION(9),
        SELECT_NO_WEAPON(10),
        SELECT_WEAPON_0(11),
        SELECT_WEAPON_1(12),
        SELECT_WEAPON_2(13),
        SELECT_WEAPON_3(14),
        SELECT_WEAPON_4(15),
        SELECT_WEAPON_5(16),
        SELECT_WEAPON_6(17),
        SELECT_WEAPON_7(18),
        SELECT_WEAPON_8(19),
        SELECT_WEAPON_9(20),
        DROP_FLAG(21),
        VEHICLE_TOGGLE_GUNNER(22),
        // C++: BOOLEAN_CONTINUOUS_FIRST
        WEAPON_FIRE_PRIMARY(23),
        WEAPON_FIRE_SECONDARY(24),
        WALK(25),
        CROUCH(26),
    }

    enum class AnalogControl { MOVE_FORWARD, MOVE_LEFT, MOVE_UP, TURN_LEFT }

    companion object {
        const val NUM_BOOLEAN_ONE_TIME   = 23  // BOOLEAN_CONTINUOUS_FIRST - BOOLEAN_ONE_TIME_FIRST
        const val NUM_BOOLEAN_CONTINUOUS = 4   // BOOLEAN_TOTAL - BOOLEAN_CONTINUOUS_FIRST
        const val ANALOG_CONTROL_COUNT   = 4
    }

    // C++: unsigned long OneTimeBooleanBits
    var oneTimeBooleanBits: UInt = 0u
        private set

    // C++: unsigned long PendingOneTimeBooleanBits
    private var pendingOneTimeBooleanBits: UInt = 0u

    // C++: unsigned char ContinuousBooleanBits
    var continuousBooleanBits: UByte = 0u
        private set

    // C++: unsigned char PendingContinuousBooleanBits
    private var pendingContinuousBooleanBits: UByte = 0u

    // C++: float AnalogValues[ANALOG_CONTROL_COUNT]
    private val analogValues = FloatArray(ANALOG_CONTROL_COUNT)

    // C++: void Clear_Control()
    fun clearControl() {
        oneTimeBooleanBits = 0u
        pendingOneTimeBooleanBits = 0u
        continuousBooleanBits = 0u
        pendingContinuousBooleanBits = 0u
        analogValues.fill(0f)
    }

    // C++: void Clear_Boolean()
    fun clearBoolean() {
        oneTimeBooleanBits = 0u
        continuousBooleanBits = 0u
    }

    // C++: void Clear_One_Time_Boolean() { OneTimeBooleanBits = 0; }
    fun clearOneTimeBoolean() { oneTimeBooleanBits = 0u }

    // C++: void Set_Boolean(BooleanControl control, bool state)
    fun setBoolean(control: BooleanControl, state: Boolean = true) {
        if (control.bit < NUM_BOOLEAN_ONE_TIME) {
            if (state) pendingOneTimeBooleanBits = pendingOneTimeBooleanBits or (1u shl control.bit)
        } else {
            val bit = control.bit - NUM_BOOLEAN_ONE_TIME
            if (state) pendingContinuousBooleanBits = (pendingContinuousBooleanBits.toInt() or (1 shl bit)).toUByte()
            else        pendingContinuousBooleanBits = (pendingContinuousBooleanBits.toInt() and (1 shl bit).inv()).toUByte()
        }
    }

    // C++: bool Get_Boolean(BooleanControl control)
    fun getBoolean(control: BooleanControl): Boolean {
        return if (control.bit < NUM_BOOLEAN_ONE_TIME)
            (oneTimeBooleanBits and (1u shl control.bit)) != 0u
        else
            (continuousBooleanBits.toInt() and (1 shl (control.bit - NUM_BOOLEAN_ONE_TIME))) != 0
    }

    // C++: void Set_Analog(AnalogControl control, float value)
    fun setAnalog(control: AnalogControl, value: Float) { analogValues[control.ordinal] = value }

    // C++: float Get_Analog(AnalogControl control)
    fun getAnalog(control: AnalogControl): Float = analogValues[control.ordinal]

    // C++: void Import_Cs(BitStreamClass& packet) — #if 01 branch
    fun importCs(packet: BitStream) {
        val otbBits = packet.getInt(BITPACK_ONE_TIME_BOOLEAN_BITS).toUInt()
        oneTimeBooleanBits = oneTimeBooleanBits or otbBits
        continuousBooleanBits = packet.getByte(BITPACK_CONTINUOUS_BOOLEAN_BITS).toUByte()
        analogValues[AnalogControl.MOVE_FORWARD.ordinal] = packet.getFloat(BITPACK_ANALOG_VALUES)
        analogValues[AnalogControl.MOVE_LEFT.ordinal]    = packet.getFloat(BITPACK_ANALOG_VALUES)
        analogValues[AnalogControl.MOVE_UP.ordinal]      = packet.getFloat(BITPACK_ANALOG_VALUES)
        analogValues[AnalogControl.TURN_LEFT.ordinal]    = packet.getFloat(BITPACK_ANALOG_VALUES)
    }

    // C++: void Export_Cs(BitStreamClass& packet) — #if 01 branch
    fun exportCs(packet: BitStream) {
        packet.addInt(pendingOneTimeBooleanBits.toInt(), BITPACK_ONE_TIME_BOOLEAN_BITS)
        pendingOneTimeBooleanBits = 0u
        packet.addByte(pendingContinuousBooleanBits.toByte(), BITPACK_CONTINUOUS_BOOLEAN_BITS)
        pendingContinuousBooleanBits = 0u
        packet.addFloat(analogValues[AnalogControl.MOVE_FORWARD.ordinal], BITPACK_ANALOG_VALUES)
        packet.addFloat(analogValues[AnalogControl.MOVE_LEFT.ordinal],    BITPACK_ANALOG_VALUES)
        packet.addFloat(analogValues[AnalogControl.MOVE_UP.ordinal],      BITPACK_ANALOG_VALUES)
        packet.addFloat(analogValues[AnalogControl.TURN_LEFT.ordinal],    BITPACK_ANALOG_VALUES)
    }

    // C++: void Import_Sc(BitStreamClass& packet) — #if 01 branch
    fun importSc(packet: BitStream) {
        continuousBooleanBits = packet.getByte(BITPACK_CONTINUOUS_BOOLEAN_BITS).toUByte()
        analogValues[AnalogControl.MOVE_FORWARD.ordinal] = packet.getFloat(BITPACK_ANALOG_VALUES)
        analogValues[AnalogControl.MOVE_LEFT.ordinal]    = packet.getFloat(BITPACK_ANALOG_VALUES)
        analogValues[AnalogControl.MOVE_UP.ordinal]      = packet.getFloat(BITPACK_ANALOG_VALUES)
        analogValues[AnalogControl.TURN_LEFT.ordinal]    = packet.getFloat(BITPACK_ANALOG_VALUES)
    }

    // C++: void Export_Sc(BitStreamClass& packet) — #if 01 branch
    fun exportSc(packet: BitStream) {
        packet.addByte(continuousBooleanBits.toByte(), BITPACK_CONTINUOUS_BOOLEAN_BITS)
        packet.addFloat(analogValues[AnalogControl.MOVE_FORWARD.ordinal], BITPACK_ANALOG_VALUES)
        packet.addFloat(analogValues[AnalogControl.MOVE_LEFT.ordinal],    BITPACK_ANALOG_VALUES)
        packet.addFloat(analogValues[AnalogControl.MOVE_UP.ordinal],      BITPACK_ANALOG_VALUES)
        packet.addFloat(analogValues[AnalogControl.TURN_LEFT.ordinal],    BITPACK_ANALOG_VALUES)
    }
}
