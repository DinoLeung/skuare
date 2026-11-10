package xyz.d1n0.lib.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class CustomWatchModelTest {
	@Test
	fun defaultsToMinimalProfile() {
		val model = CustomWatchModel()
		assertEquals(2, model.clockCount)
		assertEquals(0, model.reminderCount)
		assertFalse(model.hasFontStyle)
		assertFalse(model.hasShortCityNames)
		assertFalse(model.hasWeekdayLanguage)
	}

	@Test
	fun acceptsCountsWithinRange() {
		CustomWatchModel.CLOCK_COUNT_RANGE.forEach { CustomWatchModel(clockCount = it) }
		CustomWatchModel.REMINDER_COUNT_RANGE.forEach { CustomWatchModel(reminderCount = it) }
	}

	@Test
	fun rejectsClockCountOutOfRange() {
		// the pinned clock always exists, and positions only go up to 5
		assertFailsWith<IllegalArgumentException> { CustomWatchModel(clockCount = 0) }
		assertFailsWith<IllegalArgumentException> { CustomWatchModel(clockCount = 7) }
	}

	@Test
	fun rejectsReminderCountOutOfRange() {
		assertFailsWith<IllegalArgumentException> { CustomWatchModel(reminderCount = -1) }
		assertFailsWith<IllegalArgumentException> { CustomWatchModel(reminderCount = 6) }
	}

	@Test
	fun copiesFeaturesOfEveryKnownModel() {
		// also checks every known model fits the custom ranges, from() would throw otherwise
		WatchModel.entries.forEach { known ->
			val custom = CustomWatchModel.from(known)
			assertEquals(known.clockCount, custom.clockCount, "$known clockCount")
			assertEquals(known.reminderCount, custom.reminderCount, "$known reminderCount")
			assertEquals(known.hasFontStyle, custom.hasFontStyle, "$known hasFontStyle")
			assertEquals(known.hasShortCityNames, custom.hasShortCityNames, "$known hasShortCityNames")
			assertEquals(known.hasWeekdayLanguage, custom.hasWeekdayLanguage, "$known hasWeekdayLanguage")
		}
	}
}
