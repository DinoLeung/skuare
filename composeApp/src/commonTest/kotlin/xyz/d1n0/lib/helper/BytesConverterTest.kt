package xyz.d1n0.helper

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.number
import xyz.d1n0.lib.helper.fromBcdByte
import xyz.d1n0.lib.helper.fromBcdByteArray
import xyz.d1n0.lib.helper.fromByteArray
import xyz.d1n0.lib.helper.fromCasioByte
import xyz.d1n0.lib.helper.fromCasioByteArray
import xyz.d1n0.lib.helper.fromLittleEndianByteArray
import xyz.d1n0.lib.helper.toBcdByte
import xyz.d1n0.lib.helper.toBcdByteArray
import xyz.d1n0.lib.helper.toByteArray
import xyz.d1n0.lib.helper.toCasioByte
import xyz.d1n0.lib.helper.toCasioByteArray
import xyz.d1n0.lib.helper.toLittleEndianByteArray
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.nanoseconds
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
	fun shortConversionPreservesSignedBitPatterns() {
		listOf(Short.MIN_VALUE, (-1).toShort(), 0.toShort(), Short.MAX_VALUE).forEach { value ->
			assertEquals(value, Short.fromLittleEndianByteArray(value.toLittleEndianByteArray()))
		}
		assertContentEquals(byteArrayOf(0x00, 0x80.toByte()), Short.MIN_VALUE.toLittleEndianByteArray())
		assertContentEquals(byteArrayOf(0xFF.toByte(), 0xFF.toByte()), (-1).toShort().toLittleEndianByteArray())
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
	fun doubleConversionUsesBigEndianAndPreservesSpecialValues() {
		assertContentEquals(
			byteArrayOf(0x3F, 0xF0.toByte(), 0, 0, 0, 0, 0, 0),
			1.0.toByteArray(),
		)
		listOf(0.0, -0.0, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY).forEach { value ->
			assertEquals(value.toBits(), Double.fromByteArray(value.toByteArray()).toBits())
		}
		val nan = Double.fromBits(0x7FF8_0000_0000_1234L)
		assertEquals(nan.toBits(), Double.fromByteArray(nan.toByteArray()).toBits())
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
	fun bcdConversionCoversBoundariesAndBothInvalidNibbles() {
		assertEquals(0x00, 0.toBcdByte().toInt())
		assertEquals(0x99.toByte(), 99.toBcdByte())
		assertEquals(0, Int.fromBcdByte(0x00))
		assertEquals(99, Int.fromBcdByte(0x99.toByte()))
		assertFailsWith<IllegalArgumentException> { (-1).toBcdByte() }
		assertFailsWith<IllegalArgumentException> { 100.toBcdByte() }
		assertFailsWith<IllegalArgumentException> { Int.fromBcdByte(0xA0.toByte()) }
		assertFailsWith<IllegalArgumentException> { Int.fromBcdByte(0x0A) }
	}

	@Test
	fun testLocalDateTimeToByteArray() {
		val localDateTime = LocalDateTime(2025, 12, 31, 23, 59, 59, 500_000_000)
		val bytes = localDateTime.toByteArray()
		// Expected length: 2 bytes for year, then 1 byte each for month, day, hour, minute, second, weekday, and scaled milliseconds (total 9 bytes)
		assertEquals(9, bytes.size)
		val expectedYearBytes = localDateTime.year.toShort().toLittleEndianByteArray()
		assertContentEquals(expectedYearBytes, bytes.sliceArray(0..1))
		assertEquals(localDateTime.month.number.toByte(), bytes[2])
		assertContentEquals(
			byteArrayOf(0xE9.toByte(), 0x07, 12, 31, 23, 59, 59, 3, 127),
			bytes,
		)
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
	fun localDateBcdConversionRejectsInvalidValues() {
		assertFailsWith<IllegalArgumentException> { LocalDate(1999, 12, 31).toBcdByteArray() }
		assertFailsWith<IllegalArgumentException> { LocalDate(2100, 1, 1).toBcdByteArray() }
		assertFailsWith<IllegalArgumentException> {
			LocalDate.fromBcdByteArray(byteArrayOf(0x25, 0x13, 0x01))
		}
		assertFailsWith<IllegalArgumentException> {
			LocalDate.fromBcdByteArray(byteArrayOf(0x25, 0x02, 0x30))
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
	fun localTimeConversionRejectsInvalidFields() {
		assertFailsWith<IllegalArgumentException> { LocalTime.fromByteArray(byteArrayOf(24, 0)) }
		assertFailsWith<IllegalArgumentException> { LocalTime.fromByteArray(byteArrayOf(23, 60)) }
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
	fun durationConversionDropsSubSecondPrecision() {
		val duration = 1.hours + 2.minutes + 3.seconds + 999_999_999.nanoseconds

		assertContentEquals(byteArrayOf(1, 2, 3), duration.toByteArray())
		assertEquals(1.hours + 2.minutes + 3.seconds, Duration.fromByteArray(duration.toByteArray()))
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
	fun casioConversionCoversEmptyJisAndFallbackRows() {
		assertNull(Char.fromCasioByte(0x00))
		assertNull(Char.fromCasioByte(0x0F))
		assertEquals('■', Char.fromCasioByte(0x10))
		assertEquals('■', Char.fromCasioByte(0x8F.toByte()))
		assertEquals('ｱ', Char.fromCasioByte(0xB1.toByte()))
		assertEquals(0xB1.toByte(), 'ｱ'.toCasioByte())
		assertEquals(0x00, '\u0001'.toCasioByte().toInt())
		assertEquals(0xFF.toByte(), 'é'.toCasioByte())
		assertEquals('■', Char.fromCasioByte(0xFF.toByte()))
	}

	@Test
	fun testCasioStringConversion() {
		val original = "◆♪ ｢TeSt!｣"
		val bytes = original.toCasioByteArray()
		val decoded = String.fromCasioByteArray(bytes)
		assertEquals(original, decoded)
	}

	@Test
	fun casioStringDecodingSkipsZeroPaddingAndUsesFallback() {
		assertEquals("AB■", String.fromCasioByteArray(byteArrayOf(0, 'A'.code.toByte(), 'B'.code.toByte(), 0xFF.toByte())))
		assertContentEquals(byteArrayOf('A'.code.toByte(), 0xFF.toByte()), "Aé".toCasioByteArray())
	}
}
