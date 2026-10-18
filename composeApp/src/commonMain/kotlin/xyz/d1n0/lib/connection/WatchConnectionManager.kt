package xyz.d1n0.lib.connection

import com.juul.kable.ExperimentalKableApi
import com.juul.kable.Peripheral
import com.juul.kable.State
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import xyz.d1n0.Log
import xyz.d1n0.lib.model.Watch

/**
 * Owns the connection to the watch. It is the only place that creates or drops a [Watch], so the
 * UI and background sync never connect to a watch at the same time.
 */
class WatchConnectionManager : KoinComponent {
	private val log: Log by inject()

	private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
	private val connectMutex = Mutex()

	private val _watch = MutableStateFlow<Watch?>(null)

	/**
	 * The connected watch, or null when no watch is connected.
	 */
	val watch: StateFlow<Watch?> = _watch.asStateFlow()

	/**
	 * @return The connected watch.
	 * @throws IllegalStateException if no watch is connected.
	 */
	fun requireWatch(): Watch = _watch.value ?: error("No watch connected")

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
	suspend fun connect(peripheral: Peripheral): Watch = connectMutex.withLock {
		_watch.value?.let { previous ->
			runCatching { previous.disconnect() }
			release(previous)
		}
		connectLocked(peripheral)
	}

	/**
	 * Like [connect], but leaves the current watch alone. Used by background sync, so it never
	 * takes over a watch the UI is connected to.
	 *
	 * @param peripheral The peripheral to connect to, closed if another watch is connected.
	 * @param storedName Name stored for the watch, e.g. [xyz.d1n0.data.database.PairedWatch.advertisedName].
	 *        Used when the platform doesn't know the peripheral's name, which can happen when the
	 *        peripheral is made from an address, so the watch still gets the right model.
	 * @return The connected watch, or null if another watch is already connected.
	 */
	@OptIn(ExperimentalKableApi::class)
	suspend fun connectIfIdle(peripheral: Peripheral, storedName: String? = null): Watch? = connectMutex.withLock {
		if (_watch.value != null) {
			peripheral.close()
			null
		} else {
			connectLocked(peripheral, advertisedName = peripheral.name ?: storedName)
		}
	}

	@OptIn(ExperimentalKableApi::class)
	private suspend fun connectLocked(peripheral: Peripheral, advertisedName: String? = peripheral.name): Watch {
		val watch = Watch(peripheral, advertisedName)
		runCatching { watch.connect() }
			.onFailure { watch.close() }
			.getOrThrow()
		_watch.value = watch

		scope.launch {
			watch.state
				.dropWhile { it !is State.Connected }
				.first { it is State.Disconnected }
			log.d { "Watch disconnected" }
			release(watch)
		}

		return watch
	}

	/**
	 * Disconnects the current watch, if there is one. It is dropped from [watch] once disconnected.
	 */
	suspend fun disconnect() {
		_watch.value?.disconnect()
	}

	private fun release(watch: Watch) {
		if (_watch.compareAndSet(watch, null)) watch.close()
	}
}
