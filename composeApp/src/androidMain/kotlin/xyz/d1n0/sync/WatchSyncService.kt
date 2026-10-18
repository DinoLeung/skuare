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
import xyz.d1n0.lib.connection.watchBluetoothDevice

/**
 * Runs [WatchSessionHandler.runBackground] for a watch that wants to connect while the app is
 * closed, started by [WatchCompanionService].
 *
 * It is a foreground service rather than work inside [WatchCompanionService], the watch stops
 * advertising once connected, so the system may report it gone and unbind that service in the
 * middle of a sync.
 */
class WatchSyncService : Service(), KoinComponent {
	companion object {
		private const val EXTRA_ADDRESS = "address"
		private const val NOTIFICATION_ID = 1

		/**
		 * @param address BLE address of the watch, e.g. ED:A1:AF:D5:A2:40.
		 */
		fun start(context: Context, address: String) {
			context.startForegroundService(
				Intent(context, WatchSyncService::class.java).putExtra(EXTRA_ADDRESS, address)
			)
		}
	}

	private val log: Log by inject()
	private val watchSessionHandler: WatchSessionHandler by inject()

	private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
	private var session: Job? = null

	override fun onBind(intent: Intent?): IBinder? = null

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		// every startForegroundService call must be answered with startForeground
		startForeground(
			NOTIFICATION_ID,
			WatchNotifications.syncing(this),
			ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE,
		)

		val address = intent?.getStringExtra(EXTRA_ADDRESS)
		when {
			// the presence callback can fire more than once for one connection
			session?.isActive == true -> log.d { "Background session already running, ignoring $address" }
			address == null -> stopSelf()
			else -> session = scope.launch {
				try {
					runSession(address)
				} finally {
					stopSelf()
				}
			}
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
			WatchNotifications.showOpenAppPrompt(this, result.watch.advertisedName)
			try {
				// stay in the foreground, holding the connection, until the app takes the watch, it
				// disconnects or the prompt is dismissed
				watchSessionHandler.pendingForegroundWatch.first { it == null }
			} finally {
				WatchNotifications.cancelOpenAppPrompt(this)
			}
		}
	}
}
