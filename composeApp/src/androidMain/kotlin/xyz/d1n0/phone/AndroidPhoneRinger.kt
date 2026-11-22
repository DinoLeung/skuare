package xyz.d1n0.phone

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaDataSource
import android.media.MediaPlayer
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.VibratorManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import xyz.d1n0.Log
import kotlin.coroutines.resumeWithException

/**
 * Rings on the alarm stream, so it plays in silent and vibrate mode, and in Do Not Disturb unless
 * the user blocked alarms there. With forceMaxVolume, the alarm volume goes to its maximum while
 * ringing and back to what it was afterwards, else it rings at the alarm volume the user set. Other
 * playback, e.g. music, pauses meanwhile, and with vibrate the phone vibrates.
 *
 * The notification with Stop is [xyz.d1n0.sync.WatchSyncService]'s, shown while
 * [PhoneFinder.ringing].
 */
class AndroidPhoneRinger(context: Context) : PhoneRinger {
	private val context = context.applicationContext
	private val log = Log

	override val isSupported = true
	override val canForceMaxVolume = true

	override suspend fun ring(sound: ByteArray, forceMaxVolume: Boolean, vibrate: Boolean) {
		val audio = context.getSystemService(AudioManager::class.java)
		val vibrator = context.getSystemService(VibratorManager::class.java).defaultVibrator
		val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
			.setAudioAttributes(ALARM)
			.build()
		val previousVolume = audio.getStreamVolume(AudioManager.STREAM_ALARM)
		val player = MediaPlayer()
		try {
			player.setAudioAttributes(ALARM)
			player.setDataSource(ByteArraySource(sound))
			player.isLooping = true
			withContext(Dispatchers.IO) { player.prepare() }

			audio.requestAudioFocus(focus)
			if (forceMaxVolume) setAlarmVolume(audio, audio.getStreamMaxVolume(AudioManager.STREAM_ALARM))
			playUntilCancelledOrFailed(player) {
				if (vibrate) {
					vibrator.vibrate(
						VibrationEffect.createWaveform(VIBRATION_PATTERN, 0),
						VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM),
					)
				}
			}
		} finally {
			vibrator.cancel()
			// stop throws if prepare failed before start, nothing to stop then
			runCatching { player.stop() }
			player.release()
			audio.abandonAudioFocusRequest(focus)
			if (forceMaxVolume) setAlarmVolume(audio, previousVolume)
		}
	}

	/**
	 * Starts [player], then [onStarted], and suspends until cancelled. A playback error, e.g. a
	 * ringtone that prepared but is damaged further in, ends it with an exception instead of leaving
	 * the phone silent while it looks like ringing: [PhoneFinder] falls back to the built-in tone.
	 */
	private suspend fun playUntilCancelledOrFailed(player: MediaPlayer, onStarted: () -> Unit) {
		suspendCancellableCoroutine<Unit> { continuation ->
			player.setOnErrorListener { _, what, extra ->
				if (continuation.isActive) {
					continuation.resumeWithException(IllegalStateException("Playback failed, MediaPlayer error $what/$extra"))
				}
				// handled, no completion callback follows
				true
			}
			player.start()
			onStarted()
		}
	}

	/** Whether [MediaPlayer] gets as far as prepared with [sound], it reads the format then. */
	override suspend fun canPlay(sound: ByteArray): Boolean = withContext(Dispatchers.IO) {
		val player = MediaPlayer()
		try {
			player.setAudioAttributes(ALARM)
			player.setDataSource(ByteArraySource(sound))
			player.prepare()
			true
		} catch (e: Exception) {
			log.i { "Can't play the picked sound: ${e.message}" }
			false
		} finally {
			player.release()
		}
	}

	/** A failed volume change, e.g. blocked by Do Not Disturb, mustn't stop the ringing. */
	private fun setAlarmVolume(audio: AudioManager, volume: Int) {
		runCatching { audio.setStreamVolume(AudioManager.STREAM_ALARM, volume, 0) }
			.onFailure { log.w { "Couldn't set the alarm volume: ${it.message}" } }
	}

	/** A whole audio file in memory, for [MediaPlayer.setDataSource]. */
	private class ByteArraySource(private val data: ByteArray) : MediaDataSource() {
		override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
			if (position >= data.size) return -1
			val count = minOf(size.toLong(), data.size - position).toInt()
			data.copyInto(buffer, offset, position.toInt(), position.toInt() + count)
			return count
		}

		override fun getSize() = data.size.toLong()

		override fun close() = Unit
	}

	private companion object {
		val ALARM: AudioAttributes = AudioAttributes.Builder()
			.setUsage(AudioAttributes.USAGE_ALARM)
			.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
			.build()

		/** Off, on, off in ms, repeated from the start. */
		val VIBRATION_PATTERN = longArrayOf(0, 800, 600)
	}
}
