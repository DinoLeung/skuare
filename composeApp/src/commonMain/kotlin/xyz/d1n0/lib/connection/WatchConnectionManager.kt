package xyz.d1n0.lib.connection

import com.juul.kable.ExperimentalKableApi
import com.juul.kable.Peripheral
import com.juul.kable.State
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import xyz.d1n0.Log
import xyz.d1n0.data.database.PairedWatch
import xyz.d1n0.data.repository.PairedWatchRepository
import xyz.d1n0.lib.model.CustomWatchModel
import xyz.d1n0.lib.model.Watch
import xyz.d1n0.sync.WatchConnectionNotifier

/**
 * Owns the connection to the watch. It is the only place that creates or drops a [Watch], so the
 * UI and background sync never connect to a watch at the same time.
 *
 * @param scope Where each watch is awaited until it disconnects, a test scope in tests.
 */
class WatchConnectionManager(
	private val connectionNotifier: WatchConnectionNotifier = WatchConnectionNotifier.None,
	private val pairedWatchRepository: PairedWatchRepository? = null,
	scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
	private val log = Log

	private val slot = SingleConnectionSlot(
		scope = scope,
		disconnect = Watch::disconnect,
		awaitDisconnected = { watch -> watch.state.first { it is State.Disconnected } },
		close = Watch::close,
		onDisconnected = {
			connectionNotifier.connectionEnded()
			log.d { "Watch disconnected" }
		},
	)

	/**
	 * The connected watch, or null when no watch is connected.
	 */
	val watch: StateFlow<Watch?> = slot.value

	/**
	 * @return The connected watch.
	 * @throws IllegalStateException if no watch is connected.
	 */
	fun requireWatch(): Watch = slot.requireValue()

	/**
	 * Connects to [peripheral] and makes it the current watch.
	 *
	 * This function:
	 * 1. Disconnects and closes the current watch, if there is one.
	 * 2. Connects to [peripheral], closing it if the connection fails.
	 * 3. Stores the watch in [watch].
	 * 4. Drops the watch from [watch] and closes it once it disconnects.
	 *
	 * @param peripheral The peripheral to connect to.
	 * @return The connected watch.
	 */
	suspend fun connect(peripheral: Peripheral): Watch = slot.replace { connectPeripheral(peripheral) }

	/**
	 * Like [connect], but with [Watch.demo], to try the app without a watch. It uses no Bluetooth
	 * and shows no connection notification. Background sync leaves it alone, like any connected
	 * watch, see [connectIfIdle].
	 *
	 * @return The connected demo watch.
	 */
	suspend fun connectDemo(): Watch = slot.replace {
		// the watch it replaced is released by replace, which doesn't report its end, and the demo
		// watch starts no connection to update the notification, so end it here
		connectionNotifier.connectionEnded()
		Watch.demo().also { it.connect() }
	}

	/**
	 * Like [connect], but leaves the current watch alone. Used by background sync, so it never
	 * takes over a watch the UI is connected to.
	 *
	 * @param peripheral The peripheral to connect to, closed if another watch is connected.
	 * @param stored The watch's stored row, null if it isn't paired. Its
	 *        [PairedWatch.advertisedName] is used when the platform doesn't know the peripheral's
	 *        name, which can happen when the peripheral is made from an address, and its
	 *        [PairedWatch.customModel] is used for an unrecognised watch, so the watch gets the
	 *        right model either way.
	 * @return The connected watch, or null if another watch is already connected.
	 */
	@OptIn(ExperimentalKableApi::class)
	suspend fun connectIfIdle(peripheral: Peripheral, stored: PairedWatch? = null): Watch? =
		slot.connectIfIdle(
			onBusy = peripheral::close,
			connect = {
				connectPeripheral(
					peripheral = peripheral,
					advertisedName = peripheral.name ?: stored?.advertisedName,
					customModel = stored?.customModel,
				)
			},
		)

	@OptIn(ExperimentalKableApi::class)
	@Suppress("REDUNDANT_CALL_OF_CONVERSION_METHOD") // Android Identifier is a String; other targets are not.
	private suspend fun connectPeripheral(
		peripheral: Peripheral,
		advertisedName: String? = peripheral.name,
		customModel: CustomWatchModel? = null,
	): Watch {
		val watch = Watch(peripheral, advertisedName, customModel)
		val notificationName = resolveInitialNotificationName(
			identifier = peripheral.identifier.toString(),
			peripheralName = peripheral.name,
		)
		connectionNotifier.connectionStarted(notificationName)
		runCatching { watch.connect() }
			.onFailure {
				connectionNotifier.connectionEnded()
				watch.close()
			}
			.getOrThrow()
		return watch
	}

	internal suspend fun resolveInitialNotificationName(identifier: String, peripheralName: String?): String? =
		pairedWatchRepository
			?.find(identifier.uppercase())
			?.displayName
			?.takeIf { it.isNotBlank() }
			?: peripheralName

	/**
	 * Disconnects the current watch, if there is one. It is dropped from [watch] once disconnected.
	 */
	suspend fun disconnect() {
		slot.disconnect()
	}
}
