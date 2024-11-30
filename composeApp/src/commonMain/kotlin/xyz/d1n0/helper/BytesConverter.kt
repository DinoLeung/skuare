package xyz.d1n0.helper

class BytesConverter {
    companion object {

        /**
         * Converts a given integer to a ByteArray in little-endian format.
         *
         * The least significant byte (LSB) is stored first, followed by the most significant byte (MSB).
         *
         * @param number The integer to be converted to a little-endian ByteArray.
         *
         * @return A ByteArray of size 2 representing the integer in little-endian byte order.
         */
        fun intToLittleEdianBytes(number: Int): ByteArray {
            return byteArrayOf(
                (number and 0xFF).toByte(),        // Least significant byte
                ((number shr 8) and 0xFF).toByte() // Most significant byte
            )
        }

        /**
         * Converts a 2-byte little-endian ByteArray into an integer.
         *
         * Little-endian format means the least significant byte (LSB) is at index 0, and the most significant byte (MSB)
         * is at index 1 of the ByteArray.
         *
         * @param bytes A ByteArray of size 2 representing a number in little-endian byte order.
         *
         * @return An integer corresponding to the little-endian ByteArray.
         *
         * @throws IllegalArgumentException if the input byte array is not exactly 2 bytes long.
         */
        fun littleEdianBytesToInt(bytes: ByteArray): Int {
            if (bytes.size != 2) {
                throw IllegalArgumentException("Input must be a ByteArray of exactly 2 bytes.")
            }
            return (bytes[1].toInt() shl 8) or (bytes[0].toInt() and 0xFF)
        }

    }
}