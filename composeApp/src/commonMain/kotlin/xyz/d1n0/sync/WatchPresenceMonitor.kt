package xyz.d1n0.sync

import xyz.d1n0.data.database.PairedWatch

/**
 * Asks the OS to wake the app when a paired watch wants to connect, so time can be adjusted while
 * the app is closed. Android uses the Companion Device Manager, iOS is not supported yet.
 */
interface WatchPresenceMonitor {
	val isSupported: Boolean

	/**
	 * Starts watching for [watch].
	 *
	 * @return [watch], updated with anything the platform needs stored, e.g. an association id.
	 */
	suspend fun register(watch: PairedWatch): PairedWatch

	suspend fun unregister(watch: PairedWatch)
}

/**
 * Used on platforms without background support, background sync never runs.
 */
class UnsupportedWatchPresenceMonitor : WatchPresenceMonitor {
	override val isSupported = false
	override suspend fun register(watch: PairedWatch) = watch
	override suspend fun unregister(watch: PairedWatch) = Unit
}
