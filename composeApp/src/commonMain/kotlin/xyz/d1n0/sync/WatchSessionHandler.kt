package xyz.d1n0.sync

import com.juul.kable.Peripheral
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import xyz.d1n0.Log
import xyz.d1n0.data.database.PairedWatch
import xyz.d1n0.data.repository.InstallationRepository
import xyz.d1n0.data.repository.PairedWatchRepository
import xyz.d1n0.lib.connection.WatchConnectionManager
import xyz.d1n0.lib.constant.ConnectReason
import xyz.d1n0.lib.constant.OpCode
import xyz.d1n0.lib.model.AppInfo
import xyz.d1n0.lib.model.Watch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * What a background session did, so the platform service knows what to do next.
 */
sealed interface BackgroundSessionResult {
	/**
	 * Another watch is connected, e.g. the app is open, nothing was done.
	 */
	data object Busy : BackgroundSessionResult

	/**
	 * Not paired with this install, disconnected without writing anything.
	 */
	data class Skipped(val reason: String) : BackgroundSessionResult

	/**
	 * Time adjusted, or failed with [error], then disconnected.
	 */
	data class Synced(val error: Throwable?) : BackgroundSessionResult

	/**
	 * Lower-left long press. Time adjusted, or failed with [syncError], and still connected, held
	 * in [WatchSessionHandler.pendingForegroundWatch] until the app opens.
	 */
	data class OpenAppRequested(val watch: ConnectedWatch, val syncError: Throwable?) : BackgroundSessionResult

	/**
	 * Find phone (lower-right long press), not supported yet, disconnected.
	 */
	data object Ignored : BackgroundSessionResult

	/**
	 * Connecting or reading from the watch failed or timed out, disconnected.
	 */
	data class Failed(val error: Throwable) : BackgroundSessionResult
}

/**
 * What happens after a watch connects, shared by the UI and background sync.
 */
