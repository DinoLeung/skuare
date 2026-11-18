package xyz.d1n0.sync

import com.juul.kable.NotConnectedException
import kotlinx.coroutines.TimeoutCancellationException
import xyz.d1n0.data.database.SyncFailure
import xyz.d1n0.lib.model.WatchErrorPacket
import xyz.d1n0.lib.model.WatchTimeoutException

/** The [SyncFailure] to store for this error. */
internal val Throwable.syncFailure: SyncFailure
	get() = when (this) {
		is WatchErrorPacket -> SyncFailure.WATCH_ERROR
		is WatchTimeoutException, is TimeoutCancellationException -> SyncFailure.TIMEOUT
		is NotConnectedException -> SyncFailure.CONNECTION_LOST
		else -> SyncFailure.OTHER
	}

/** Technical detail to store with [syncFailure], see [xyz.d1n0.data.database.PairedWatch.lastSyncErrorDetail]. */
internal val Throwable.syncErrorDetail: String?
	get() = when (this) {
		is WatchErrorPacket -> hex
		else -> message ?: this::class.simpleName
	}
