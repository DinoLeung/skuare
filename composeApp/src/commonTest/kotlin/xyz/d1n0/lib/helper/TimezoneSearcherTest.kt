package xyz.d1n0.lib.helper

import xyz.d1n0.lib.constant.WorldTimezoneData
import xyz.d1n0.lib.model.WorldTimezone
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

class TimezoneSearcherTest {
	// Adelaide observes daylight saving time in October, so its current offset is +10:30 rather than +9:30.
	private val duringDaylightSaving = Instant.parse("2026-10-18T00:00:00Z")

	private val adelaide = WorldTimezoneData.values.single { it.timeZone == "Australia/Adelaide" }

	private fun search(query: String): List<WorldTimezone> =
		searchTimeZones(WorldTimezoneData.values.toList(), query, duringDaylightSaving)

	@Test
	fun offsetQueryMatchesCurrentOffsetDuringDaylightSaving() {
		assertTrue(adelaide in search("+10:30"))
		assertTrue(adelaide in search("10:30"))
		assertTrue(adelaide in search("10.30"))
	}

	@Test
	fun offsetQueryDoesNotMatchStandardOffsetDuringDaylightSaving() {
		assertFalse(adelaide in search("+9:30"))
		assertFalse(adelaide in search("9:30"))
	}

	@Test
	fun offsetQueryMatchesStandardOffsetOutsideDaylightSaving() {
		val winter = Instant.parse("2026-07-01T00:00:00Z")
		val results = searchTimeZones(WorldTimezoneData.values.toList(), "+9:30", winter)

		assertTrue(adelaide in results)
		assertFalse(adelaide in searchTimeZones(WorldTimezoneData.values.toList(), "+10:30", winter))
	}
}
