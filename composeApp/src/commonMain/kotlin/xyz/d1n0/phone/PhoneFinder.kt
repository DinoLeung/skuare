package xyz.d1n0.phone

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import xyz.d1n0.Log
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

/**
 * Rings the phone for find phone, a long press of the watch's lower-right button. The ringing runs
 * in [scope], not in the background session that started it, so another long press is a session of
 * its own that restarts the timer.
 *
 * @param loadSound The built-in tone, always there.
 * @param loadCustomSound The user's own ringtone, null for the built-in tone, see [FindPhoneSounds].
 *        If it fails to load or to play, the phone rings with [loadSound], it never stays silent.
 * @param scope Lives as long as the app process, tests pass one on virtual time.
 * @param clock For [ringEndsAt], tests pass one on virtual time.
 */
class PhoneFinder(
	private val ringer: PhoneRinger,
	private val loadSound: suspend () -> ByteArray,
	private val scope: CoroutineScope,
	private val loadCustomSound: suspend () -> ByteArray? = { null },
	private val clock: Clock = Clock.System,
) {
	private val log = Log

	/** Guards [sound] and [timer], every change to them happens while holding it. */
	private val mutex = Mutex()
	private var sound: Job? = null
	private var timer: Job? = null

	private val _ringing = MutableStateFlow(false)

	/** Whether the phone is ringing, for the in-app Stop and to keep the app alive meanwhile. */
	val ringing: StateFlow<Boolean> = _ringing.asStateFlow()

	private val _ringEndsAt = MutableStateFlow<Instant?>(null)

	/**
	 * When the ringing stops by itself, for the notification's countdown. Null while it rings until
	 * stopped, and when it doesn't ring.
	 */
	val ringEndsAt: StateFlow<Instant?> = _ringEndsAt.asStateFlow()

	/** See [PhoneRinger.isSupported]. */
	val isSupported: Boolean
		get() = ringer.isSupported

	/** See [PhoneRinger.canForceMaxVolume]. */
	val canForceMaxVolume: Boolean
		get() = ringer.canForceMaxVolume

	/**
	 * Rings for [duration], or until [stop] if null. Called while ringing, it restarts the timer with
	 * the new [duration] and keeps the sound playing, it doesn't start it again.
	 *
	 * @param forceMaxVolume At the maximum alarm volume, else the one the user set, see
	 *        [PhoneRinger.ring]. Only for a ringing that starts, a restarted one keeps its volume.
	 */
	suspend fun ring(duration: Duration?, forceMaxVolume: Boolean = true) = mutex.withLock {
		timer?.cancel()
		timer = duration?.let { scope.launch { stopAfter(it) } }
		_ringEndsAt.value = duration?.let { clock.now() + it }
		if (sound?.isActive == true) return@withLock
		_ringing.value = true
		sound = scope.launch { play(forceMaxVolume) }
	}

	/** Stops ringing, e.g. from the notification's Stop. Does nothing if it isn't ringing. */
	suspend fun stop() = mutex.withLock {
		timer?.cancel()
		timer = null
		stopSound()
	}

	private suspend fun stopAfter(duration: Duration) {
		delay(duration)
		mutex.withLock {
			// A later ring or stop cancels this timer while holding the mutex. Checked here, as
			// withLock only throws on cancellation when it had to wait, and on Dispatchers.Default a
			// ring on another thread can cancel this timer after its delay ended and release the mutex
			// before this takes it. Without the check, it would stop the new ringing.
			currentCoroutineContext().ensureActive()
			timer = null
			stopSound()
		}
	}

	/** While holding [mutex]. Waits for the ringer to finish, so [ringing] is false on return. */
	private suspend fun stopSound() {
		sound?.cancelAndJoin()
		sound = null
	}

	private suspend fun play(forceMaxVolume: Boolean) {
		try {
			if (ringCustomSound(forceMaxVolume)) return
			ringer.ring(loadSound(), forceMaxVolume)
		} catch (e: CancellationException) {
			throw e
		} catch (e: Exception) {
			// a ringer that fails must not leave the app thinking it rings
			log.e { "Ringing failed: ${e.message}" }
		} finally {
			_ringEndsAt.value = null
			_ringing.value = false
		}
	}

	/** Rings with the user's ringtone, false if there is none or it failed, then the built-in tone rings. */
	private suspend fun ringCustomSound(forceMaxVolume: Boolean): Boolean {
		val sound = try {
			loadCustomSound()
		} catch (e: CancellationException) {
			throw e
		} catch (e: Exception) {
			log.w { "Couldn't load the custom sound, ringing with the built-in one: ${e.message}" }
			null
		} ?: return false
		return try {
			ringer.ring(sound, forceMaxVolume)
			true
		} catch (e: CancellationException) {
			throw e
		} catch (e: Exception) {
			log.w { "Couldn't play the custom sound, ringing with the built-in one: ${e.message}" }
			false
		}
	}
}
