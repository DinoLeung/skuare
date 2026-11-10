package xyz.d1n0.data.database

import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.SQLiteConnection
import xyz.d1n0.lib.model.CustomWatchModel
import xyz.d1n0.lib.model.WatchModel

/**
 * Database version 1 to 2. Gives every watch paired before custom models existed the model its
 * advertised name decides, see [migratedCustomModel].
 *
 * It uses the [WatchModel] list of the app running it, so a watch that became known in a later
 * release gets no custom model. Keep it compiling against [WatchModel] and [CustomWatchModel].
 */
class CustomWatchModelMigration : AutoMigrationSpec {
	override fun onPostMigrate(connection: SQLiteConnection) {
		// read everything first, so no statement is open while updating
		val rows = connection.prepare("SELECT address, advertisedName FROM paired_watch").use { statement ->
			buildList { while (statement.step()) add(statement.getText(0) to statement.getText(1)) }
		}
		rows.forEach { (address, advertisedName) ->
			val model = migratedCustomModel(advertisedName) ?: return@forEach
			connection.prepare(
				"""
				UPDATE paired_watch SET
					model_clockCount = ?, model_reminderCount = ?, model_hasFontStyle = ?,
					model_hasShortCityNames = ?, model_hasWeekdayLanguage = ?
				WHERE address = ?
				""".trimIndent()
			).use { statement ->
				statement.bindLong(1, model.clockCount.toLong())
				statement.bindLong(2, model.reminderCount.toLong())
				statement.bindBoolean(3, model.hasFontStyle)
				statement.bindBoolean(4, model.hasShortCityNames)
				statement.bindBoolean(5, model.hasWeekdayLanguage)
				statement.bindText(6, address)
				statement.step()
			}
		}
	}
}

/**
 * The custom model a watch paired before custom models gets: none if [advertisedName] is a known
 * [WatchModel], else the default [CustomWatchModel], which the user can then adjust.
 *
 * @param advertisedName Stored advertised name, the address if the watch didn't advertise one.
 */
internal fun migratedCustomModel(advertisedName: String): CustomWatchModel? =
	if (WatchModel.fromAdvertisedName(advertisedName) == null) CustomWatchModel() else null
