package dev.aether.manager.security

object StringCrypt {

    private const val KEY: Byte = 0x5A

    @JvmStatic
    fun d(data: ByteArray): String {
        val decoded = ByteArray(data.size) { i ->
            (data[i].toInt() xor KEY.toInt()).toByte()
        }
        return String(decoded, Charsets.UTF_8)
    }
}
