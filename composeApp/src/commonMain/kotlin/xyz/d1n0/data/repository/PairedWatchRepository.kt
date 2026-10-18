package xyz.d1n0.data.repository

import kotlinx.coroutines.flow.Flow
import xyz.d1n0.data.database.PairedWatch
import xyz.d1n0.data.database.PairedWatchDao
import kotlin.time.Clock
import kotlin.time.Instant

class PairedWatchRepository(private val dao: PairedWatchDao) {
	fun observeAll(): Flow<List<PairedWatch>> = dao.observeAll()

	suspend fun find(address: String): PairedWatch? = dao.find(address)

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

	suspend fun remove(address: String) = dao.delete(address)
}
