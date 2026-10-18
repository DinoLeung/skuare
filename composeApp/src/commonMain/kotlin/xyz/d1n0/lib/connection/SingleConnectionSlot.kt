package xyz.d1n0.lib.connection

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Owns at most one connected resource and releases it when disconnection is observed.
 */
internal class SingleConnectionSlot<T>(
	private val scope: CoroutineScope,
	private val disconnect: suspend (T) -> Unit,
	private val awaitDisconnected: suspend (T) -> Unit,
	private val close: (T) -> Unit,
	private val onDisconnected: () -> Unit = {},
) {
	private val mutex = Mutex()
	private val _value = MutableStateFlow<T?>(null)

	val value: StateFlow<T?> = _value.asStateFlow()

	fun requireValue(): T = _value.value ?: error("No watch connected")

	suspend fun replace(connect: suspend () -> T): T = mutex.withLock {
		_value.value?.let { previous ->
			runCatching { disconnect(previous) }
			release(previous)
		}
		connect().also(::hold)
	}

	suspend fun connectIfIdle(onBusy: () -> Unit, connect: suspend () -> T): T? = mutex.withLock {
		if (_value.value != null) {
			onBusy()
			null
		} else {
			connect().also(::hold)
		}
	}

	suspend fun disconnect() {
		_value.value?.let { disconnect(it) }
	}

	private fun hold(value: T) {
		_value.value = value
		scope.launch {
			awaitDisconnected(value)
			onDisconnected()
			release(value)
		}
	}

	private fun release(value: T) {
		if (_value.compareAndSet(value, null)) close(value)
	}
}
