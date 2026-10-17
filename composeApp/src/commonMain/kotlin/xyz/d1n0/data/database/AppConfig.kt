package xyz.d1n0.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * App wide settings, the table only ever holds the row with [id] 0.
 *
 * @property installationId Id written to the watch's app info, see [xyz.d1n0.lib.model.AppInfo.id].
 */
@Entity(tableName = "app_config")
class AppConfig(
	@PrimaryKey val id: Int = 0,
	val installationId: ByteArray,
)
