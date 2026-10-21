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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import xyz.d1n0.Log
import xyz.d1n0.lib.connection.WatchConnectionManager
import xyz.d1n0.lib.connection.watchBluetoothDevice

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
	}

	private val log: Log by inject()
	private val watchSessionHandler: WatchSessionHandler by inject()
	private val watchConnectionManager: WatchConnectionManager by inject()

	private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
	@Volatile
	private var session: Job? = null
	@Volatile
	private var connectionActive = false

	override fun onBind(intent: Intent?): IBinder? = null

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		when (intent?.action) {
			ACTION_RUN_BACKGROUND_SYNC -> {
				showConnected(intent.getStringExtra(EXTRA_ADVERTISED_NAME))
				startBackgroundSession(intent.getStringExtra(EXTRA_ADDRESS))
			}
			ACTION_SHOW_CONNECTED -> showConnected(intent.getStringExtra(EXTRA_ADVERTISED_NAME))
			ACTION_CONNECTION_ENDED -> {
				connectionActive = false
				if (session?.isActive != true) stopSelfResult(startId)
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
		// activity is visible.
		startForeground(
			WatchNotifications.CONNECTION_ID,
			WatchNotifications.connected(this, advertisedName),
			ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE,
		)
	}

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
					// service keeps showing its notification until that connection ends.
					if (!connectionActive || watchConnectionManager.watch.value == null) stopSelf()
				}
			}
		}
	}
}
