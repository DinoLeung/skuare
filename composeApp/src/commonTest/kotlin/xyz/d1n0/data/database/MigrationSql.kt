package xyz.d1n0.data.database

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement

/**
 * The SQL of Room's generated auto migration [from] to [to], run against a connection that only
 * records it: the bundled SQLite driver doesn't load in unit tests.
 */
internal fun autoMigrationSql(from: Int, to: Int): List<String> {
	val migration = SkuareDatabaseConstructor.initialize()
		.createAutoMigrations(emptyMap())
		.single { it.startVersion == from && it.endVersion == to }
	return RecordingConnection().also { migration.migrate(it) }.statements
}

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
