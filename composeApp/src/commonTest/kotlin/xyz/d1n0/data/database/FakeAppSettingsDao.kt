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
}
