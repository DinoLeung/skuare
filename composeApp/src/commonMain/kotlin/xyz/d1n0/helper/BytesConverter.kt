package xyz.d1n0.helper

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.isoDayNumber
import kotlin.time.Duration
import xyz.d1n0.constant.customCharset
import xyz.d1n0.constant.customCharsetByte
import xyz.d1n0.constant.fallbackByte
import xyz.d1n0.constant.fallbackChar
import xyz.d1n0.constant.jisX0201Charset
import xyz.d1n0.constant.jisX0201CharsetByte

/**
 * Converts this Short into a 2-byte array in little-endian order, preserving its two’s complement representation.
 *
 * In little-endian format, the least significant byte is placed at index 0, while the most significant byte is placed at index 1.
 *
 * @return a ByteArray of length 2 representing the Short in little-endian byte order.
 */
fun Short.toLittleEndianByteArray(): ByteArray {
    return byteArrayOf(
        (this.toInt() and 0xFF).toByte(),        // Least significant byte
        ((this.toInt() shr 8) and 0xFF).toByte() // Most significant byte
    )
}

/**
 * Decodes a 2-byte little-endian ByteArray into its corresponding Short value.
 *
 * In little-endian order, the byte at index 0 is the least significant, and the byte at index 1
 * is the most significant.
 *
 * @param bytes a ByteArray of exactly 2 bytes.
 * @return the Short represented by the Byte Array.
 * @throws IllegalArgumentException if the byte array does not contain exactly 2 bytes.
 */
fun Short.Companion.fromLittleEndianByteArray(bytes: ByteArray): Short {
    require(bytes.size == 2) { "Input must be a ByteArray of exactly 2 bytes." }
    val low = bytes[0].toInt() and 0xFF
    val high = bytes[1].toInt() and 0xFF
    return ((high shl 8) or low).toShort()
}

/**
 * Converts this Double into an 8-byte array in big-endian order.
 *
 * The resulting byte array is in big-endian order, with the most significant byte at index 0.
 *
 * @return a ByteArray of length 8 representing the Double.
 */
fun Double.toByteArray(): ByteArray {
    val bits = this.toBits()
    val byteArray = ByteArray(8)
    for (i in 0 until 8) {
        // Extract the least-significant byte and place it in reverse order
        byteArray[7 - i] = ((bits shr (8 * i)) and 0xFF).toByte()
    }
    return byteArray
}

/**
 * Constructs a Double from an 8-byte array in big-endian order.
 *
 * The byte array must be exactly 8 bytes long, with the most significant byte at index 0.
 *
 * @param bytes a ByteArray of exactly 8 bytes.
 * @return the corresponding Double value.
 * @throws IllegalArgumentException if the byte array does not contain exactly 8 bytes.
 */
fun Double.Companion.fromByteArray(bytes: ByteArray): Double {
    require(bytes.size == 8) { "Input must be exactly 8 bytes long." }
    var bits = 0L
    for (byte in bytes) {
        bits = (bits shl 8) or (byte.toLong() and 0xFF)
    }
    return Double.fromBits(bits)
}

/**
 * Converts this integer (expected to be between 0 and 99) into a Binary-Coded Decimal (BCD)
 * encoded byte.
 *
 * In BCD, the tens digit is stored in the upper 4 bits and the ones digit in the lower 4 bits.
 * For example:
 * - 31 is encoded as 0x31 (with 3 in the high nibble and 1 in the low nibble).
 * - 99 is encoded as 0x99.
 *
 * @return a Byte representing the BCD encoding of the integer.
 * @throws IllegalArgumentException if the integer is not in the range 0..99.
 */
fun Int.toBcdByte(): Byte {
    require(this in 0..99) { "Value must be between 0 and 99 for BCD encoding" }
    val tens = this / 10
    val ones = this % 10
    return ((tens shl 4) or ones).toByte()
}

/**
 * Decodes a BCD-encoded byte into its corresponding integer value.
 *
 * In Binary-Coded Decimal (BCD) encoding, the high nibble represents the tens digit and the low
 * nibble represents the ones digit. For example, a BCD byte 0x31 decodes to 31.
 *
 * @param bcd the BCD-encoded byte.
 * @return the integer value (0 to 99) represented by the BCD byte.
 * @throws IllegalArgumentException if either nibble is greater than 9, indicating an invalid BCD value.
 */
