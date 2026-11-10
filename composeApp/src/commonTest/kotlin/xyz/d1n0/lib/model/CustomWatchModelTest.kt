package xyz.d1n0.lib.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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

	@Test
	fun resolvesKnownNameOverCustomModel() {
		val custom = CustomWatchModel(clockCount = 4)
		assertEquals(WatchModel.MIP_SQUARE, resolveWatchModel("CASIO GW-BX5600", custom))
		assertEquals(WatchModel.DW_SQUARE, resolveWatchModel("CASIO DW-B5600#", null))
	}

	@Test
	fun resolvesStoredCustomModel() {
		val custom = CustomWatchModel(clockCount = 4, hasWeekdayLanguage = true)
		assertEquals(custom, resolveWatchModel("CASIO GA-B2100", custom))
	}

	@Test
	fun resolvesUnknownWatchWithoutCustomModelToDefault() {
		// not paired yet, or paired before custom models existed
		assertEquals(CustomWatchModel(), resolveWatchModel("CASIO GA-B2100", null))
		// the address is stored as the name when the watch didn't advertise one
		assertEquals(CustomWatchModel(), resolveWatchModel("ED:A1:AF:D5:A2:40", null))
		assertEquals(CustomWatchModel(), resolveWatchModel(null, null))
	}

	@Test
	fun customWatchSwitchesToOtherCustomModel() {
		assertTrue(shouldSwitchToCustomModel(CustomWatchModel(), CustomWatchModel(clockCount = 4)))
	}

	@Test
	fun customWatchKeepsSameCustomModel() {
		// nothing to reset, so the clocks and reminders read from the watch are kept
		assertFalse(shouldSwitchToCustomModel(CustomWatchModel(clockCount = 4), CustomWatchModel(clockCount = 4)))
	}

	@Test
	fun knownWatchCannotSwitchToCustomModel() {
		WatchModel.entries.forEach { known ->
			assertFailsWith<IllegalStateException>("$known") {
				shouldSwitchToCustomModel(known, CustomWatchModel.from(known))
			}
		}
	}
}
