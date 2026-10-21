package xyz.d1n0.sync

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context

object WatchNotifications {
	private const val CHANNEL_ID = "watch_connection"
	const val CONNECTION_ID = 1

	/**
	 * Ongoing notification of [WatchSyncService]. Immediate foreground-service behavior prevents
	 * Android from deferring its display during a short connection.
	 */
	fun connected(context: Context, name: String?): Notification =
		connectionNotification(context, "Connected to ${name ?: "watch"}")

	fun syncing(context: Context): Notification = connectionNotification(context, "Syncing time")

	fun updateConnected(context: Context, name: String?) {
		context.getSystemService(NotificationManager::class.java)
			.notify(CONNECTION_ID, connected(context, name))
	}

	fun updateSyncing(context: Context) {
		context.getSystemService(NotificationManager::class.java)
			.notify(CONNECTION_ID, syncing(context))
	}

	private fun connectionNotification(context: Context, status: String): Notification {
		createChannel(context)
		val openApp = PendingIntent.getActivity(
			context,
			0,
			context.packageManager.getLaunchIntentForPackage(context.packageName),
			PendingIntent.FLAG_IMMUTABLE,
		)
		return Notification.Builder(context, CHANNEL_ID)
			.setSmallIcon(android.R.drawable.stat_notify_sync)
			.setContentTitle(status)
			.setContentIntent(openApp)
			.setCategory(Notification.CATEGORY_SERVICE)
			.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
			.setOnlyAlertOnce(true)
			.setOngoing(true)
			.build()
	}

	private fun createChannel(context: Context) {
		context.getSystemService(NotificationManager::class.java).createNotificationChannel(
			NotificationChannel(CHANNEL_ID, "Watch connection", NotificationManager.IMPORTANCE_LOW)
		)
	}
}
