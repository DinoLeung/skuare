package xyz.d1n0.sync

import com.juul.kable.Peripheral
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import xyz.d1n0.Log
import xyz.d1n0.data.database.PairedWatch
import xyz.d1n0.data.repository.AppSettingsRepository
import xyz.d1n0.data.repository.InstallationRepository
import xyz.d1n0.data.repository.PairedWatchRepository
import xyz.d1n0.data.repository.PressedLocationRepository
import xyz.d1n0.lib.connection.WatchConnectionManager
import xyz.d1n0.lib.constant.ConnectReason
import xyz.d1n0.lib.constant.OpCode
import xyz.d1n0.lib.model.AppInfo
import xyz.d1n0.lib.model.CustomWatchModel
import xyz.d1n0.lib.model.Watch
import xyz.d1n0.phone.LocationProvider
import xyz.d1n0.phone.LocationResult
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

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
	 *
	 * @property location What reading the phone's location gave after a lower-right short press,
	 *           null if it wasn't asked for, see [AppSettingsRepository.isLocationRecordingEnabled].
	 */
	data class Synced(val error: Throwable?, val location: LocationResult? = null) : BackgroundSessionResult

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
	private val appSettingsRepository: AppSettingsRepository,
	private val pressedLocationRepository: PressedLocationRepository,
	private val locationProvider: LocationProvider,
	private val connectionNotifier: WatchConnectionNotifier = WatchConnectionNotifier.None,
) {
	private companion object {
		/**
		 * How long a background session waits for a location fix after the watch disconnected. The
		 * read starts at the press, so it also has the whole watch session before that.
		 */
		val LOCATION_TIMEOUT = 10.seconds

		/** The oldest last known location that still counts as where the phone was at the press. */
		val LOCATION_MAX_AGE = 2.minutes
	}

	private val log = Log

	private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

	/**
	 * Held while a location is stored and while a watch is forgotten, so a read still finishing
	 * can't store a location for a watch the user just forgot, see [finishLocationRead].
	 */
	private val locationStore = Mutex()

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
	 * 3. Switches an unrecognised watch to its stored custom model, so the time is adjusted with
	 *    the right clock count.
	 * 4. Requests the watch status and adjusts the time.
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
			val customModel = pair(watch, address, appInfo).customModel
			// a known watch has no custom model, its features are fixed
			if (customModel != null && watch.model is CustomWatchModel) watch.useCustomModel(customModel)
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
	 *    - auto sync: adjusts the time and stores the result in [PairedWatch].
	 *    - manual sync (lower-right short press): the same, and if the user turned on location
	 *      recording, also reads the phone's location and stores it as a
	 *      [xyz.d1n0.data.database.PressedLocation]. The read starts before the time adjustment.
	 *    - setup or default (lower-left long press): adjusts the time like a sync, then keeps the
	 *      connection and holds the watch in [pendingForegroundWatch], the caller should ask the
	 *      user to open the app.
	 *    - find: does nothing.
	 * 5. Disconnects, unless the watch is waiting for the app.
	 * 6. Waits for the location read, if one started, so the watch isn't kept connected for it: up to
	 *    10 s from the disconnect, then a recent last known location, see [finishLocationRead].
	 *
	 * @param peripheral The watch that wants to connect.
	 * @param address BLE address of the watch, used to look up its stored row, for its name in case
	 *        the platform doesn't know the peripheral's name, and its custom model.
	 * @param timeout How long the session with the watch may take, the location read after it has
	 *        its own limit.
	 */
	suspend fun runBackground(
		peripheral: Peripheral,
		address: String,
		timeout: Duration = 60.seconds,
	): BackgroundSessionResult = runBackground(address, timeout) { stored ->
		connectionManager.connectIfIdle(peripheral, stored)?.let(::KableConnectedWatch)
	}

	internal suspend fun runBackground(
		address: String,
		timeout: Duration = 60.seconds,
		connectIfIdle: suspend (stored: PairedWatch?) -> ConnectedWatch?,
	): BackgroundSessionResult = coroutineScope {
		// started in this scope rather than inside the timeout, so it outlives the watch session
		var locationRead: LocationRead? = null
		val result = runWatchSession(address, timeout, connectIfIdle) { request ->
			locationRead = LocationRead(request, async { readCurrentLocation() })
		}
		val read = locationRead ?: return@coroutineScope result
		// the watch is gone, the notification shouldn't say it's still connected meanwhile
		if (read.fix.isActive) connectionNotifier.savingLocation()
		val location = finishLocationRead(read)
		// the kind only, a Fix holds the phone's position, which doesn't belong in the system log
		log.i { "Location after the button press: ${location::class.simpleName}" }
		if (result is BackgroundSessionResult.Synced) result.copy(location = location) else result
	}

	/**
	 * The part of [runBackground] with the watch connected, disconnects it before returning unless
	 * it waits for the app.
	 *
	 * @param startLocationRead Starts reading the phone's location without waiting for it.
	 */
	private suspend fun runWatchSession(
		address: String,
		timeout: Duration,
		connectIfIdle: suspend (stored: PairedWatch?) -> ConnectedWatch?,
		startLocationRead: (LocationRequest) -> Unit,
	): BackgroundSessionResult {
		var watch: ConnectedWatch? = null
		var keepConnected = false
		try {
			val result = withTimeoutOrNull(timeout) {
				val connected = connectIfIdle(pairedWatchRepository.find(address))
					?: return@withTimeoutOrNull BackgroundSessionResult.Busy
				watch = connected
				backgroundSession(connected, startLocationRead)
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
	 * @param deleteLocations Also deletes the locations recorded with this watch, the user chose
	 *        it in the forget dialog. Kept otherwise, pairing the watch again finds them by address.
	 */
	suspend fun forget(address: String, deleteLocations: Boolean) {
		forget(
			address = address,
			deleteLocations = deleteLocations,
			connectedWatch = connectionManager.watch.value?.let(::KableConnectedWatch),
		)
	}

	internal suspend fun forget(address: String, deleteLocations: Boolean, connectedWatch: ConnectedWatch?) {
		pairedWatchRepository.find(address)?.let { presenceMonitor.unregister(it) }
		locationStore.withLock {
			pairedWatchRepository.remove(address)
			if (deleteLocations) pressedLocationRepository.deleteFor(address)
		}
		log.i { "Forgot $address${if (deleteLocations) " and its locations" else ""}" }
		connectedWatch
			?.takeIf { it.info.value.address == address }
			?.let { runCatching { it.disconnect() } }
	}

	private suspend fun backgroundSession(
		watch: ConnectedWatch,
		startLocationRead: (LocationRequest) -> Unit,
	): BackgroundSessionResult {
		val appInfo = AppInfo.fromPacket(watch.query(OpCode.APP_INFO))
		watch.query(OpCode.CONNECT_REASON)
		// the watch connects right after the button press
		val pressedAt = Clock.System.now()
		val address = checkNotNull(watch.info.value.address) { "Watch address unknown" }
		val connectReason = checkNotNull(watch.info.value.connectReason) { "Connect reason unknown" }

		if (appInfo != installationRepository.appInfo())
			return BackgroundSessionResult.Skipped("$address is paired with another app")
		val stored = pairedWatchRepository.find(address)
			?: return BackgroundSessionResult.Skipped("$address is not paired")

		// the watch may have become known or unrecognised since it was stored, e.g. after an update
		if (watch.model is CustomWatchModel) storeDefaultCustomModelIfMissing(stored)
		else clearStoredCustomModel(watch, stored)

		// the user may have renamed the watch since it was paired
		watch.queryName()
		watchDisplayName(watch)?.let { pairedWatchRepository.updateDisplayName(address, it) }

		return when (connectReason) {
			ConnectReason.AUTO_SYNC -> BackgroundSessionResult.Synced(adjustTimeAndRecord(watch, address))

			ConnectReason.MANUAL_SYNC -> {
				if (locationProvider.isSupported && appSettingsRepository.isLocationRecordingEnabled()) {
					val watchName = watchConnectionName(watch, address) ?: address
					startLocationRead(LocationRequest(address, watchName, pressedAt))
				}
				BackgroundSessionResult.Synced(adjustTimeAndRecord(watch, address))
			}

			ConnectReason.SETUP, ConnectReason.DEFAULT -> {
				// before holding the watch, so the app can't take it while the time is still being written
				val syncError = adjustTimeAndRecord(watch, address)
				holdForApp(watch)
				BackgroundSessionResult.OpenAppRequested(watch, syncError)
			}

			ConnectReason.FIND -> BackgroundSessionResult.Ignored
		}
	}

	/** A fresh fix, waited for until [finishLocationRead] gives up. Never throws, except for cancellation. */
	private suspend fun readCurrentLocation(): LocationResult =
		try {
			locationProvider.currentLocation()
		} catch (e: CancellationException) {
			throw e
		} catch (e: Exception) {
			LocationResult.Failed(e)
		}

	/**
	 * Waits up to [LOCATION_TIMEOUT] from now, after the watch session, for the fix started at the
	 * press. Without one, it stops the read and takes a last known location up to
	 * [LOCATION_MAX_AGE] old. Stores what it got for the press, unless the watch was forgotten
	 * meanwhile: the user may have asked to delete its locations, a late one mustn't come back.
	 * Never throws, except for cancellation: a failure is returned and logged, the time sync
	 * doesn't depend on it.
	 */
	private suspend fun finishLocationRead(read: LocationRead): LocationResult {
		val fresh = withTimeoutOrNull(LOCATION_TIMEOUT) { read.fix.await() }
		if (fresh == null) read.fix.cancel()
		val result = when (fresh) {
			null, LocationResult.TimedOut -> lastKnownLocation() ?: LocationResult.TimedOut
			else -> fresh
		}
		val request = read.request
		if (result !is LocationResult.Fix) {
			log.w { "No location recorded for ${request.address}: $result" }
			return result
		}
		return try {
			locationStore.withLock {
				if (pairedWatchRepository.find(request.address) == null) {
					log.i { "No location recorded for ${request.address}, it was forgotten meanwhile" }
				} else {
					pressedLocationRepository.record(
						watchAddress = request.address,
						watchName = request.watchName,
						pressedAt = request.pressedAt,
						point = result.point,
					)
				}
			}
			result
		} catch (e: CancellationException) {
			throw e
		} catch (e: Exception) {
			log.e { "Failed to store location for ${request.address}: ${e.message}" }
			LocationResult.Failed(e)
		}
	}

	private suspend fun lastKnownLocation(): LocationResult.Fix? =
		try {
			locationProvider.lastKnownLocation(LOCATION_MAX_AGE)?.let { LocationResult.Fix(it) }
		} catch (e: CancellationException) {
			throw e
		} catch (e: Exception) {
			log.w { "Failed to read the last known location: ${e.message}" }
			null
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
				failure = error?.syncFailure,
				errorDetail = error?.syncErrorDetail,
			)
		}
		return error
	}

	/**
	 * Pairs the watch with this install:
	 * 1. Writes our app info if the watch holds another one, then checks the watch stored it.
	 * 2. Stores the watch with its display name, keeping the settings of an existing entry, its
	 *    custom model and intro flag included. An unrecognised watch without a custom model gets
	 *    the default [CustomWatchModel]. A known watch has none, one stored while it wasn't
	 *    recognised, e.g. before skuare supported it, is cleared with its intro flag.
	 * 3. Registers the watch with [WatchPresenceMonitor] for background sync.
	 *
	 * @return The stored watch.
	 */
	private suspend fun pair(watch: ConnectedWatch, address: String, appInfo: AppInfo): PairedWatch {
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
		val customModel = if (watch.model is CustomWatchModel) {
			existing?.customModel ?: CustomWatchModel()
		} else {
			existing?.customModel?.also { logClearingCustomModel(address, watch) }
			null
		}
		val pairedWatch = existing?.copy(
			advertisedName = advertisedName,
			displayName = displayName,
			customModel = customModel,
			// the intro is about the custom model, a known watch never shows it
			customModelIntroShown = customModel != null && existing.customModelIntroShown,
		) ?: PairedWatch(
			address = address,
			advertisedName = advertisedName,
			displayName = displayName,
			pairedAt = Clock.System.now(),
			customModel = customModel,
		)
		return presenceMonitor.register(pairedWatch).also { pairedWatchRepository.upsert(it) }
	}

	/**
	 * Stores the default [CustomWatchModel] for an unrecognised watch stored without one, e.g. one
	 * paired while its model was known. The watch already uses it, see [xyz.d1n0.lib.model.resolveWatchModel], and
	 * the Adjust button and intro only show once it is stored. [pair] does the same when
	 * connecting from the app.
	 */
	private suspend fun storeDefaultCustomModelIfMissing(stored: PairedWatch) {
		if (stored.customModel != null) return
		log.i { "Storing the default custom model of ${stored.address}, it isn't recognised now" }
		pairedWatchRepository.upsert(stored.copy(customModel = CustomWatchModel(), customModelIntroShown = false))
	}

	/**
	 * Clears the custom model stored while the watch wasn't recognised, and its intro flag, for a
	 * watch that is a known [xyz.d1n0.lib.model.WatchModel] now, e.g. after an update added it.
	 * Nothing can edit it then, the Adjust button only shows for custom watches. [pair] does the
	 * same when connecting from the app.
	 */
	private suspend fun clearStoredCustomModel(watch: ConnectedWatch, stored: PairedWatch) {
		if (stored.customModel == null) return
		logClearingCustomModel(stored.address, watch)
		pairedWatchRepository.clearCustomModel(stored.address)
	}

	private fun logClearingCustomModel(address: String, watch: ConnectedWatch) =
		log.i { "Clearing custom model of $address, it is ${watch.model}" }

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

	/** A lower-right short press whose location is to be recorded. */
	private class LocationRequest(val address: String, val watchName: String, val pressedAt: Instant)

	/** The fresh fix for [request], started at the press. */
	private class LocationRead(val request: LocationRequest, val fix: Deferred<LocationResult>)

	private fun holdForApp(watch: ConnectedWatch) {
		_pendingForegroundWatch.value = watch
		scope.launch {
			watch.awaitDisconnected()
			_pendingForegroundWatch.compareAndSet(watch, null)
		}
	}
}
