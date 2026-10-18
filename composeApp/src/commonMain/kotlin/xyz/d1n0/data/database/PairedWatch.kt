package xyz.d1n0.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.time.Instant

/**
 * A watch paired with this app, i.e. its app info holds our installation id.
 *
 * @property address BLE address of the watch, e.g. ED:A1:AF:D5:A2:40, see [xyz.d1n0.lib.model.WatchInfo.address].
 * @property advertisedName Name the watch advertises, e.g. CASIO GMW-B5000#.
 * @property displayName Name the user set on the watch, see [xyz.d1n0.lib.model.WatchInfo.name]. Falls
 *           back to [advertisedName] until the watch has told us its name.
 * @property lastSyncAt When the last time sync finished, in the background or from the app, null if it never ran.
 * @property lastSyncError Why the last time sync failed, null if it succeeded.
 * @property platformAssociationId Android Companion Device Manager association id, unused on iOS.
 */
@Entity(tableName = "paired_watch")
data class PairedWatch(
	@PrimaryKey val address: String,
	val advertisedName: String,
	val displayName: String,
	val pairedAt: Instant,
	val lastSyncAt: Instant? = null,
	val lastSyncError: String? = null,
	val platformAssociationId: Int? = null,
)
