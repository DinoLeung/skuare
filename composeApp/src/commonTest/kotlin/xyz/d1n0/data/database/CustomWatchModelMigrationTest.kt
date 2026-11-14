package xyz.d1n0.data.database

import xyz.d1n0.lib.model.CustomWatchModel
import kotlin.test.Test
import kotlin.test.assertContains
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
			"CASIO GA-B2100",
			"CASIO GBM-2100",
			"CASIO GM-B2100",
		).forEach { assertNull(migratedCustomModel(it), it) }
	}

	@Test
	fun version3FillsNewFeaturesOfCustomWatchesOnly() {
		val sql = AlarmAndLocationFeaturesMigration.FILL_NEW_FEATURES_SQL
		// custom watches keep their snooze alarm and start without the time zone location
		assertContains(sql, "model_hasSnoozeAlarm = 1")
		assertContains(sql, "model_hasTimeZoneLocation = 0")
		// a watch without a custom model must keep all its model columns NULL
		assertContains(sql, "WHERE model_clockCount IS NOT NULL")
	}

	@Test
	fun version4FillsHandAdjustmentOfCustomWatchesOnly() {
		val sql = HandAdjustFeatureMigration.FILL_CAN_ADJUST_HANDS_SQL
		// custom watches start without hand adjustment
		assertContains(sql, "model_canAdjustHands = 0")
		assertContains(sql, "WHERE model_clockCount IS NOT NULL")
	}

	@Test
	fun unknownWatchGetsDefaultCustomModel() {
		assertEquals(CustomWatchModel(), migratedCustomModel("CASIO GBD-H2000"))
		assertEquals(CustomWatchModel(), migratedCustomModel("CASIO GW-B5600BC"))
	}

	@Test
	fun watchStoredByAddressGetsDefaultCustomModel() {
		// the address is stored as the name when the watch didn't advertise one
		assertEquals(CustomWatchModel(), migratedCustomModel("ED:A1:AF:D5:A2:40"))
	}
}
