package xyz.d1n0.data.database

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Room's generated 5 to 6 auto migration, see [autoMigrationSql]. */
class PressedLocationMigrationTest {
	private fun migration5To6SQL() = autoMigrationSql(from = 5, to = 6)

	@Test
	fun version6AddsSettingsWithLocationRecordingOff() {
		val create = migration5To6SQL().single { it.startsWith("CREATE TABLE IF NOT EXISTS `app_settings`") }
		// no row is inserted, the column default only matters once the user changes a setting
		assertTrue("`locationRecordingEnabled` INTEGER NOT NULL DEFAULT 0" in create, create)
		assertFalse(migration5To6SQL().any { it.startsWith("INSERT") })
	}

	@Test
	fun version6OnlyAddsTablesAndLeavesInstallationIdAndPairingsAlone() {
		val sql = migration5To6SQL()
		assertTrue(sql.any { it.startsWith("CREATE TABLE IF NOT EXISTS `pressed_location`") }, "$sql")
		// the installation id and the pairings must survive, see SkuareDatabase
		assertFalse(sql.any { "DROP" in it || "ALTER" in it || "app_config" in it || "paired_watch" in it }, "$sql")
		// two tables, two indices on pressed_location
		assertEquals(4, sql.size, "$sql")
	}
}
