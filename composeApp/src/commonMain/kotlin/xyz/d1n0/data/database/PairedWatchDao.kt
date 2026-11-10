package xyz.d1n0.data.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

@Dao
interface PairedWatchDao {
	@Query("SELECT * FROM paired_watch ORDER BY pairedAt")
	fun observeAll(): Flow<List<PairedWatch>>

	@Query("SELECT * FROM paired_watch WHERE address = :address")
	suspend fun find(address: String): PairedWatch?

	@Query("SELECT * FROM paired_watch WHERE address = :address")
	fun observe(address: String): Flow<PairedWatch?>

	@Upsert
	suspend fun upsert(watch: PairedWatch)

	@Query("UPDATE paired_watch SET lastSyncAt = :at, lastSyncError = :error WHERE address = :address")
	suspend fun updateLastSync(address: String, at: Instant, error: String?)

	@Query("UPDATE paired_watch SET displayName = :displayName WHERE address = :address")
	suspend fun updateDisplayName(address: String, displayName: String)

	/**
	 * Updates the custom model of a watch paired as custom, a watch without one is left alone, so
	 * a known [xyz.d1n0.lib.model.WatchModel] can't get one here. The first custom model is stored
	 * with the whole row when the watch is paired.
	 *
	 * @return Rows updated, 0 if the watch isn't paired or isn't custom.
	 */
	@Query(
		"""
		UPDATE paired_watch SET
			model_clockCount = :clockCount, model_reminderCount = :reminderCount,
			model_hasFontStyle = :hasFontStyle, model_hasShortCityNames = :hasShortCityNames,
			model_hasWeekdayLanguage = :hasWeekdayLanguage
		WHERE address = :address AND model_clockCount IS NOT NULL
		"""
	)
	suspend fun updateCustomModel(
		address: String,
		clockCount: Int,
		reminderCount: Int,
		hasFontStyle: Boolean,
		hasShortCityNames: Boolean,
		hasWeekdayLanguage: Boolean,
	): Int

	@Query("UPDATE paired_watch SET customModelIntroShown = 1 WHERE address = :address")
	suspend fun markCustomModelIntroShown(address: String)

	@Query("DELETE FROM paired_watch WHERE address = :address")
	suspend fun delete(address: String)
}
