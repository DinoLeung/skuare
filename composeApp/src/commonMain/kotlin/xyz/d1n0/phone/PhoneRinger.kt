package xyz.d1n0.phone

/**
 * Plays the find phone sound, implemented per platform. [PhoneFinder] decides when it starts and
 * stops, see research/button-actions-plan.md.
 */
interface PhoneRinger {
	/** False where the phone can't ring from a background session, the feature is hidden then. */
	val isSupported: Boolean

	/**
	 * Whether [ring] can play at the loudest level whatever the phone's volume. False on iOS, which
	 * has no public API to raise the system volume, the sound follows it there.
	 */
	val canForceMaxVolume: Boolean

	/**
	 * Plays [sound], a whole audio file, in a loop at the loudest level the platform allows, until
	 * the calling coroutine is cancelled. Restores anything it changed, e.g. the alarm volume, on the
	 * way out. Cancellation is the only way to stop it, there is no stop() to keep in step with it on
	 * each platform. The alarm-style notification with Stop is the platform's, shown while
	 * [PhoneFinder.ringing], e.g. Android's foreground service.
	 *
	 * @param forceMaxVolume False for App settings' Play sound: the phone's current volume, no
	 *        vibration.
	 * @throws Exception If [sound] can't be played, [PhoneFinder] falls back to the built-in tone.
	 */
	suspend fun ring(sound: ByteArray, forceMaxVolume: Boolean = true)

	/** Whether this phone's player decodes [sound], checked before a picked ringtone is stored. */
	suspend fun canPlay(sound: ByteArray): Boolean
}

/** For platforms that can't ring yet, find phone stays hidden. */
class UnsupportedPhoneRinger : PhoneRinger {
	override val isSupported = false
	override val canForceMaxVolume = false
	override suspend fun ring(sound: ByteArray, forceMaxVolume: Boolean) = Unit
	override suspend fun canPlay(sound: ByteArray) = false
}
