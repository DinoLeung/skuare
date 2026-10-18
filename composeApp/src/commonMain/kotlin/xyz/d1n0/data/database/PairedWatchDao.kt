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

	@Upsert
	suspend fun upsert(watch: PairedWatch)

	@Query("UPDATE paired_watch SET lastSyncAt = :at, lastSyncError = :error WHERE address = :address")
	suspend fun updateLastSync(address: String, at: Instant, error: String?)

	@Query("UPDATE paired_watch SET displayName = :displayName WHERE address = :address")
	suspend fun updateDisplayName(address: String, displayName: String)

	@Query("DELETE FROM paired_watch WHERE address = :address")
	suspend fun delete(address: String)
}
