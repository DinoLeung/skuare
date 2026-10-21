package xyz.d1n0.sync

/**
 * Reports the parts of a watch connection that need to be visible to the user.
 *
 * Android implements this with an ongoing foreground-service notification. Other platforms use
 * [None] until they have an equivalent implementation.
 */
interface WatchConnectionNotifier {
	/** Called immediately before the BLE connection attempt starts. */
	fun connectionStarted(advertisedName: String?)

	/** Called immediately before the watch time is adjusted. */
	fun timeSyncStarted()

	/** Restores the connected state after the time adjustment finishes. */
	fun timeSyncFinished(advertisedName: String?)

	/** Called after the BLE connection ends or its connection attempt fails. */
	fun connectionEnded()

	object None : WatchConnectionNotifier {
		override fun connectionStarted(advertisedName: String?) = Unit
		override fun timeSyncStarted() = Unit
		override fun timeSyncFinished(advertisedName: String?) = Unit
		override fun connectionEnded() = Unit
	}
}
