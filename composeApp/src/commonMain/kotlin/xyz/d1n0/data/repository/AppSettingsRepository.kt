package xyz.d1n0.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import xyz.d1n0.data.database.AppSettings
import xyz.d1n0.data.database.AppSettingsDao
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * The settings on the App settings screen, see [AppSettings]. A missing row reads as the defaults.
 */
class AppSettingsRepository(private val dao: AppSettingsDao) {
	private val defaults = AppSettings()

	/** See [AppSettings.locationRecordingEnabled]. */
	fun observeLocationRecordingEnabled(): Flow<Boolean> =
		dao.observe().map { (it ?: defaults).locationRecordingEnabled }

	suspend fun isLocationRecordingEnabled(): Boolean = (dao.get() ?: defaults).locationRecordingEnabled

	suspend fun setLocationRecordingEnabled(enabled: Boolean) = update { dao.setLocationRecordingEnabled(enabled) }

	fun observeFindPhoneSettings(): Flow<FindPhoneSettings> =
		dao.observe().map { FindPhoneSettings.from(it ?: defaults) }

	/** Read at each long press, so a change takes effect at the next one. */
	suspend fun findPhoneSettings(): FindPhoneSettings = FindPhoneSettings.from(dao.get() ?: defaults)

	suspend fun setFindPhoneEnabled(enabled: Boolean) = update { dao.setFindPhoneEnabled(enabled) }

	suspend fun setFindPhoneRingUntilStopped(untilStopped: Boolean) =
		update { dao.setFindPhoneRingUntilStopped(untilStopped) }

	/**
	 * Stores [length] in whole seconds, the screen only offers those.
	 *
	 * @throws IllegalArgumentException If [length] is under a second, the screen shows that as an
	 *         error and doesn't save it.
	 */
	suspend fun setFindPhoneRingLength(length: Duration) {
		require(length >= 1.seconds) { "Ring length must be at least a second, was $length" }
		update { dao.setFindPhoneRingSeconds(length.inWholeSeconds.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()) }
	}

	/** Null for the built-in tone, see [FindPhoneSettings.soundName]. */
	suspend fun setFindPhoneSoundName(name: String?) = update { dao.setFindPhoneSoundName(name) }

	/** Creates the row with every default first, the setters only update an existing one. */
	private suspend fun update(set: suspend () -> Unit) {
		dao.insertIfMissing(defaults)
		set()
	}
}
