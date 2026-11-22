package xyz.d1n0.phone

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import xyz.d1n0.Log
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration

/**
 * Rings the phone for find phone, a long press of the watch's lower-right button. The ringing runs
 * in [scope], not in the background session that started it, so another long press is a session of
 * its own that restarts the timer.
 *
 * @param loadSound The sound to ring with, the built-in tone or the user's own.
 * @param scope Lives as long as the app process, tests pass one on virtual time.
 */
class PhoneFinder(
	private val ringer: PhoneRinger,
	private val loadSound: suspend () -> ByteArray,
	private val scope: CoroutineScope,
) {
	private val log = Log

	/** Guards [sound] and [timer], every change to them happens while holding it. */
	private val mutex = Mutex()
	private var sound: Job? = null
	private var timer: Job? = null

	private val _ringing = MutableStateFlow(false)

	/** Whether the phone is ringing, for the in-app Stop and to keep the app alive meanwhile. */
	val ringing: StateFlow<Boolean> = _ringing.asStateFlow()

	/**
	 * Rings for [duration], or until [stop] if null. Called while ringing, it restarts the timer with
	 * the new [duration] and keeps the sound playing, it doesn't start it again.
	 */
	suspend fun ring(duration: Duration?) = mutex.withLock {
		timer?.cancel()
		timer = duration?.let { scope.launch { stopAfter(it) } }
		if (sound?.isActive == true) return@withLock
		_ringing.value = true
		sound = scope.launch { play() }
	}

	/** Stops ringing, e.g. from the notification's Stop. Does nothing if it isn't ringing. */
	suspend fun stop() = mutex.withLock {
		timer?.cancel()
		timer = null
		stopSound()
	}

	private suspend fun stopAfter(duration: Duration) {
		delay(duration)
		// throws if a later ring cancelled this timer meanwhile, so it can't stop the new ringing
		mutex.withLock {
			timer = null
			stopSound()
		}
	}

	/** While holding [mutex]. Waits for the ringer to finish, so [ringing] is false on return. */
	private suspend fun stopSound() {
		sound?.cancelAndJoin()
		sound = null
	}

	private suspend fun play() {
		try {
			ringer.ring(loadSound())
		} catch (e: CancellationException) {
			throw e
		} catch (e: Exception) {
			// a ringer that fails must not leave the app thinking it rings
			log.e { "Ringing failed: ${e.message}" }
		} finally {
			_ringing.value = false
		}
	}
}
