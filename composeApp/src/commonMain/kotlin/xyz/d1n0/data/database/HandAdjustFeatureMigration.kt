package xyz.d1n0.data.database

import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import xyz.d1n0.lib.model.CustomWatchModel

/**
 * Database version 3 to 4, adds [CustomWatchModel.canAdjustHands].
 *
 * Like [AlarmAndLocationFeaturesMigration], the new column has no default and is only filled in
 * for watches with a custom model, see [FILL_CAN_ADJUST_HANDS_SQL]. A watch without one must keep
 * all its model columns NULL.
 */
class HandAdjustFeatureMigration : AutoMigrationSpec {
	override fun onPostMigrate(connection: SQLiteConnection) {
		connection.execSQL(FILL_CAN_ADJUST_HANDS_SQL)
	}

	companion object {
		/** Custom watches start without hand adjustment, the [CustomWatchModel] default. */
		internal val FILL_CAN_ADJUST_HANDS_SQL = """
			UPDATE paired_watch SET
				model_canAdjustHands = ${if (CustomWatchModel().canAdjustHands) 1 else 0}
			WHERE model_clockCount IS NOT NULL
		""".trimIndent()
	}
}
