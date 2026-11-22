package xyz.d1n0.ui.screen.locations

import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class LocationAgeTest {
	private val utc = TimeZone.UTC

	@Test
	fun cutsOffAtTheSameTimeOfDay() {
		val now = Instant.parse("2026-11-22T14:05:00Z")
		assertEquals(Instant.parse("2026-11-15T14:05:00Z"), LocationAge.WEEK.cutoff(now, utc))
		assertEquals(Instant.parse("2026-10-22T14:05:00Z"), LocationAge.MONTH.cutoff(now, utc))
		assertEquals(Instant.parse("2026-08-22T14:05:00Z"), LocationAge.THREE_MONTHS.cutoff(now, utc))
		assertEquals(Instant.parse("2026-05-22T14:05:00Z"), LocationAge.SIX_MONTHS.cutoff(now, utc))
		assertEquals(Instant.parse("2025-11-22T14:05:00Z"), LocationAge.YEAR.cutoff(now, utc))
	}

	@Test
	fun monthsAreCalendarMonths() {
		// no 31 February, the month before 31 March ends on the 28th
		assertEquals(
			Instant.parse("2026-02-28T09:00:00Z"),
			LocationAge.MONTH.cutoff(Instant.parse("2026-03-31T09:00:00Z"), utc),
		)
	}

	@Test
	fun weeksFollowTheWallClockAcrossDaylightSaving() {
		// London moved its clocks back on 25 October 2026, a week before 9:00 is still 9:00 local
		val london = TimeZone.of("Europe/London")
		assertEquals(
			Instant.parse("2026-10-22T08:00:00Z"),
			LocationAge.WEEK.cutoff(Instant.parse("2026-10-29T09:00:00Z"), london),
		)
	}
}
