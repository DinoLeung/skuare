package xyz.d1n0.data.database

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlin.time.Instant

/**
 * Where the phone was when the watch's lower-right button was pressed briefly, recorded when the
 * user turned on [AppSettings.locationRecordingEnabled]. Kept until the user deletes it.
 *
 * There is no foreign key to [PairedWatch]: forgetting a watch asks whether to delete its rows, so
 * they can outlive it.
 *
 * @property watchAddress BLE address of the watch, see [PairedWatch.address].
 * @property watchName The watch's display name at the press, the only name left once the watch is
 *           forgotten.
 * @property pressedAt When the watch said why it connected, i.e. the press.
 * @property accuracyMeters See [xyz.d1n0.phone.GeoPoint.accuracyMeters].
 */
@Entity(
	tableName = "pressed_location",
	indices = [Index("watchAddress"), Index("pressedAt")],
)
data class PressedLocation(
	@PrimaryKey(autoGenerate = true) val id: Long = 0,
	val watchAddress: String,
	val watchName: String,
	val pressedAt: Instant,
	val latitude: Double,
	val longitude: Double,
	val accuracyMeters: Double?,
)

/**
 * A [PressedLocation] with the current name of its watch, null once the watch is forgotten.
 */
data class PressedLocationWithWatch(
	@Embedded val location: PressedLocation,
	val pairedDisplayName: String?,
) {
	/** The watch's current name while it is paired, else its name at the press. */
	val watchName: String
		get() = pairedDisplayName?.takeIf { it.isNotBlank() } ?: location.watchName
}

/** How many locations a watch has, see [PressedLocationDao.observeCounts]. */
data class PressedLocationCount(
	val watchAddress: String,
	val count: Int,
)
