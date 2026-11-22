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
 * @property findPhoneEnabled Whether a long press of the lower-right button rings the phone. Off
 *           until the user turns it on.
 * @property findPhoneRingUntilStopped Rings until the user stops it, rather than for
 *           [findPhoneRingSeconds]. Its own column, not a missing length, so turning it off brings
 *           back the length the user typed before.
 * @property findPhoneRingSeconds How long the phone rings, in whole seconds, at least 1.
 * @property findPhoneSoundName The display name of the user's own ringtone, null for the built-in
 *           tone. The sound itself is a file, kept apart from the database.
 * @property findPhoneMaxVolume Whether the phone rings at the maximum alarm volume, or at the one
 *           the user set. On, as find phone rang before it was a setting.
 */
@Entity(tableName = "app_settings")
data class AppSettings(
	@PrimaryKey val id: Int = 0,
	@ColumnInfo(defaultValue = "0") val locationRecordingEnabled: Boolean = false,
	@ColumnInfo(defaultValue = "0") val findPhoneEnabled: Boolean = false,
	@ColumnInfo(defaultValue = "0") val findPhoneRingUntilStopped: Boolean = false,
	@ColumnInfo(defaultValue = "$DEFAULT_RING_SECONDS") val findPhoneRingSeconds: Int = DEFAULT_RING_SECONDS,
	val findPhoneSoundName: String? = null,
	@ColumnInfo(defaultValue = "1") val findPhoneMaxVolume: Boolean = true,
) {
	companion object {
		/** A minute, the column default and the fallback for a length that isn't valid. */
		const val DEFAULT_RING_SECONDS = 60
	}
}
