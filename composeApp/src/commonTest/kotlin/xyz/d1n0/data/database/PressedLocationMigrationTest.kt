package xyz.d1n0.data.database

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Runs Room's generated 5 to 6 auto migration against a connection that only records its SQL, the
 * bundled SQLite driver doesn't load in unit tests.
 */
class PressedLocationMigrationTest {
	private class RecordingConnection : SQLiteConnection {
		val statements = mutableListOf<String>()

		override fun prepare(sql: String): SQLiteStatement {
			statements += sql
			return NoResultStatement
		}

		override fun close() = Unit
	}

	/** A statement for SQL that returns no rows, as a migration's ALTER and CREATE do. */
	private object NoResultStatement : SQLiteStatement {
		override fun bindBlob(index: Int, value: ByteArray) = unused()
		override fun bindDouble(index: Int, value: Double) = unused()
		override fun bindLong(index: Int, value: Long) = unused()
		override fun bindText(index: Int, value: String) = unused()
		override fun bindNull(index: Int) = unused()
		override fun getBlob(index: Int): ByteArray = unused()
		override fun getDouble(index: Int): Double = unused()
		override fun getLong(index: Int): Long = unused()
		override fun getText(index: Int): String = unused()
		override fun isNull(index: Int): Boolean = unused()
		override fun getColumnCount() = 0
		override fun getColumnName(index: Int): String = unused()
		override fun getColumnType(index: Int): Int = unused()
		override fun step() = false
		override fun reset() = Unit
		override fun clearBindings() = Unit
		override fun close() = Unit

		private fun unused(): Nothing = error("The migration doesn't read or bind values")
	}

	private fun migration5To6SQL(): List<String> {
		val migration = SkuareDatabaseConstructor.initialize()
			.createAutoMigrations(emptyMap())
			.single { it.startVersion == 5 && it.endVersion == 6 }
		return RecordingConnection().also { migration.migrate(it) }.statements
	}

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
