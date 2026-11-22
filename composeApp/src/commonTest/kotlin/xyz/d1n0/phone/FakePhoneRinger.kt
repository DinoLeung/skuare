package xyz.d1n0.phone

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration

/**
 * [PhoneRinger] that "rings" until cancelled, like the real ones, and records it. With [failure]
 * set, it throws that instead of ringing, with [failsOn] only for that sound.
 */
internal class FakePhoneRinger(
	override val isSupported: Boolean = true,
	override val canForceMaxVolume: Boolean = true,
) : PhoneRinger {
	var failure: Exception? = null
	var failsOn: ByteArray? = null

	/** What [canPlay] says, every sound plays by default. */
	var playable: (ByteArray) -> Boolean = { true }

	/** The sound of each ring, in order. */
	val sounds = mutableListOf<ByteArray>()

	/** The forceMaxVolume of each ring, in order. */
	val maxVolumes = mutableListOf<Boolean>()

	/** The vibrate of each ring, in order. */
	val vibrates = mutableListOf<Boolean>()

	var cancelCount = 0
		private set

	/** How long stopping takes once cancelled, like a player released off the main thread. */
	var stopTakes: Duration = Duration.ZERO

	override suspend fun ring(sound: ByteArray, forceMaxVolume: Boolean, vibrate: Boolean) {
		sounds += sound
		maxVolumes += forceMaxVolume
		vibrates += vibrate
		failure?.takeIf { failsOn == null || failsOn.contentEquals(sound) }?.let { throw it }
		try {
			awaitCancellation()
		} catch (e: CancellationException) {
			cancelCount++
			if (stopTakes > Duration.ZERO) withContext(NonCancellable) { delay(stopTakes) }
			throw e
		}
	}

	override suspend fun canPlay(sound: ByteArray) = playable(sound)
}
