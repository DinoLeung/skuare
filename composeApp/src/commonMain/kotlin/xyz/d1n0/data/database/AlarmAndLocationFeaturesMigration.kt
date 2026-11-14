package xyz.d1n0.data.database

import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import xyz.d1n0.lib.model.CustomWatchModel

/**
 * Database version 2 to 3, adds [CustomWatchModel.hasSnoozeAlarm] and
 * [CustomWatchModel.hasTimeZoneLocation].
 *
 * The new columns are nullable like the rest of the custom model, and must stay NULL on watches
 * without one: Room only reads the custom model as null when all its columns are NULL. So they
 * get no column default, and only watches with a custom model are filled in here, see
 * [FILL_NEW_FEATURES_SQL].
 */
class AlarmAndLocationFeaturesMigration : AutoMigrationSpec {
	override fun onPostMigrate(connection: SQLiteConnection) {
		connection.execSQL(FILL_NEW_FEATURES_SQL)
	}

	companion object {
		/**
		 * Custom watches keep their snooze alarm, as before, and stop writing the time zone
		 * location until the user turns it on, the [CustomWatchModel] default.
		 */
		internal val FILL_NEW_FEATURES_SQL = """
			UPDATE paired_watch SET
				model_hasSnoozeAlarm = ${CustomWatchModel().hasSnoozeAlarm.toSqlInt()},
				model_hasTimeZoneLocation = ${CustomWatchModel().hasTimeZoneLocation.toSqlInt()}
			WHERE model_clockCount IS NOT NULL
		""".trimIndent()

		private fun Boolean.toSqlInt() = if (this) 1 else 0
	}
}
