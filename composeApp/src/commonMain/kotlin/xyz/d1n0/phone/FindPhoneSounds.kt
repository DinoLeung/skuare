package xyz.d1n0.phone

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import xyz.d1n0.Log
import xyz.d1n0.data.repository.AppSettingsRepository
import kotlin.coroutines.cancellation.CancellationException

/**
 * The sound find phone rings with: the user's own ringtone, a copy kept by [store], or the built-in
 * tone. The settings hold the ringtone's name, see [xyz.d1n0.data.repository.FindPhoneSettings.soundName].
 *
 * @param loadBuiltIn The built-in tone, tests pass their own.
 */
class FindPhoneSounds(
	private val store: CustomSoundStore,
	private val appSettingsRepository: AppSettingsRepository,
	private val ringer: PhoneRinger,
	private val loadBuiltIn: suspend () -> ByteArray = ::builtInFindPhoneSound,
) {
	private val log = Log

	/** Why a picked file wasn't taken, the previous sound stays then. */
	sealed interface Rejection {
		/** Over [MAX_SIZE_BYTES], the whole file is held in memory while ringing. */
		data object TooLarge : Rejection

		/** The phone's own player can't decode it, e.g. Ogg on iOS. */
		data object Unplayable : Rejection

		/** Reading or storing it failed. */
		data class Failed(val error: Throwable) : Rejection
	}

	/** The user's ringtone, null for the built-in tone, or if its copy is missing or unreadable. */
	suspend fun customSound(): ByteArray? {
		if (appSettingsRepository.findPhoneSettings().soundName == null) return null
		return store.load().also { if (it == null) log.w { "The custom find phone sound is missing" } }
	}

	/** A ringtone is set but its copy is gone, e.g. after restoring a backup, which leaves it out. */
	suspend fun isCustomSoundMissing(): Boolean =
		appSettingsRepository.findPhoneSettings().soundName != null && !store.exists()

	/**
	 * Takes [picked] as the ringtone: checks its size and that the phone can play it, stores a copy,
	 * then its name.
	 *
	 * @return Why it wasn't taken, null if it was.
	 */
	suspend fun choose(picked: PickedAudio): Rejection? {
		if ((picked.sizeBytes ?: 0) > MAX_SIZE_BYTES) return Rejection.TooLarge
		return try {
			// one byte over the limit tells a file that's too large from one exactly at it
			val bytes = picked.read(MAX_SIZE_BYTES + 1)
			when {
				bytes.size > MAX_SIZE_BYTES -> Rejection.TooLarge
				!ringer.canPlay(bytes) -> Rejection.Unplayable
				else -> {
					replace(bytes, picked.displayName)
					null
				}
			}
		} catch (e: CancellationException) {
			throw e
		} catch (e: Exception) {
			log.w { "Couldn't take the picked sound: ${e.message}" }
			Rejection.Failed(e)
		}
	}

	/**
	 * Stores [bytes] as the copy, then [name]. If either fails or is cancelled, the previous copy and
	 * name are put back, so the screen never names a sound other than the one that rings. Cancelling
	 * can land after the save is done, so the copy is restored even when the save itself threw.
	 */
	private suspend fun replace(bytes: ByteArray, name: String) {
		val previousName = appSettingsRepository.findPhoneSettings().soundName
		val previous = previousName?.let { store.load() }
		try {
			store.save(bytes, extensionOf(name))
			appSettingsRepository.setFindPhoneSoundName(name)
		} catch (e: Throwable) {
			withContext(NonCancellable) { restore(previousName, previous) }
			throw e
		}
	}

	/** Without [bytes], e.g. the built-in tone was set, the copy is deleted. */
	private suspend fun restore(name: String?, bytes: ByteArray?) {
		try {
			if (bytes != null) store.save(bytes, extensionOf(name.orEmpty())) else store.delete()
			appSettingsRepository.setFindPhoneSoundName(name)
		} catch (e: Exception) {
			log.e { "Couldn't restore the previous find phone sound: ${e.message}" }
		}
	}

	/** Back to the built-in tone, the copy is deleted. */
	suspend fun reset() {
		appSettingsRepository.setFindPhoneSoundName(null)
		store.delete()
	}

	/** Plays the sound find phone would ring with at the phone's current volume, until cancelled. */
	suspend fun playAtCurrentVolume() {
		ringer.ring(customSound() ?: loadBuiltIn(), forceMaxVolume = false, vibrate = false)
	}

	companion object {
		const val MAX_SIZE_BYTES = 10 * 1024 * 1024

		/** e.g. mp3 for Ringtone.MP3, a hint for the player, empty without one. */
		internal fun extensionOf(fileName: String): String =
			fileName.substringAfterLast('.', "").lowercase()
				.takeIf { ext -> ext.length in 1..5 && ext.all { it in 'a'..'z' || it in '0'..'9' } }
				?: ""
	}
}
