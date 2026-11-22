package xyz.d1n0.data.database

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Room's generated 6 to 7 auto migration, see [autoMigrationSql]. */
class FindPhoneSettingsMigrationTest {
	private val sql = autoMigrationSql(from = 6, to = 7)

	@Test
	fun version7AddsFindPhoneOffForAMinuteWithTheBuiltInTone() {
		assertEquals(
			listOf(
				"ALTER TABLE `app_settings` ADD COLUMN `findPhoneEnabled` INTEGER NOT NULL DEFAULT 0",
				"ALTER TABLE `app_settings` ADD COLUMN `findPhoneRingUntilStopped` INTEGER NOT NULL DEFAULT 0",
				"ALTER TABLE `app_settings` ADD COLUMN `findPhoneRingSeconds` INTEGER NOT NULL DEFAULT 60",
				"ALTER TABLE `app_settings` ADD COLUMN `findPhoneSoundName` TEXT DEFAULT NULL",
			),
			sql,
		)
	}

	@Test
	fun columnDefaultsMatchTheEntity() {
		// a row created by the app and one upgraded from version 6 must read the same
		val defaults = AppSettings()
		assertEquals(false, defaults.findPhoneEnabled)
		assertEquals(false, defaults.findPhoneRingUntilStopped)
		assertTrue(sql.any { it.endsWith("`findPhoneRingSeconds` INTEGER NOT NULL DEFAULT ${defaults.findPhoneRingSeconds}") })
		assertEquals(null, defaults.findPhoneSoundName)
	}

	@Test
	fun version8AddsFullVolumeOnAsFindPhoneRangBefore() {
		assertEquals(
			listOf("ALTER TABLE `app_settings` ADD COLUMN `findPhoneMaxVolume` INTEGER NOT NULL DEFAULT 1"),
			autoMigrationSql(from = 7, to = 8),
		)
		assertTrue(AppSettings().findPhoneMaxVolume)
	}
}
