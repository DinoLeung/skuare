package xyz.d1n0.data.repository

import xyz.d1n0.data.database.AppSettings
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Find phone as the user set it on the App settings screen, see [AppSettings].
 *
 * @property ringLength The length the user typed, kept while [ringUntilStopped] is on, so turning
 *           that off brings it back.
 * @property soundName The user's own ringtone, null for the built-in tone.
 */
data class FindPhoneSettings(
	val enabled: Boolean = false,
	val ringUntilStopped: Boolean = false,
	val ringLength: Duration = DEFAULT_RING_LENGTH,
	val soundName: String? = null,
) {
	/** How long a long press rings, null until the user stops it. */
	val ringDuration: Duration?
		get() = if (ringUntilStopped) null else ringLength

	companion object {
		val DEFAULT_RING_LENGTH = AppSettings.DEFAULT_RING_SECONDS.seconds

		/** A stored length that isn't positive, which the screen doesn't allow, rings for a minute. */
		fun from(settings: AppSettings) = FindPhoneSettings(
			enabled = settings.findPhoneEnabled,
			ringUntilStopped = settings.findPhoneRingUntilStopped,
			ringLength = settings.findPhoneRingSeconds.takeIf { it > 0 }?.seconds ?: DEFAULT_RING_LENGTH,
			soundName = settings.findPhoneSoundName,
		)
	}
}
