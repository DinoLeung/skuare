package xyz.d1n0.data.database

import xyz.d1n0.lib.model.CustomWatchModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CustomWatchModelMigrationTest {
	@Test
	fun knownWatchGetsNoCustomModel() {
		listOf(
			"CASIO GMW-B5000",
			"CASIO GMW-B5000#",
			"CASIO GW-B5600",
			"CASIO MRG-B5000",
			"CASIO GW-BX5600#",
			"CASIO GMW-BZ5000",
			"CASIO DW-B5600",
		).forEach { assertNull(migratedCustomModel(it), it) }
	}

	@Test
	fun unknownWatchGetsDefaultCustomModel() {
		assertEquals(CustomWatchModel(), migratedCustomModel("CASIO GA-B2100"))
		assertEquals(CustomWatchModel(), migratedCustomModel("CASIO GW-B5600BC"))
	}

	@Test
	fun watchStoredByAddressGetsDefaultCustomModel() {
		// the address is stored as the name when the watch didn't advertise one
		assertEquals(CustomWatchModel(), migratedCustomModel("ED:A1:AF:D5:A2:40"))
	}
}
