package xyz.d1n0.phone

import kotlinx.coroutines.awaitCancellation
import kotlin.coroutines.cancellation.CancellationException

/**
 * [PhoneRinger] that "rings" until cancelled, like the real ones, and records it. With [failure]
 * set, it throws that instead of ringing.
 */
internal class FakePhoneRinger(
	override val isSupported: Boolean = true,
	override val canForceMaxVolume: Boolean = true,
) : PhoneRinger {
	var failure: Exception? = null

	/** The sound of each ring, in order. */
	val sounds = mutableListOf<ByteArray>()
	var cancelCount = 0
		private set

	override suspend fun ring(sound: ByteArray) {
		sounds += sound
		failure?.let { throw it }
		try {
			awaitCancellation()
		} catch (e: CancellationException) {
			cancelCount++
			throw e
		}
	}
}