@OptIn(ExperimentalStdlibApi::class)
fun Int.Companion.fromBcdByte(bcd: Byte): Int {
    val highNibble = (bcd.toInt() ushr 4) and 0x0F
    val lowNibble = bcd.toInt() and 0x0F
    require(highNibble in 0..9 && lowNibble in 0..9) {
        "Invalid BCD byte: contains non-decimal digits ${bcd.toHexString(HexFormat.UpperCase)}"
    }
    return highNibble * 10 + lowNibble
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
        *this.year.toShort().toLittleEndianByteArray(),
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

/**
 * Converts this LocalDate into a 3-byte array where each component is encoded in Binary-Coded Decimal (BCD).
 *
 * The encoding scheme is as follows:
 * - Byte 0: Year as a 2-digit number offset from 2000 (e.g., 2025 becomes 25, then encoded as 0x25)
 * - Byte 1: Month (in BCD)
 * - Byte 2: Day of the month (in BCD)
 *
 * For example, LocalDate.of(2025, 3, 24) is encoded as:
 * ```
 * byteArrayOf(0x25, 0x03, 0x24)
 * ```
 *
 * @return a ByteArray of length 3 containing the BCD-encoded date.
 * @throws IllegalArgumentException if any date component is out of the valid range for BCD encoding.
 */
fun LocalDate.toBcdByteArray() =
    byteArrayOf(
        (this.year - 2000).toBcdByte(),
        this.monthNumber.toBcdByte(),
        this.dayOfMonth.toBcdByte()
    )

/**
 * Creates a LocalDate from a 3-byte BCD-encoded ByteArray.
 *
 * The byte array must have the following structure:
 * - Byte 0: Year offset from 2000 (in BCD), e.g. 0x25 represents 2025.
 * - Byte 1: Month (in BCD)
 * - Byte 2: Day of month (in BCD)
 *
 * For example, byteArrayOf(0x25, 0x03, 0x24) decodes to LocalDate.of(2025, 3, 24).
 *
 * @param bytes a ByteArray of exactly 3 bytes containing the BCD-encoded date.
 * @return the corresponding LocalDate.
 * @throws IllegalArgumentException if the array length is not 3 or if the BCD values are invalid.
 */
fun LocalDate.Companion.fromBcdByteArray(bytes: ByteArray): LocalDate {
    require(bytes.size == 3) { "BCD date array must be exactly 3 bytes" }

    val year = 2000 + Int.fromBcdByte(bytes[0])
    val month = Int.fromBcdByte(bytes[1])
    val day = Int.fromBcdByte(bytes[2])

    return LocalDate(year = year, monthNumber = month, dayOfMonth = day)
}

/**
 * Converts this LocalTime into a 2-byte array.
 *
 * The returned array contains:
 * - Byte 0: Hour (0 to 23)
 * - Byte 1: Minute (0 to 59)
 *
 * These values are stored as raw byte values (i.e. not BCD encoded).
 *
 * @return a ByteArray of length 2 representing this LocalTime.
 */
fun LocalTime.toByteArray() =
    byteArrayOf(
        this.hour.toByte(),
        this.minute.toByte(),
    )

/**
 * Constructs a LocalTime instance from a 2-byte array.
 *
 * The expected byte array structure is:
 * - Byte 0: Hour (0 to 23)
 * - Byte 1: Minute (0 to 59)
 *
 * Each value is read directly as a raw byte.
 *
 * @param bytes a ByteArray of exactly 2 bytes.
 * @return a LocalTime corresponding to the provided hour and minute.
 * @throws IllegalArgumentException if the byte array does not contain exactly 2 bytes.
 */
fun LocalTime.Companion.fromByteArray(bytes: ByteArray): LocalTime {
    require(bytes.size == 2) { "Time byte array must be exactly 2 bytes" }
    val hour = bytes[0].toInt()
    val minute = bytes[1].toInt()
    return LocalTime(hour = hour, minute = minute)
}


/**
 * Serializes this Duration into a 3-byte array.
 *
 * The duration is decomposed into hours, minutes, and seconds, with each component stored
 * as a single byte. The nanosecond part of the Duration is ignored.
 *
 * **Important:** Ensure that the hours, minutes, and seconds values do not exceed 255.
 *
 * For example, a duration of 12 hours, 30 minutes, and 45 seconds is converted to:
 * ```
 * byteArrayOf(12, 30, 45)
 * ```
 *
 * @return a ByteArray of length 3 containing the hours, minutes, and seconds.
 */
fun Duration.toByteArray() =
    this.toComponents { hours, minutes, seconds, _ ->
        byteArrayOf(
            hours.toByte(),
            minutes.toByte(),
            seconds.toByte(),
        )
    }

/**
 * Constructs a Duration from a 3-byte array.
 *
 * The byte array must be structured as follows:
 * - Byte 0: Hours (0 to 255)
 * - Byte 1: Minutes (0 to 255)
 * - Byte 2: Seconds (0 to 255)
 *
 * Each byte is directly interpreted as an integer value.
 * For instance, byteArrayOf(0x0C, 0x1E, 0x2D) corresponds to a duration of 12 hours, 30 minutes, and 45 seconds.
 *
 * @param bytes a ByteArray of exactly 3 bytes.
 * @return a Duration representing the interval defined by the byte array.
 * @throws IllegalArgumentException if the byte array length is not exactly 3.
 */
fun Duration.Companion.fromByteArray(bytes: ByteArray): Duration {
    require(bytes.size == 3) { "Duration byte array must be exactly 3 bytes" }
    val hours = bytes[0].toInt()
    val minutes = bytes[1].toInt()
    val seconds = bytes[2].toInt()
    return hours.hours + minutes.minutes + seconds.seconds
}

/**
 * Decodes a single Casio-encoded byte into its corresponding character.
 *
 * The encoding scheme is as follows:
 * - If the byte's high nibble is 0 (row 0), returns null.
 * - If the row is 1, returns the fallback character.
 * - For rows 2 to 7, the byte is interpreted as a standard ASCII code.
 * - For rows 8 and 9, the character is looked up in the [customCharset] mapping.
 * - For rows 0xA to 0xD, the character is looked up in the [jisX0201Charset] mapping.
 * - For any other row, returns the fallback character.
 *
 * @param byte a Casio-encoded byte (0x00 to 0xFF)
 * @return the decoded character, or null if the byte is in row 0.
 */
fun Char.Companion.fromCasioByte(byte: Byte): Char? {
    val unsigned = byte.toInt() and 0xFF
    val row = unsigned shr 4
    val col = unsigned and 0x0F
    return when (row) {
        0 -> null
        1 -> fallbackChar
        in 2..7 -> (unsigned).toChar()
        in 8..9 -> customCharset[row]?.get(col) ?: fallbackChar
        in 0xA..0xD -> jisX0201Charset[row]?.get(col) ?: fallbackChar
        else -> fallbackChar
    }
}

/**
 * Encodes this character into a Casio-encoded byte.
 *
 * The encoding logic is as follows:
 * - If the character’s code is in the range 0x00 to 0x01, returns 0.
 * - If the character is in the ASCII range (0x20 to 0x7F), it is encoded directly using its code value.
 * - If the character exists in the [customCharsetByte] mapping, that encoding is used.
 * - If the character exists in the [jisX0201CharsetByte] mapping, that encoding is used.
 * - Otherwise, the [fallbackByte] is returned.
 *
 * @return a Byte representing the Casio-encoded value of this character.
 */
fun Char.toCasioByte(): Byte {
    if (this.code in 0x00..0x01)
        return 0

    if (this.code in 0x20..0x7F)
        return this.code.toByte()

    if (this in customCharsetByte)
        return customCharsetByte.getValue(this)

    if (this in jisX0201CharsetByte)
        return jisX0201CharsetByte.getValue(this)

    // 0x10..0x1f || 0xE0..0xFF
    return fallbackByte
}

/**
 * Decodes a ByteArray of Casio-encoded bytes into a String.
 *
 * Each byte is processed with [Char.fromCasioByte] and converted to its string representation.
 * If a byte decodes to null, it is skipped in the output.
 *
 * @param bytes a ByteArray containing Casio-encoded bytes.
 * @return a String representing the decoded characters.
 */
fun String.Companion.fromCasioByteArray(bytes: ByteArray): String =
    bytes.joinToString(separator = "") { Char.fromCasioByte(it)?.toString() ?: "" }


/**
 * Encodes this String into a ByteArray of Casio-encoded bytes.
 *
 * Each character in the string is converted using [Char.toCasioByte], and the resulting bytes
 * are collected into a ByteArray.
 *
 * @return a ByteArray containing the Casio-encoded bytes for this string.
 */
fun String.toCasioByteArray(): ByteArray =
    this.map { it.toCasioByte() }.toByteArray()
