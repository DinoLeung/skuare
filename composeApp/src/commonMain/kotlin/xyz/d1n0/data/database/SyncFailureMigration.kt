package xyz.d1n0.data.database

import androidx.room.RenameColumn
import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Database version 4 to 5, stores why the last time sync failed as a [SyncFailure] so it can be
 * shown in the user's language.
 *
 * lastSyncError held the error's message, it becomes [PairedWatch.lastSyncErrorDetail]. The kind
 * of an old failure isn't known, so it becomes [SyncFailure.OTHER] with its message, see
 * [FILL_SYNC_FAILURE_SQL].
 */
@RenameColumn(tableName = "paired_watch", fromColumnName = "lastSyncError", toColumnName = "lastSyncErrorDetail")
class SyncFailureMigration : AutoMigrationSpec {
	override fun onPostMigrate(connection: SQLiteConnection) {
		connection.execSQL(FILL_SYNC_FAILURE_SQL)
	}

	companion object {
		internal val FILL_SYNC_FAILURE_SQL = """
			UPDATE paired_watch SET lastSyncFailure = '${SyncFailure.OTHER.name}'
			WHERE lastSyncErrorDetail IS NOT NULL
		""".trimIndent()
	}
}
