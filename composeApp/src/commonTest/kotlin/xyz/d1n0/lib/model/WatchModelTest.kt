package xyz.d1n0.lib.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

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
	fun returnsNullForUnknownNames() {
		assertNull(WatchModel.fromAdvertisedName(null))
		assertNull(WatchModel.fromAdvertisedName("CASIO GA-B2100"))
		// exact match only, a longer name isn't a prefix match
		assertNull(WatchModel.fromAdvertisedName("CASIO GW-B5600BC"))
		// the address is stored as the name when the watch didn't advertise one
		assertNull(WatchModel.fromAdvertisedName("ED:A1:AF:D5:A2:40"))
	}
}
