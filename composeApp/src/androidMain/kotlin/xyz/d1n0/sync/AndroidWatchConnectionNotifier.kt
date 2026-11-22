package xyz.d1n0.sync

import android.content.Context

/**
 * Keeps the foreground-service notification aligned with the shared connection lifecycle.
 *
 * @param isRinging Whether find phone rings, its notification is shown then and these updates
 *        wait, see [WatchSyncService]. The name is still kept for when the ringing stops.
 */
class AndroidWatchConnectionNotifier(
	context: Context,
	private val isRinging: () -> Boolean = { false },
) : WatchConnectionNotifier {
	private val applicationContext = context.applicationContext

	@Volatile
	private var connectionActive = false

	override fun connectionStarted(name: String?) {
		connectionActive = true
		WatchSyncService.showConnected(applicationContext, name)
	}

	override fun connectionNameUpdated(name: String?) = showConnected(name)

	override fun timeSyncStarted() {
		if (connectionActive && !isRinging()) WatchNotifications.updateSyncing(applicationContext)
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
		if (!isRinging()) WatchNotifications.updateSavingLocation(applicationContext)
	}

	private fun showConnected(name: String?) {
		if (!connectionActive) return
		if (isRinging()) WatchNotifications.lastConnectedName = name
		else WatchNotifications.updateConnected(applicationContext, name)
	}
}
