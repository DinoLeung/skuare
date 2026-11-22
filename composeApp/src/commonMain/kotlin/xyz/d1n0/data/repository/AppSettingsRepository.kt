package xyz.d1n0.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import xyz.d1n0.data.database.AppSettings
import xyz.d1n0.data.database.AppSettingsDao

/**
 * The settings on the App settings screen, see [AppSettings]. A missing row reads as the defaults.
 */
class AppSettingsRepository(private val dao: AppSettingsDao) {
	private val defaults = AppSettings()

	/** See [AppSettings.locationRecordingEnabled]. */
	fun observeLocationRecordingEnabled(): Flow<Boolean> =
		dao.observe().map { (it ?: defaults).locationRecordingEnabled }

	suspend fun isLocationRecordingEnabled(): Boolean = (dao.get() ?: defaults).locationRecordingEnabled

	suspend fun setLocationRecordingEnabled(enabled: Boolean) {
		dao.insertIfMissing(defaults)
		dao.setLocationRecordingEnabled(enabled)
	}
}
