package xyz.d1n0.lib.helper

import xyz.d1n0.lib.constant.WorldTimezoneData
import xyz.d1n0.lib.model.WorldTimezone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

class TimezoneSearcherTest {
	// Adelaide observes daylight saving time in October, so its current offset is +10:30 rather than +9:30.
	private val duringDaylightSaving = Instant.parse("2026-10-18T00:00:00Z")

	private val adelaide = WorldTimezoneData.values.single { it.timeZone == "Australia/Adelaide" }

	private fun search(query: String): List<WorldTimezone> =
		searchTimeZones(WorldTimezoneData.values.toList(), query, duringDaylightSaving)

	private fun timezone(
		city: String,
		country: String = "Test Country",
		identifier: Short,
	) = WorldTimezone(
		timeZone = "Etc/UTC",
		country = country,
		city = city,
		longitude = 0.0,
		latitude = 0.0,
		cityName = city.uppercase(),
		cityCode = null,
		identifier = identifier,
		offset = 0.0,
		dstDiff = 0.0,
		dstRules = 0,
	)

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

	@Test
	fun signedOffsetOnlyMatchesTheRequestedSign() {
		val positive = search("+10")
		val negative = search("-10")

		assertTrue(adelaide in positive)
		assertFalse(adelaide in negative)
		assertTrue(positive.all { it.currentOffsetMinutes(duringDaylightSaving) >= 0 })
		assertTrue(negative.all { it.currentOffsetMinutes(duringDaylightSaving) < 0 })
	}

	@Test
	fun unsignedOffsetMatchesEitherSignAndOmittedMinutes() {
		val results = search("10")

		assertTrue(adelaide in results) // +10:30: omitted minutes match the whole +10 hour
		assertTrue(results.any { it.currentOffsetMinutes(duringDaylightSaving) < 0 })
		assertTrue(results.all { kotlin.math.abs(it.currentOffsetMinutes(duringDaylightSaving)) / 60 == 10 })
	}

	@Test
	fun invalidOffsetSyntaxFallsBackToTextSearch() {
		assertTrue(search("+24:00").isEmpty())
		assertTrue(search("+10:60").isEmpty())
	}

	@Test
	fun blankQueryKeepsSourceOrder() {
		val zones = listOf(
			timezone("Third", identifier = 3),
			timezone("First", identifier = 1),
			timezone("Second", identifier = 2),
		)

		assertEquals(zones, searchTimeZones(zones, " \t ", duringDaylightSaving))
	}

	@Test
	fun textSearchNormalizesCaseAndPunctuation() {
		val saintJohns = timezone("St. John's", identifier = 1)
		val other = timezone("Elsewhere", identifier = 2)

		assertEquals(
			listOf(saintJohns),
			searchTimeZones(listOf(other, saintJohns), "ST. JOHN'S", duringDaylightSaving),
		)
	}

	@Test
	fun textSearchCanMatchTokensAcrossCityAndCountry() {
		val losAngeles = timezone("Los Angeles", country = "United States", identifier = 1)
		val london = timezone("London", country = "United Kingdom", identifier = 2)

		assertEquals(
			listOf(losAngeles),
			searchTimeZones(listOf(london, losAngeles), "united angeles", duringDaylightSaving),
		)
	}

	@Test
	fun exactMatchesSortAheadOfPrefixesAndSubstrings() {
		val yorktown = timezone("Yorktown", identifier = 1)
		val newYork = timezone("New York", identifier = 2)
		val york = timezone("York", identifier = 3)

		assertEquals(
			listOf(york, newYork, yorktown),
			searchTimeZones(listOf(yorktown, newYork, york), "york", duringDaylightSaving),
		)
	}

	@Test
	fun typoMatchingHandlesAdjacentTransposition() {
		val adelaide = timezone("Adelaide", identifier = 1)
		val unrelated = timezone("Tokyo", identifier = 2)

		assertEquals(
			listOf(adelaide),
			searchTimeZones(listOf(unrelated, adelaide), "adeliade", duringDaylightSaving),
		)
		assertTrue(searchTimeZones(listOf(adelaide), "completely different", duringDaylightSaving).isEmpty())
	}
}
