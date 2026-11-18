package xyz.d1n0.lib.constant

import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals

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
}
