package xyz.d1n0.lib.constant

import kotlinx.datetime.LocalTime
import xyz.d1n0.lib.model.ValueOutOfRangeException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AutoSyncDelayTest {
	@Test
	fun adjustmentTimesAreEverySixHoursAtTheDelay() {
		assertEquals(
			listOf(LocalTime(0, 30), LocalTime(6, 30), LocalTime(12, 30), LocalTime(18, 30)),
			AutoSyncDelay(30).adjustmentTimes,
		)
	}

	@Test
	fun zeroDelayAdjustsOnTheHour() {
		assertEquals(
			listOf(LocalTime(0, 0), LocalTime(6, 0), LocalTime(12, 0), LocalTime(18, 0)),
			AutoSyncDelay(0).adjustmentTimes,
		)
	}

	@Test
	fun rejectsMinutesPastTheHourOutsideTheRange() {
		assertEquals(0..59, assertFailsWith<ValueOutOfRangeException> { AutoSyncDelay(60) }.range)
		assertFailsWith<ValueOutOfRangeException> { AutoSyncDelay(-1) }
	}
}
