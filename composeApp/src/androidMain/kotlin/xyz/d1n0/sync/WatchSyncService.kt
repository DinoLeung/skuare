package xyz.d1n0.sync

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import com.juul.kable.Peripheral
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import xyz.d1n0.Log
import xyz.d1n0.lib.connection.WatchConnectionManager
import xyz.d1n0.lib.connection.watchBluetoothDevice
import xyz.d1n0.phone.PhoneFinder
import kotlin.time.Instant

/**
 * Owns the ongoing notification for every watch connection. It also runs
 * [WatchSessionHandler.runBackground] when [WatchCompanionService] starts a background sync.
 *
 * Keeping the connection in a foreground service makes the notification and BLE work independent
 * of whether the activity is visible, backgrounded, or removed from Recents. Background work does
 * not run directly in [WatchCompanionService] because a connected watch stops advertising, so the
 * system may report it gone and unbind that service in the middle of a sync.
 */
class WatchSyncService : Service(), KoinComponent {
	companion object {
		private const val ACTION_RUN_BACKGROUND_SYNC = "xyz.d1n0.action.RUN_BACKGROUND_SYNC"
		private const val ACTION_SHOW_CONNECTED = "xyz.d1n0.action.SHOW_CONNECTED"
		private const val ACTION_CONNECTION_ENDED = "xyz.d1n0.action.CONNECTION_ENDED"
		private const val ACTION_STOP_RINGING = "xyz.d1n0.action.STOP_RINGING"
		private const val EXTRA_ADDRESS = "address"
		private const val EXTRA_ADVERTISED_NAME = "advertised_name"

		/**
		 * @param address BLE address of the watch, e.g. ED:A1:AF:D5:A2:40.
		 */
		fun start(context: Context, address: String, advertisedName: String?) {
			context.startForegroundService(
				Intent(context, WatchSyncService::class.java)
					.setAction(ACTION_RUN_BACKGROUND_SYNC)
					.putExtra(EXTRA_ADDRESS, address)
					.putExtra(EXTRA_ADVERTISED_NAME, advertisedName)
			)
		}

		fun showConnected(context: Context, advertisedName: String?) {
			context.startForegroundService(
				Intent(context, WatchSyncService::class.java)
					.setAction(ACTION_SHOW_CONNECTED)
					.putExtra(EXTRA_ADVERTISED_NAME, advertisedName)
			)
		}

		fun connectionEnded(context: Context) {
			// The service is already running in the foreground, so updating it from the background is
			// allowed. It waits for any background session to unwind before stopping itself.
			context.startService(
				Intent(context, WatchSyncService::class.java).setAction(ACTION_CONNECTION_ENDED)
			)
		}

		/** Stop on the find phone notification, and swiping it away. */
		fun stopRingingIntent(context: Context): Intent =
			Intent(context, WatchSyncService::class.java).setAction(ACTION_STOP_RINGING)
	}

	private val log: Log by inject()
	private val watchSessionHandler: WatchSessionHandler by inject()
	private val watchConnectionManager: WatchConnectionManager by inject()
	private val phoneFinder: PhoneFinder by inject()

	private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
	@Volatile
	private var session: Job? = null
	@Volatile
	private var connectionActive = false

	override fun onBind(intent: Intent?): IBinder? = null

	override fun onCreate() {
		super.onCreate()
		// Find phone rings outside any session, see PhoneFinder. While it rings, this service's
		// notification is the ringing one and the service stays up, a long press session started it.
		scope.launch(Dispatchers.Main) {
			var wasRinging = false
			combine(phoneFinder.ringing, phoneFinder.ringEndsAt) { ringing, endsAt -> ringing to endsAt }
				.collect { (ringing, endsAt) ->
					if (ringing) showRinging(endsAt)
					else if (wasRinging) ringingStopped()
					wasRinging = ringing
				}
		}
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		when (intent?.action) {
			ACTION_RUN_BACKGROUND_SYNC -> {
				showConnected(intent.getStringExtra(EXTRA_ADVERTISED_NAME))
				startBackgroundSession(intent.getStringExtra(EXTRA_ADDRESS))
			}
			ACTION_SHOW_CONNECTED -> showConnected(intent.getStringExtra(EXTRA_ADVERTISED_NAME))
			ACTION_CONNECTION_ENDED -> {
				connectionActive = false
				if (session?.isActive != true && !phoneFinder.ringing.value) stopSelfResult(startId)
			}
			ACTION_STOP_RINGING -> scope.launch {
				phoneFinder.stop()
				// started by the notification after the ringing ended on its own, nothing else to do
				if (isIdle()) stopSelf()
			}
			else -> stopSelfResult(startId)
		}
		return START_NOT_STICKY
	}

	override fun onDestroy() {
		scope.cancel()
		super.onDestroy()
	}

	private suspend fun runSession(address: String) {
		val result = watchSessionHandler.runBackground(Peripheral(watchBluetoothDevice(address)), address)
		if (result is BackgroundSessionResult.OpenAppRequested) {
			// Keep the connection available if the user opens the app themselves. The ongoing
			// connection notification remains; no separate open-app prompt is posted.
			watchSessionHandler.pendingForegroundWatch.first { it == null }
		}
	}

	private fun showConnected(advertisedName: String?) {
		connectionActive = true
		// Every startForegroundService call is answered immediately, including updates while the
		// activity is visible. While find phone rings, its notification stays, a watch connecting
		// meanwhile, e.g. for another long press, doesn't replace it.
		val notification =
			if (phoneFinder.ringing.value) {
				WatchNotifications.lastConnectedName = advertisedName
				WatchNotifications.ringing(this, phoneFinder.ringEndsAt.value)
			} else {
				WatchNotifications.connected(this, advertisedName)
			}
		startForeground(WatchNotifications.CONNECTION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
	}

	private fun showRinging(endsAt: Instant?) {
		startForeground(
			WatchNotifications.CONNECTION_ID,
			WatchNotifications.ringing(this, endsAt),
			ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE,
		)
	}

	/** Back to the connection notification while a watch is still connected, else done. */
	private fun ringingStopped() {
		if (isIdle()) stopSelf()
		else showConnected(WatchNotifications.lastConnectedName)
	}

	/** No background session, no watch connection and no ringing left to show. */
	private fun isIdle() =
		session?.isActive != true && !phoneFinder.ringing.value &&
			(!connectionActive || watchConnectionManager.watch.value == null)

	private fun startBackgroundSession(address: String?) {
		when {
			// The presence callback can fire more than once for one connection.
			session?.isActive == true -> log.d { "Background session already running, ignoring $address" }
			address == null -> {
				connectionActive = false
				stopSelf()
			}
			else -> session = scope.launch {
				try {
					runSession(address)
				} finally {
					session = null
					// If the app accepted an open-app handoff, the watch is still connected and this
					// service keeps showing its notification until that connection ends. A long press
					// leaves the phone ringing, the service stays for that too.
					if (isIdle()) stopSelf()
				}
			}
		}
	}
}
