package xyz.d1n0.data.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * In-memory [AppSettingsDao] that follows the same rules as the Room queries: no row until one is
 * inserted, and an update before that does nothing.
 */
internal class FakeAppSettingsDao : AppSettingsDao {
	private val settings = MutableStateFlow<AppSettings?>(null)

	override suspend fun get(): AppSettings? = settings.value

	override fun observe(): Flow<AppSettings?> = settings

	override suspend fun insertIfMissing(settings: AppSettings) {
		this.settings.compareAndSet(null, settings)
	}

	override suspend fun setLocationRecordingEnabled(enabled: Boolean) {
		settings.update { it?.copy(locationRecordingEnabled = enabled) }
	}

	override suspend fun setFindPhoneEnabled(enabled: Boolean) {
		settings.update { it?.copy(findPhoneEnabled = enabled) }
	}

	override suspend fun setFindPhoneRingUntilStopped(untilStopped: Boolean) {
		settings.update { it?.copy(findPhoneRingUntilStopped = untilStopped) }
	}

	override suspend fun setFindPhoneRingSeconds(seconds: Int) {
		settings.update { it?.copy(findPhoneRingSeconds = seconds) }
	}

	override suspend fun setFindPhoneSoundName(name: String?) {
		settings.update { it?.copy(findPhoneSoundName = name) }
	}

	/** Stores [settings] as they are, e.g. a value the screen wouldn't allow. */
	fun store(settings: AppSettings) {
		this.settings.value = settings
	}
}
