package xyz.d1n0.data.repository

import kotlinx.coroutines.flow.Flow
import xyz.d1n0.data.database.PairedWatch
import xyz.d1n0.data.database.PairedWatchDao
import xyz.d1n0.lib.model.CustomWatchModel
import kotlin.time.Clock
import kotlin.time.Instant

class PairedWatchRepository(private val dao: PairedWatchDao) {
	fun observeAll(): Flow<List<PairedWatch>> = dao.observeAll()

	suspend fun find(address: String): PairedWatch? = dao.find(address)

	fun observe(address: String): Flow<PairedWatch?> = dao.observe(address)

	suspend fun upsert(watch: PairedWatch) = dao.upsert(watch)

	/**
	 * Stores the result of a background sync.
	 *
	 * @param address BLE address of the watch.
	 * @param error Why the sync failed, null if it succeeded.
	 * @param at When the sync finished.
	 */
	suspend fun recordSync(address: String, error: String? = null, at: Instant = Clock.System.now()) =
		dao.updateLastSync(address = address, at = at, error = error)

	suspend fun updateDisplayName(address: String, displayName: String) =
		dao.updateDisplayName(address = address, displayName = displayName)

	/**
	 * Stores [model] for a watch paired as custom, see [PairedWatchDao.updateCustomModel].
	 *
	 * @return false if the watch isn't paired or isn't custom, nothing was stored then.
	 */
	suspend fun updateCustomModel(address: String, model: CustomWatchModel): Boolean =
		dao.updateCustomModel(
			address = address,
			clockCount = model.clockCount,
			reminderCount = model.reminderCount,
			hasFontStyle = model.hasFontStyle,
			hasShortCityNames = model.hasShortCityNames,
			hasWeekdayLanguage = model.hasWeekdayLanguage,
		) > 0

	suspend fun markCustomModelIntroShown(address: String) = dao.markCustomModelIntroShown(address)

	/** See [PairedWatchDao.clearCustomModel]. */
	suspend fun clearCustomModel(address: String) = dao.clearCustomModel(address)

	suspend fun remove(address: String) = dao.delete(address)
}
