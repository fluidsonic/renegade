package ccr.server.core

internal class Random3Class(seed: UInt = 0u, index: UInt = 0u) {
    private var seed: Int = seed.toInt()
    private var index: Int = index.toInt()

    fun next(): Int {
        var loword = seed
        var hiword = index++
        for (i in 0 until 4) {
            val hihold = hiword
            val temp = (hihold.toUInt() xor MIX1[i]).toInt()
            val itmpl = temp and 0xffff
            val itmph = temp shr 16
            var t = itmpl * itmpl + (itmph * itmph).inv()
            t = (t ushr 16) or (t shl 16)
            hiword = (loword.toUInt() xor ((t.toUInt() xor MIX2[i]) + (itmpl * itmph).toUInt())).toInt()
            loword = hihold
        }
        return hiword
    }

    companion object {
        private val MIX1 = uintArrayOf(
            0x0baa96887u, 0x01e17d32cu, 0x003bcdc3cu, 0x00f33d1b2u,
            0x076a6491du, 0x0c570d85du, 0x0e382b1e3u, 0x078db4362u,
            0x07439a9d4u, 0x09cea8ac5u, 0x089537c5cu, 0x02588f55du,
            0x0415b5e1du, 0x0216e3d95u, 0x085c662e7u, 0x05e8ab368u,
            0x03ea5cc8cu, 0x0d26a0f74u, 0x0f3a9222bu, 0x048aad7e4u,
        )
        private val MIX2 = uintArrayOf(
            0x04b0f3b58u, 0x0e874f0c3u, 0x06955c5a6u, 0x055a7ca46u,
            0x04d9a9d86u, 0x0fe28a195u, 0x0b1ca7865u, 0x06b235751u,
            0x09a997a61u, 0x0aa6e95c8u, 0x0aaa98ee1u, 0x05af9154cu,
            0x0fc8e2263u, 0x0390f5e8cu, 0x058ffd802u, 0x0ac0a5ebau,
            0x0ac4874f6u, 0x0a9df0913u, 0x086be4c74u, 0x0ed2c123bu,
        )
    }
}

internal class Random2Class(seed: UInt = 0u) {
    private var index1 = 0
    private var index2 = 103
    private val table = IntArray(250)

    init {
        val seeder = Random3Class(seed)
        for (i in 0 until 250) {
            table[i] = seeder.next()
        }
    }

    fun next(): Int {
        table[index1] = table[index1] xor table[index2]
        val v = table[index1]
        if (++index1 >= 250) index1 = 0
        if (++index2 >= 250) index2 = 0
        return v
    }
}

object FreeRandom {
    private val generator = Random2Class()

    fun getInt(): Int = generator.next()

    fun getInt(max: Int): Int = (generator.next() and 0x7FFFFFFF) % max

    fun getInt(min: Int, max: Int): Int {
        val lo = if (min <= max) min else max
        val hi = if (min <= max) max else min
        return getInt(hi - lo) + lo
    }

    fun getFloat(): Float = getInt(4097).toFloat() / 4096f

    fun getFloat(max: Float): Float = getFloat() * max

    fun getFloat(min: Float, max: Float): Float {
        val lo = if (min <= max) min else max
        val hi = if (min <= max) max else min
        return getFloat() * (hi - lo) + lo
    }
}
