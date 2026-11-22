package xyz.d1n0.sync

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import xyz.d1n0.R
import kotlin.time.Instant

object WatchNotifications {
	private const val CHANNEL_ID = "watch_connection"
	private const val FIND_PHONE_CHANNEL_ID = "find_phone"
	const val CONNECTION_ID = 1

	/**
	 * The watch's name in the last connection notification, to show it again when find phone stops
	 * ringing while a watch is still connected.
	 */
	@Volatile
	var lastConnectedName: String? = null

	/**
	 * Ongoing notification of [WatchSyncService]. Immediate foreground-service behavior prevents
	 * Android from deferring its display during a short connection.
	 */
	fun connected(context: Context, name: String?): Notification {
		lastConnectedName = name
		return connectionNotification(
			context,
			name?.let { context.getString(R.string.notification_connected, it) }
				?: context.getString(R.string.notification_connected_unnamed),
		)
	}

	fun syncing(context: Context): Notification =
		connectionNotification(context, context.getString(R.string.notification_syncing))

	fun updateConnected(context: Context, name: String?) {
		context.getSystemService(NotificationManager::class.java)
			.notify(CONNECTION_ID, connected(context, name))
	}

	fun updateSyncing(context: Context) {
		context.getSystemService(NotificationManager::class.java)
			.notify(CONNECTION_ID, syncing(context))
	}

	/**
	 * The watch disconnected and the phone's location is still being read. Replaces the
	 * foreground notification, so it goes away with [WatchSyncService].
	 */
	fun updateSavingLocation(context: Context) {
		context.getSystemService(NotificationManager::class.java)
			.notify(CONNECTION_ID, connectionNotification(context, context.getString(R.string.notification_saving_location)))
	}

	/**
	 * [WatchSyncService]'s notification while find phone rings, in place of the connection one: shown
	 * heads-up and on the lock screen, with Stop. Swiping it away, which Android 14+ allows for
	 * foreground service notifications, stops the ringing too, so it never rings without one.
	 *
	 * @param endsAt When the ringing stops by itself, counted down, null while it rings until stopped.
	 */
	fun ringing(context: Context, endsAt: Instant?): Notification {
		createFindPhoneChannel(context)
		val stop = PendingIntent.getService(
			context,
			0,
			WatchSyncService.stopRingingIntent(context),
			PendingIntent.FLAG_IMMUTABLE,
		)
		return Notification.Builder(context, FIND_PHONE_CHANNEL_ID)
			.setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
			.setContentTitle(context.getString(R.string.notification_find_phone_title))
			.setContentIntent(openApp(context))
			.setDeleteIntent(stop)
			.addAction(
				Notification.Action.Builder(null, context.getString(R.string.notification_find_phone_stop), stop).build()
			)
			.setCategory(Notification.CATEGORY_ALARM)
			.setVisibility(Notification.VISIBILITY_PUBLIC)
			.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
			.setOngoing(true)
			.apply {
				if (endsAt != null) {
					setWhen(endsAt.toEpochMilliseconds())
					setUsesChronometer(true)
					setChronometerCountDown(true)
				} else {
					setShowWhen(false)
				}
			}
			.build()
	}

	private fun connectionNotification(context: Context, status: String): Notification {
		createChannel(context)
		return Notification.Builder(context, CHANNEL_ID)
			.setSmallIcon(android.R.drawable.stat_notify_sync)
			.setContentTitle(status)
			.setContentIntent(openApp(context))
			.setCategory(Notification.CATEGORY_SERVICE)
			.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
			.setOnlyAlertOnce(true)
			.setOngoing(true)
			.build()
	}

	private fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
		context,
		0,
		context.packageManager.getLaunchIntentForPackage(context.packageName),
		PendingIntent.FLAG_IMMUTABLE,
	)

	private fun createChannel(context: Context) {
		context.getSystemService(NotificationManager::class.java).createNotificationChannel(
			NotificationChannel(
				CHANNEL_ID,
				context.getString(R.string.notification_channel_watch_connection),
				NotificationManager.IMPORTANCE_LOW,
			)
		)
	}

	/** High importance, so it shows heads-up. Silent: the ringer plays its own sound and vibrates. */
	private fun createFindPhoneChannel(context: Context) {
		context.getSystemService(NotificationManager::class.java).createNotificationChannel(
			NotificationChannel(
				FIND_PHONE_CHANNEL_ID,
				context.getString(R.string.notification_channel_find_phone),
				NotificationManager.IMPORTANCE_HIGH,
			).apply {
				setSound(null, null)
				enableVibration(false)
				lockscreenVisibility = Notification.VISIBILITY_PUBLIC
			}
		)
	}
}
