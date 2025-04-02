package xyz.d1n0.helper

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class BytesConverterTest {

    @Test
    fun testShortToLittleEndianByteArray() {
        val value: Short = 0x1234.toShort()
        val result = value.toLittleEndianByteArray()
        val expected = byteArrayOf(0x34, 0x12)
        assertContentEquals(expected, result)
    }

    @Test
    fun testShortFromLittleEndianByteArray() {
        val bytes = byteArrayOf(0x34, 0x12)
        val result = Short.fromLittleEndianByteArray(bytes)
        val expected: Short = 0x1234.toShort()
        assertEquals(expected, result)

        assertFailsWith<IllegalArgumentException> {
            Short.fromLittleEndianByteArray(byteArrayOf(0x12))
        }
    }

    @Test
    fun testDoubleToByteArrayAndFromByteArray() {
        val value = 1234.5678
        val bytes = value.toByteArray()
        assertEquals(8, bytes.size)
        val result = Double.fromByteArray(bytes)
        assertEquals(value, result)

        assertFailsWith<IllegalArgumentException> {
            Double.fromByteArray(byteArrayOf(0x00, 0x01))
        }
    }

    @Test
    fun testIntToBcdByteAndFromBcdByte() {
        val value = 31
        val bcdByte = value.toBcdByte()
        val expected = 0x31.toByte()
        assertEquals(expected, bcdByte)

        val result = Int.fromBcdByte(bcdByte)
        assertEquals(value, result)

        assertFailsWith<IllegalArgumentException> {
            100.toBcdByte()
        }

        // Create an invalid BCD byte (high nibble > 9)
        val invalidBcd: Byte = 0xA1.toByte()
        assertFailsWith<IllegalArgumentException> {
            Int.fromBcdByte(invalidBcd)
        }
    }

    @Test
    fun testLocalDateTimeToByteArray() {
        val localDateTime = LocalDateTime(2025, 12, 31, 23, 59, 59, 500_000_000)
        val bytes = localDateTime.toByteArray()
        // Expected length: 2 bytes for year, then 1 byte each for month, day, hour, minute, second, weekday, and scaled milliseconds (total 9 bytes)
        assertEquals(9, bytes.size)
        val expectedYearBytes = localDateTime.year.toShort().toLittleEndianByteArray()
        assertContentEquals(expectedYearBytes, bytes.sliceArray(0..1))
        assertEquals(localDateTime.monthNumber.toByte(), bytes[2])
    }

    @Test
    fun testLocalDateBcdConversion() {
        val localDate = LocalDate(2025, 3, 24)
        val bytes = localDate.toBcdByteArray()
        assertEquals(3, bytes.size)
        val expected = byteArrayOf(0x25, 0x03, 0x24) // (2025-2000 = 25, then month and day in BCD)
        assertContentEquals(expected, bytes)

        val decodedDate = LocalDate.fromBcdByteArray(bytes)
        assertEquals(localDate, decodedDate)

        assertFailsWith<IllegalArgumentException> {
            LocalDate.fromBcdByteArray(byteArrayOf(0x25, 0x03))
        }
    }

    @Test
    fun testLocalTimeConversion() {
        val localTime = LocalTime(15, 30)
        val bytes = localTime.toByteArray()
        val expected = byteArrayOf(15, 30)
        assertContentEquals(expected, bytes)

        val decodedTime = LocalTime.fromByteArray(bytes)
        assertEquals(localTime, decodedTime)

        assertFailsWith<IllegalArgumentException> {
            LocalTime.fromByteArray(byteArrayOf(15))
        }
    }

    @Test
    fun testDurationConversion() {
        val duration = 12.hours + 30.minutes + 45.seconds
        val bytes = duration.toByteArray()
        val expected = byteArrayOf(12, 30, 45)
        assertContentEquals(expected, bytes)

        val decodedDuration = Duration.fromByteArray(bytes)
        assertEquals(duration, decodedDuration)

        assertFailsWith<IllegalArgumentException> {
            Duration.fromByteArray(byteArrayOf(12, 30))
        }
    }

    @Test
    fun testCasioByteConversion() {
        // Test ASCII conversion: a character in the range 0x20 to 0x7F should be encoded as its ASCII value.
        val asciiChar = 'A' // ASCII code 65
        val asciiByte = asciiChar.toCasioByte()
        assertEquals(asciiChar.code.toByte(), asciiByte)

        // Decoding should return the original ASCII character.
        val decodedAsciiChar = Char.fromCasioByte(asciiByte)
        assertEquals(asciiChar, decodedAsciiChar)

        // Test custom charset conversion:
        // Example from row 0x8: '¥' should map to a byte in the range 0x80 to 0x8D.
        val customChar1 = '¥'
        val expectedCustomByte1 = 0x80.toByte() // 0x80
        val customByte1 = customChar1.toCasioByte()
        assertEquals(expectedCustomByte1, customByte1)
        val decodedCustomChar1 = Char.fromCasioByte(customByte1)
        assertEquals(customChar1, decodedCustomChar1)

        // Example from row 0x9: '▶' should map to a byte in the range 0x90 to 0x93.
        val customChar2 = '▶'
        val expectedCustomByte2 = 0x93.toByte() // 0x90 + 0x3 = 0x93
        val customByte2 = customChar2.toCasioByte()
        assertEquals(expectedCustomByte2, customByte2)
        val decodedCustomChar2 = Char.fromCasioByte(customByte2)
        assertEquals(customChar2, decodedCustomChar2)
    }

    @Test
    fun testCasioStringConversion() {
        val original = "◆♪ ｢TeSt!｣"
        val bytes = original.toCasioByteArray()
        val decoded = String.fromCasioByteArray(bytes)
        assertEquals(original, decoded)
    }
}