package xyz.d1n0.sync

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

object WatchNotifications {
	private const val CHANNEL_ID = "watch_connection"

	/**
	 * Notification of [WatchSyncService]. Android usually doesn't show it at all, foreground
	 * service notifications are delayed by up to 10 seconds and a sync is shorter.
	 */
	fun syncing(context: Context): Notification {
		createChannel(context)
		return Notification.Builder(context, CHANNEL_ID)
			.setSmallIcon(android.R.drawable.stat_notify_sync)
			.setContentTitle("Connected to watch")
			.setOngoing(true)
			.build()
	}

	private fun createChannel(context: Context) {
		context.getSystemService(NotificationManager::class.java).createNotificationChannel(
			NotificationChannel(CHANNEL_ID, "Watch connection", NotificationManager.IMPORTANCE_LOW)
		)
	}
}
