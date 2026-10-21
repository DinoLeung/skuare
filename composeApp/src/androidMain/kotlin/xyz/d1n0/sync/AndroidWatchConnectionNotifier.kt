package xyz.d1n0.sync

import android.content.Context

/** Keeps the foreground-service notification aligned with the shared connection lifecycle. */
class AndroidWatchConnectionNotifier(context: Context) : WatchConnectionNotifier {
	private val applicationContext = context.applicationContext

	@Volatile
	private var connectionActive = false

	override fun connectionStarted(advertisedName: String?) {
		connectionActive = true
		WatchSyncService.showConnected(applicationContext, advertisedName)
	}

	override fun timeSyncStarted() {
		if (connectionActive) WatchNotifications.updateSyncing(applicationContext)
	}

	override fun timeSyncFinished(advertisedName: String?) {
		if (connectionActive) WatchNotifications.updateConnected(applicationContext, advertisedName)
	}

	override fun connectionEnded() {
		if (!connectionActive) return
		connectionActive = false
		WatchSyncService.connectionEnded(applicationContext)
	}
}
