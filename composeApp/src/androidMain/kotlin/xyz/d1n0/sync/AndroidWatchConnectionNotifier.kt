package xyz.d1n0.sync

import android.content.Context
import android.os.Handler
import android.os.Looper

/**
 * Keeps the foreground-service notification aligned with the shared connection lifecycle.
 *
 * The updates are posted to the main thread, where [WatchSyncService] reacts to find phone's
 * ringing, so each one runs wholly before or after that reaction: checked and posted before it,
 * the ringing notification replaces the update, checked after it, the update waits. Checked on the
 * session's thread instead, an update could pass the check just before the ringing starts and
 * replace the ringing notification, the one with Stop.
 *
 * @param isRinging Whether find phone rings, its notification is shown then and these updates
 *        wait, see [WatchSyncService]. The name is still kept for when the ringing stops.
 */
class AndroidWatchConnectionNotifier(
	context: Context,
	private val isRinging: () -> Boolean = { false },
) : WatchConnectionNotifier {
	private val applicationContext = context.applicationContext
	private val mainThread = Handler(Looper.getMainLooper())

	@Volatile
	private var connectionActive = false

	override fun connectionStarted(name: String?) {
		connectionActive = true
		WatchSyncService.showConnected(applicationContext, name)
	}

	override fun connectionNameUpdated(name: String?) = showConnected(name)

	override fun timeSyncStarted() {
		mainThread.post {
			if (connectionActive && !isRinging()) WatchNotifications.updateSyncing(applicationContext)
		}
	}

	override fun timeSyncFinished(name: String?) = showConnected(name)

	override fun connectionEnded() {
		if (!connectionActive) return
		connectionActive = false
		WatchSyncService.connectionEnded(applicationContext)
	}

	// Not gated on connectionActive: the watch disconnected already. WatchSyncService keeps
	// running, with this notification, until the background session that calls this ends.
	override fun savingLocation() {
		mainThread.post {
			if (!isRinging()) WatchNotifications.updateSavingLocation(applicationContext)
		}
	}

	/**
	 * connectionActive is read on the main thread too, an update queued just before the watch
	 * disconnected mustn't post an ongoing notification after the service stopped.
	 */
	private fun showConnected(name: String?) {
		mainThread.post {
			if (!connectionActive) return@post
			if (isRinging()) WatchNotifications.lastConnectedName = name
			else WatchNotifications.updateConnected(applicationContext, name)
		}
	}
}
