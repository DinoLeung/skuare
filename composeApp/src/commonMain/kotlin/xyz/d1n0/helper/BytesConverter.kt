package xyz.d1n0.helper

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.isoDayNumber

/**
 * Converts a given integer to a ByteArray in little-endian format.
 *
 * The least significant byte (LSB) is stored first, followed by the most significant byte (MSB).
 *
 * @return A ByteArray of size 2 representing the integer in little-endian byte order.
 */
fun Int.to2BytesLittleEndian(): ByteArray {
    require(this in 0x0000..0xFFFF) { "Input must be a 16-bit unsigned integer." }
    return byteArrayOf(
        (this and 0xFF).toByte(),        // Least significant byte
        ((this shr 8) and 0xFF).toByte() // Most significant byte
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
fun Int.Companion.from2BytesLittleEndian(bytes: ByteArray): Int {
    require(bytes.size == 2) { "Input must be a ByteArray of exactly 2 bytes." }
    return (bytes.get(1).toInt() shl 8) or (bytes.get(0).toInt() and 0xFF)
}

/**
 * Converts the current `LocalDateTime` instance to a `ByteArray`.
 *
 * This function serializes the date-time components into a compact byte representation:
 * - The year is represented using two bytes in little-endian format.
 * - The month and day are each represented as a single byte.
 * - The hour, minute, and second are each represented as a single byte.
 * - The weekday is represented as a single byte, where Monday is 1 and Sunday is 7.
 * - Milliseconds are scaled down and stored as a single byte.
 *
 * @receiver The `LocalDateTime` instance to be converted.
 * @return A `ByteArray` containing the serialized date-time information.
 */
fun LocalDateTime.toByteArray() =
    byteArrayOf(
        // Year represented in two bytes
        *this.year.to2BytesLittleEndian(),
        // Month and Day as single bytes
        this.monthNumber.toByte(),
        this.dayOfMonth.toByte(),
        // Hour, Minute, and Second as single bytes
        this.hour.toByte(),
        this.minute.toByte(),
        this.second.toByte(),
        // Weekday (Monday = 1, ..., Sunday = 7)
        this.dayOfWeek.isoDayNumber.toByte(),
        // Milliseconds, scaled to 0-255 for 1 byte
        ((this.nanosecond / 1_000_000) * 255 / 999).toByte()
    )