class WatchSessionHandler(
	private val connectionManager: WatchConnectionManager,
	private val pairedWatchRepository: PairedWatchRepository,
	private val installationRepository: InstallationRepository,
	private val presenceMonitor: WatchPresenceMonitor,
	private val connectionNotifier: WatchConnectionNotifier = WatchConnectionNotifier.None,
) {
	private val log = Log

	private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

	private val _pendingForegroundWatch = MutableStateFlow<ConnectedWatch?>(null)

	/**
	 * Watch connected in background after a lower-left long press, waiting for the app to open and
	 * run [runForeground] on it. Cleared when the watch disconnects.
	 */
	val pendingForegroundWatch: StateFlow<ConnectedWatch?> = _pendingForegroundWatch.asStateFlow()

	/**
	 * Takes [pendingForegroundWatch], so only one caller runs the foreground session for it.
	 */
	fun takePendingForegroundWatch(): ConnectedWatch? = _pendingForegroundWatch.getAndUpdate { null }

	/**
	 * Gives up on [pendingForegroundWatch] and disconnects it.
	 */
	suspend fun dropPendingForegroundWatch() {
		takePendingForegroundWatch()?.let { runCatching { it.disconnect() } }
	}

	/**
	 * Runs when the user connected from the app.
	 *
	 * This function:
	 * 1. Reads the app info, the connect reason (for the watch address) and the watch name.
	 * 2. Pairs the watch, see [pair].
	 * 3. Requests the watch status and adjusts the time.
	 *
	 * Only pairing errors fail the result, a failed watch name, watch status or time adjustment is
	 * logged. Every time adjustment is stored in [PairedWatch], like a background sync.
	 *
	 * @param watch A connected watch.
	 * @return A [Result] that fails if the watch could not be paired.
	 */
	suspend fun runForeground(watch: Watch): Result<Unit> = runForeground(KableConnectedWatch(watch))

	internal suspend fun runForeground(watch: ConnectedWatch): Result<Unit> {
		val result = runCatching {
			val appInfo = AppInfo.fromPacket(watch.query(OpCode.APP_INFO))
			watch.query(OpCode.CONNECT_REASON)
			watch.queryName()
			val address = checkNotNull(watch.info.value.address) { "Watch address unknown" }
			pair(watch, address, appInfo)
		}.onFailure { error ->
			log.e { "Failed to pair watch: ${error.message}" }
		}
		watch.requestWatchStatus()
		adjustTimeAndRecord(watch, watch.info.value.address)
		return result
	}

	/**
	 * Runs when the OS woke the app because a paired watch wants to connect.
	 *
	 * This function:
	 * 1. Returns [BackgroundSessionResult.Busy] if a watch is already connected.
	 * 2. Connects and reads the app info and the connect reason.
	 * 3. Skips watches that aren't paired with this install. Whether the watch connects on its own
	 *    is up to its auto sync setting, a button press is always handled.
	 * 4. Acts on the connect reason:
	 *    - auto or manual sync: adjusts the time and stores the result in [PairedWatch].
	 *    - setup or default (lower-left long press): adjusts the time like a sync, then keeps the
	 *      connection and holds the watch in [pendingForegroundWatch], the caller should ask the
	 *      user to open the app.
	 *    - find: does nothing.
	 * 5. Disconnects, unless the watch is waiting for the app.
	 *
	 * @param peripheral The watch that wants to connect.
	 * @param address BLE address of the watch, used to look up its stored name in case the
	 *        platform doesn't know the peripheral's name.
	 * @param timeout How long the whole session may take.
	 */
	suspend fun runBackground(
		peripheral: Peripheral,
		address: String,
		timeout: Duration = 60.seconds,
	): BackgroundSessionResult = runBackground(address, timeout) { storedName ->
		connectionManager.connectIfIdle(peripheral, storedName)?.let(::KableConnectedWatch)
	}

	internal suspend fun runBackground(
		address: String,
		timeout: Duration = 60.seconds,
		connectIfIdle: suspend (storedName: String?) -> ConnectedWatch?,
	): BackgroundSessionResult {
		var watch: ConnectedWatch? = null
		var keepConnected = false
		try {
			val result = withTimeoutOrNull(timeout) {
				val storedName = pairedWatchRepository.find(address)?.advertisedName
				val connected = connectIfIdle(storedName)
					?: return@withTimeoutOrNull BackgroundSessionResult.Busy
				watch = connected
				backgroundSession(connected)
			} ?: BackgroundSessionResult.Failed(IllegalStateException("Timed out after $timeout"))
			keepConnected = result is BackgroundSessionResult.OpenAppRequested
			return result.also { log.i { "Background session: $it" } }
		} catch (e: TimeoutCancellationException) {
			return BackgroundSessionResult.Failed(e).also { log.e { "Background session timed out: ${e.message}" } }
		} catch (e: CancellationException) {
			throw e
		} catch (e: Exception) {
			return BackgroundSessionResult.Failed(e).also { log.e { "Background session failed: ${e.message}" } }
		} finally {
			if (!keepConnected) watch?.let {
				withContext(NonCancellable) { runCatching { it.disconnect() } }
			}
		}
	}

	/**
	 * Forgets a paired watch.
	 *
	 * This function:
	 * 1. Unregisters it from [WatchPresenceMonitor], so it no longer wakes the app.
	 * 2. Deletes its [PairedWatch] row.
	 * 3. Disconnects the watch if it is the connected one. This comes last, disconnecting sends
	 *    the UI back to the pairing screen, which can cancel the caller's coroutine.
	 *
	 * The watch keeps our app info, connecting to it from the app pairs it again.
	 *
	 * @param address BLE address of the watch.
	 */
	suspend fun forget(address: String) {
		forget(
			address = address,
			connectedWatch = connectionManager.watch.value?.let(::KableConnectedWatch),
		)
	}

	internal suspend fun forget(address: String, connectedWatch: ConnectedWatch?) {
		pairedWatchRepository.find(address)?.let { presenceMonitor.unregister(it) }
		pairedWatchRepository.remove(address)
		log.i { "Forgot $address" }
		connectedWatch
			?.takeIf { it.info.value.address == address }
			?.let { runCatching { it.disconnect() } }
	}

	private suspend fun backgroundSession(watch: ConnectedWatch): BackgroundSessionResult {
		val appInfo = AppInfo.fromPacket(watch.query(OpCode.APP_INFO))
		watch.query(OpCode.CONNECT_REASON)
		val address = checkNotNull(watch.info.value.address) { "Watch address unknown" }
		val connectReason = checkNotNull(watch.info.value.connectReason) { "Connect reason unknown" }

		when {
			appInfo != installationRepository.appInfo() ->
				return BackgroundSessionResult.Skipped("$address is paired with another app")

			pairedWatchRepository.find(address) == null ->
				return BackgroundSessionResult.Skipped("$address is not paired")
		}

		// the user may have renamed the watch since it was paired
		watch.queryName()
		watchDisplayName(watch)?.let { pairedWatchRepository.updateDisplayName(address, it) }

		return when (connectReason) {
			ConnectReason.AUTO_SYNC, ConnectReason.MANUAL_SYNC ->
				BackgroundSessionResult.Synced(adjustTimeAndRecord(watch, address))

			ConnectReason.SETUP, ConnectReason.DEFAULT -> {
				// before holding the watch, so the app can't take it while the time is still being written
				val syncError = adjustTimeAndRecord(watch, address)
				holdForApp(watch)
				BackgroundSessionResult.OpenAppRequested(watch, syncError)
			}

			ConnectReason.FIND -> BackgroundSessionResult.Ignored
		}
	}

	/**
	 * Adjusts the time and stores the result in [PairedWatch].
	 *
	 * @param address BLE address of the watch, nothing is stored if it is unknown.
	 * @return Why the time adjustment failed, null if it succeeded.
	 */
	private suspend fun adjustTimeAndRecord(watch: ConnectedWatch, address: String?): Throwable? {
		val connectionName = watchConnectionName(watch, address)
		connectionNotifier.timeSyncStarted()
		val error = try {
			watch.adjustTime().exceptionOrNull()
		} finally {
			connectionNotifier.timeSyncFinished(connectionName)
		}
		address?.let {
			pairedWatchRepository.recordSync(
				address = it,
				error = error?.let { e -> e.message ?: e::class.simpleName ?: "Unknown error" },
			)
		}
		return error
	}

	/**
	 * Pairs the watch with this install:
	 * 1. Writes our app info if the watch holds another one, then checks the watch stored it.
	 * 2. Stores the watch with its display name, keeping the settings of an existing entry.
	 * 3. Registers the watch with [WatchPresenceMonitor] for background sync.
	 */
	private suspend fun pair(watch: ConnectedWatch, address: String, appInfo: AppInfo) {
		val ourAppInfo = installationRepository.appInfo()
		if (appInfo != ourAppInfo) {
			log.i { "Pairing $address, it held $appInfo" }
			watch.writeAppInfo(ourAppInfo)
			check(AppInfo.fromPacket(watch.query(OpCode.APP_INFO)) == ourAppInfo) {
				"Watch did not store our app info"
			}
		}

		val advertisedName = watch.advertisedName ?: address
		val existing = pairedWatchRepository.find(address)
		val displayName = watchDisplayName(watch) ?: existing?.displayName ?: advertisedName
		val pairedWatch = existing?.copy(advertisedName = advertisedName, displayName = displayName)
			?: PairedWatch(
				address = address,
				advertisedName = advertisedName,
				displayName = displayName,
				pairedAt = Clock.System.now(),
			)
		pairedWatchRepository.upsert(presenceMonitor.register(pairedWatch))
	}

	/**
	 * Reads the watch name into [Watch.info], a failed read is logged and leaves the name unknown.
	 */
	private suspend fun ConnectedWatch.queryName() {
		runCatching { query(OpCode.WATCH_NAME) }
			.onFailure { log.w { "Failed to read watch name: ${it.message}" } }
		connectionNotifier.connectionNameUpdated(watchConnectionName(this, info.value.address))
	}

	/**
	 * The name the user set on the watch, null if it is unknown or blank.
	 */
	private fun watchDisplayName(watch: ConnectedWatch): String? =
		watch.info.value.name?.value?.takeIf { it.isNotBlank() }

	/** Live watch name, falling back to its stored display name and then advertised model name. */
	private suspend fun watchConnectionName(watch: ConnectedWatch, address: String?): String? =
		watchDisplayName(watch)
			?: address?.let { pairedWatchRepository.find(it)?.displayName?.takeIf { name -> name.isNotBlank() } }
			?: watch.advertisedName

	private fun holdForApp(watch: ConnectedWatch) {
		_pendingForegroundWatch.value = watch
		scope.launch {
			watch.awaitDisconnected()
			_pendingForegroundWatch.compareAndSet(watch, null)
		}
	}
}
