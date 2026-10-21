package xyz.d1n0.sync

/**
 * Reports the parts of a watch connection that need to be visible to the user.
 *
 * Android implements this with an ongoing foreground-service notification. Other platforms use
 * [None] until they have an equivalent implementation.
 */
interface WatchConnectionNotifier {
	/** Called immediately before the BLE connection attempt starts with its best known name. */
	fun connectionStarted(name: String?)

	/** Updates the connected message after the user-defined watch name has been read. */
	fun connectionNameUpdated(name: String?)

	/** Called immediately before the watch time is adjusted. */
	fun timeSyncStarted()

	/** Restores the connected state after the time adjustment finishes. */
	fun timeSyncFinished(name: String?)

	/** Called after the BLE connection ends or its connection attempt fails. */
	fun connectionEnded()

	object None : WatchConnectionNotifier {
		override fun connectionStarted(name: String?) = Unit
		override fun connectionNameUpdated(name: String?) = Unit
		override fun timeSyncStarted() = Unit
		override fun timeSyncFinished(name: String?) = Unit
		override fun connectionEnded() = Unit
	}
}
