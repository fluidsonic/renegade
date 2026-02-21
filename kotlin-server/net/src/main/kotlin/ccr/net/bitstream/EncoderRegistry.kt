package ccr.net.bitstream

// C++: cEncoderList / cEncoderTypeEntry in wwbitpack/encoderlist.h
// Maps encoder IDs to precision specs for quantized bit encoding.

const val MAX_ENCODER_TYPES = 100

data class EncoderEntry(
    // When set via setPrecision(bits): min=0, max=2^bits-1, resolution=1, bitPrecision=bits
    // When set via setPrecision(min, max, resolution): scaled to fewest bits needed.
    // NOTE: resolution is the ADJUSTED resolution = (max-min)/(max_units-1), not the input resolution.
    // This matches cEncoderTypeEntry::Calc_Bit_Precision in wwbitpack/encodertypeentry.cpp:151-186.
    val bitPrecision: Int,
    val min: Double = 0.0,
    val max: Double = 0.0,
    val resolution: Double = 1.0,
    val isRaw: Boolean = true,  // true = raw bit count, false = quantized range
) {
    val maxScaledValue: Long = (1L shl bitPrecision) - 1

    fun scale(value: Double): Long {
        return if (isRaw) {
            value.toLong() and maxScaledValue
        } else {
            val clamped = value.coerceIn(min, max)
            // C++: Round((value - Min) / Resolution)  (encodertypeentry.cpp:117)
            Math.round((clamped - min) / resolution).coerceIn(0, maxScaledValue)
        }
    }

    fun unscale(scaled: Long): Double {
        return if (isRaw) {
            scaled.toDouble()
        } else {
            min + scaled * resolution
        }
    }
}

// C++: cEncoderList — global singleton, maps bitpack IDs to EncoderEntry
object EncoderRegistry {
    var isCompressionEnabled: Boolean = true
        private set

    private val entries = arrayOfNulls<EncoderEntry>(MAX_ENCODER_TYPES)

    fun setCompressionEnabled(enabled: Boolean) {
        isCompressionEnabled = enabled
    }

    fun setPrecision(type: Int, numBits: Int) {
        require(type in 0 until MAX_ENCODER_TYPES)
        require(numBits in 1..32)
        entries[type] = EncoderEntry(bitPrecision = numBits, isRaw = true)
    }

    fun setPrecision(type: Int, min: Double, max: Double, resolution: Double = 1.0) {
        require(type in 0 until MAX_ENCODER_TYPES)
        require(max >= min)
        require(resolution > 0)

        // C++: cEncoderTypeEntry::Calc_Bit_Precision (wwbitpack/encodertypeentry.cpp:151-186)
        // MISCUTIL_EPSILON = 0.0001f (wwutil/miscutil.h:41)
        val epsilon = 1e-4
        val fUnits = Math.ceil((max - min) / resolution - epsilon) + 1.0
        val units = fUnits.toLong()

        var bitPrecision = 0
        var maxUnits = 0L
        while (maxUnits < units) {
            maxUnits += 1L shl bitPrecision
            bitPrecision++
            if (bitPrecision == 1) maxUnits++  // special: 1 bit → 2 slots
        }

        // Resolution is recalculated to exactly span (max-min) in (maxUnits-1) equal steps.
        // This ensures server encoding and client decoding use the same step size.
        val adjustedResolution = if (maxUnits > 1) (max - min) / (maxUnits - 1).toDouble() else resolution

        entries[type] = EncoderEntry(
            bitPrecision = bitPrecision,
            min = min,
            max = max,
            resolution = adjustedResolution,
            isRaw = false,
        )
    }

    fun getEntry(type: Int): EncoderEntry {
        return checkNotNull(entries[type]) { "Encoder type $type not registered" }
    }

    fun clearAll() {
        entries.fill(null)
    }
}
