package xyz.d1n0.lib.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WatchModelTest {
	@Test
	fun matchesCapturedNames() {
		assertEquals(WatchModel.CLASSIC_SQUARE, WatchModel.fromAdvertisedName("CASIO GMW-B5000"))
		assertEquals(WatchModel.CLASSIC_SQUARE, WatchModel.fromAdvertisedName("CASIO GMW-B5000#"))
		assertEquals(WatchModel.CLASSIC_SQUARE, WatchModel.fromAdvertisedName("CASIO GW-B5600#"))
		assertEquals(WatchModel.MIP_SQUARE, WatchModel.fromAdvertisedName("CASIO GW-BX5600"))
	}

	@Test
	fun ignoresTrailingHash() {
		assertEquals(WatchModel.CLASSIC_SQUARE, WatchModel.fromAdvertisedName("CASIO GW-B5600"))
		assertEquals(WatchModel.MIP_SQUARE, WatchModel.fromAdvertisedName("CASIO GW-BX5600#"))
		assertEquals(WatchModel.MIP_SQUARE, WatchModel.fromAdvertisedName("CASIO GMW-BZ5000"))
		assertEquals(WatchModel.MIP_SQUARE, WatchModel.fromAdvertisedName("CASIO GMW-BZ5000#"))
	}

	@Test
	fun matchesFamilyPrefix() {
		assertEquals(WatchModel.DW_SQUARE, WatchModel.fromAdvertisedName("CASIO DW-B5600"))
		assertEquals(WatchModel.DW_SQUARE, WatchModel.fromAdvertisedName("CASIO DW-B5600#"))
		assertEquals(WatchModel.CLASSIC_SQUARE, WatchModel.fromAdvertisedName("CASIO GW-B5600"))
		assertEquals(WatchModel.CLASSIC_SQUARE, WatchModel.fromAdvertisedName("CASIO MRG-B5000"))
		assertNull(WatchModel.fromAdvertisedName("CASIO OTHER-B5000"))
		assertNull(WatchModel.fromAdvertisedName("CASIO OTHER-BX5600#"))
	}

	@Test
	fun weekdayLanguage() {
		assertTrue(WatchModel.CLASSIC_SQUARE.hasWeekdayLanguage)
		assertTrue(WatchModel.DW_SQUARE.hasWeekdayLanguage)
		assertFalse(WatchModel.MIP_SQUARE.hasWeekdayLanguage)
	}

	@Test
	fun squaresKeepSnoozeAlarmAndTimeZoneLocation() {
		listOf(WatchModel.CLASSIC_SQUARE, WatchModel.MIP_SQUARE, WatchModel.DW_SQUARE).forEach { model ->
			assertTrue(model.hasSnoozeAlarm, "$model hasSnoozeAlarm")
			assertTrue(model.hasTimeZoneLocation, "$model hasTimeZoneLocation")
			assertFalse(model.canAdjustHands, "$model canAdjustHands")
		}
	}

	@Test
	fun returnsNullForUnknownNames() {
		assertNull(WatchModel.fromAdvertisedName(null))
		assertNull(WatchModel.fromAdvertisedName("CASIO GBD-H2000"))
		// exact match only, a longer name isn't a prefix match
		assertNull(WatchModel.fromAdvertisedName("CASIO GW-B5600BC"))
		// the address is stored as the name when the watch didn't advertise one
		assertNull(WatchModel.fromAdvertisedName("ED:A1:AF:D5:A2:40"))
	}
}
