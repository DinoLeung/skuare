package xyz.d1n0.data.database

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

/** Room's generated 8 to 9 auto migration, see [autoMigrationSql]. */
class LocationNameMigrationTest {
	@Test
	fun version9AddsAnEmptyNameSoRecordedLocationsKeepTheWatchsName() {
		assertEquals(
			listOf("ALTER TABLE `pressed_location` ADD COLUMN `name` TEXT DEFAULT NULL"),
			autoMigrationSql(from = 8, to = 9),
		)
		// a location recorded by the app and one upgraded from version 8 must read the same
		assertNull(
			PressedLocation(
				watchAddress = "ED:A1:AF:D5:A2:40",
				watchName = "CASIO GW-B5600",
				pressedAt = Instant.parse("2026-11-20T08:00:00Z"),
				latitude = 1.0,
				longitude = 2.0,
				accuracyMeters = null,
			).name
		)
	}
}
