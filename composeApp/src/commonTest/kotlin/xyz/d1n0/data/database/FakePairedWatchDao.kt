package xyz.d1n0.data.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import xyz.d1n0.lib.model.CustomWatchModel
import kotlin.time.Instant

/**
 * In-memory [PairedWatchDao] that follows the same rules as the Room queries.
 */
internal class FakePairedWatchDao : PairedWatchDao {
	private val watches = MutableStateFlow<List<PairedWatch>>(emptyList())

	override fun observeAll(): Flow<List<PairedWatch>> = watches

	override suspend fun find(address: String): PairedWatch? =
		watches.value.firstOrNull { it.address == address }

	override fun observe(address: String): Flow<PairedWatch?> =
		watches.map { current -> current.firstOrNull { it.address == address } }

	override suspend fun upsert(watch: PairedWatch) {
		watches.update { current -> current.filterNot { it.address == watch.address } + watch }
	}

	override suspend fun updateLastSync(address: String, at: Instant, error: String?) =
		updateWhere(address) { it.copy(lastSyncAt = at, lastSyncError = error) }

	override suspend fun updateDisplayName(address: String, displayName: String) =
		updateWhere(address) { it.copy(displayName = displayName) }

	override suspend fun updateCustomModel(
		address: String,
		clockCount: Int,
		reminderCount: Int,
		hasFontStyle: Boolean,
		hasShortCityNames: Boolean,
		hasWeekdayLanguage: Boolean,
	): Int {
		// like the query, only a watch that already has a custom model is updated
		val target = find(address)?.takeIf { it.customModel != null } ?: return 0
		upsert(
			target.copy(
				customModel = CustomWatchModel(
					clockCount = clockCount,
					reminderCount = reminderCount,
					hasFontStyle = hasFontStyle,
					hasShortCityNames = hasShortCityNames,
					hasWeekdayLanguage = hasWeekdayLanguage,
				)
			)
		)
		return 1
	}

	override suspend fun markCustomModelIntroShown(address: String) =
		updateWhere(address) { it.copy(customModelIntroShown = true) }

	override suspend fun delete(address: String) {
		watches.update { current -> current.filterNot { it.address == address } }
	}

	private fun updateWhere(address: String, transform: (PairedWatch) -> PairedWatch) {
		watches.update { current -> current.map { if (it.address == address) transform(it) else it } }
	}
}
