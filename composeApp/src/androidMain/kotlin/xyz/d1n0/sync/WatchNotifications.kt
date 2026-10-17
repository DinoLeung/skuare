package xyz.d1n0.sync

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import xyz.d1n0.Log

object WatchNotifications {
	private const val CHANNEL_ID = "watch_connection"
	private const val OPEN_APP_PROMPT_CHANNEL_ID = "open_app_prompt"
	private const val OPEN_APP_PROMPT_ID = 2

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

	/**
	 * Asks the user to open the app for a watch that connected after a lower-left long press.
	 * Android doesn't let the app start an activity from the background, so this is the only way.
	 *
	 * Tapping it opens the app, which takes the waiting watch. Dismissing it disconnects the watch,
	 * see [OpenAppPromptDismissedReceiver]. It is not auto cancelled, [WatchSyncService] removes it
	 * with [cancelOpenAppPrompt] once the watch is taken or disconnects.
	 *
	 * @param watchName Name shown as the title, e.g. CASIO GW-B5600#.
	 */
	fun showOpenAppPrompt(context: Context, watchName: String?) {
		if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
			Log.w { "Notification permission not granted, can't ask to open the app" }
			return
		}
		createOpenAppPromptChannel(context)

		val openApp = PendingIntent.getActivity(
			context,
			0,
			// same as the launcher, brings an existing task to the front instead of stacking a new one
			context.packageManager.getLaunchIntentForPackage(context.packageName),
			PendingIntent.FLAG_IMMUTABLE,
		)
		val dismissed = PendingIntent.getBroadcast(
			context,
			0,
			Intent(context, OpenAppPromptDismissedReceiver::class.java),
			PendingIntent.FLAG_IMMUTABLE,
		)
		val notification = Notification.Builder(context, OPEN_APP_PROMPT_CHANNEL_ID)
			.setSmallIcon(android.R.drawable.stat_notify_sync)
			.setContentTitle(watchName ?: "Watch connected")
			.setContentText("Tap to open skuare")
			.setContentIntent(openApp)
			.setDeleteIntent(dismissed)
			.build()
		context.getSystemService(NotificationManager::class.java).notify(OPEN_APP_PROMPT_ID, notification)
	}

	fun cancelOpenAppPrompt(context: Context) {
		context.getSystemService(NotificationManager::class.java).cancel(OPEN_APP_PROMPT_ID)
	}

	private fun createChannel(context: Context) {
		context.getSystemService(NotificationManager::class.java).createNotificationChannel(
			NotificationChannel(CHANNEL_ID, "Watch connection", NotificationManager.IMPORTANCE_LOW)
		)
	}

	/**
	 * High importance, so the prompt pops up. On the low importance [CHANNEL_ID] it went into the
	 * silent section of the shade and was easy to miss, while the watch only waits until its
	 * connection timeout.
	 */
	private fun createOpenAppPromptChannel(context: Context) {
		context.getSystemService(NotificationManager::class.java).createNotificationChannel(
			NotificationChannel(OPEN_APP_PROMPT_CHANNEL_ID, "Open app prompt", NotificationManager.IMPORTANCE_HIGH)
		)
	}
}
