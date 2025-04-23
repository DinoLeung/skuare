package xyz.d1n0.lib.helper

import kotlin.test.DefaultAsserter.assertEquals
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class StringConverterTest {
	@Test
	fun `Int toOrdinalString produces correct suffixes`() {
		val cases = mapOf(
			1 to "1st",
			2 to "2nd",
			3 to "3rd",
			4 to "4th",
			11 to "11th",
			12 to "12th",
			13 to "13th",
			21 to "21st",
			22 to "22nd",
			23 to "23rd",
			100 to "100th",
			111 to "111th",
			-1 to "-1th"
		)
		cases.forEach { (input, expected) ->
			assertEquals(
				"Expected $expected to become ${input.toOrdinalString()}",
				expected,
				input.toOrdinalString()
			)
		}
	}

	@Test
	fun `Double toSignedString handles positive negative and zero`() {
		assertEquals("Zero should be prefixed with ±", "±0.0", 0.0.toSignedString())
		assertEquals("Positive values should be prefixed with +", "+1.23", 1.23.toSignedString())
		assertEquals(
			"Negative values should remain with - as default",
			"-4.56",
			(-4.56).toSignedString()
		)
	}

	@Test
	fun `Duration toHHMMSSString formats durations under 100h`() {
		assertEquals("000000", 0.seconds.toHHMMSSString())
		assertEquals("000001", 1.seconds.toHHMMSSString())
		assertEquals("000100", 1.minutes.toHHMMSSString())
		assertEquals("010000", 1.hours.toHHMMSSString())
		assertEquals("995959", (100.hours - 1.seconds).toHHMMSSString())
	}

	@Test
	fun `Duration toHHMMSSString formats durations over 100h`() {
		assertEquals("996000", 100.hours.toHHMMSSString())
		assertEquals("999900", (99.hours + 99.minutes).toHHMMSSString())
		assertEquals("999960", (99.hours + 100.minutes).toHHMMSSString())
		assertEquals("999999", (99.hours + 99.minutes + 99.seconds).toHHMMSSString())
	}

	@Test
	fun `Duration toHHMMSSString throws for 99h99m99s or more`() {
		val max = 99.hours + 99.minutes + 99.seconds
		assertFailsWith<IllegalArgumentException>("Durations >=99h99m99s should throw") {
			(max + 1.seconds).toHHMMSSString()
		}
	}

	@Test
	fun `Duration fromHHMMSS parses valid strings`() {
		assertEquals(Duration.ZERO, Duration.fromHHMMSS("000000"))
		assertEquals(1.seconds, Duration.fromHHMMSS("000001"))
		assertEquals(1.minutes, Duration.fromHHMMSS("000100"))
		assertEquals(1.hours + 2.minutes + 3.seconds, Duration.fromHHMMSS("010203"))
		assertEquals(23.hours + 59.minutes + 59.seconds, Duration.fromHHMMSS("235959"))

		assertEquals(100.hours, Duration.fromHHMMSS("996000"))
		assertEquals(99.hours + 99.minutes, Duration.fromHHMMSS("999900"))
		assertEquals(99.hours + 100.minutes, Duration.fromHHMMSS("999960"))
		assertEquals(99.hours + 99.minutes + 99.seconds, Duration.fromHHMMSS("999999"))
	}

	@Test
	fun `Duration fromHHMMSS throws for invalid input`() {
		assertFailsWith<IllegalArgumentException> { Duration.fromHHMMSS("123") }
		assertFailsWith<IllegalArgumentException> { Duration.fromHHMMSS("1234567") }
	}
}