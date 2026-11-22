package xyz.d1n0.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The user's choices on the App settings screen. The table only ever holds the row with [id] 0,
 * and only once something was changed: no row means every setting has its default.
 *
 * Kept apart from [AppConfig], which holds the installation id, so a setting never needs that row.
 * A new setting is a new column with a default, so existing installs keep the current behaviour.
 *
 * @property locationRecordingEnabled Whether a short press of the watch's lower-right button also
 *           records the phone's location, see [PressedLocation]. Off until the user turns it on.
 */
@Entity(tableName = "app_settings")
data class AppSettings(
	@PrimaryKey val id: Int = 0,
	@ColumnInfo(defaultValue = "0") val locationRecordingEnabled: Boolean = false,
)
