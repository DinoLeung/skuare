package xyz.d1n0.helper

class BytesConverter {
    companion object {

        // Casio represents 2 bytes number in reverse order, e.g. [0xE8, 0x07] represents 2024
        fun intToLittleEdianBytes(number: Int): ByteArray {
            return byteArrayOf(
                (number and 0xFF).toByte(),        // Least significant byte
                ((number shr 8) and 0xFF).toByte() // Most significant byte
            )
        }

        fun littleEdianBytesToInt(bytes: ByteArray): Int {
            if (bytes.size != 2) {
                throw IllegalArgumentException("Input must be a ByteArray of exactly 2 bytes.")
            }
            return (bytes[1].toInt() shl 8) or (bytes[0].toInt() and 0xFF)
        }

    }
}