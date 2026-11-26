package xyz.d1n0.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

@Dao
interface PressedLocationDao {
	/** A new location gets its id from the database, a deleted one put back keeps its own. */
	@Insert
	suspend fun insert(location: PressedLocation)

	/** Newest first, with the current name of each location's watch while it is paired. */
	@Query(
		"""
		SELECT pressed_location.*, paired_watch.displayName AS pairedDisplayName
		FROM pressed_location
		LEFT JOIN paired_watch ON paired_watch.address = pressed_location.watchAddress
		ORDER BY pressedAt DESC, id DESC
		"""
	)
	fun observeAll(): Flow<List<PressedLocationWithWatch>>

	/** Watches with at least one location, for the forget dialog. */
	@Query("SELECT watchAddress, COUNT(*) AS count FROM pressed_location GROUP BY watchAddress")
	fun observeCounts(): Flow<List<PressedLocationCount>>

	/** @param name Null goes back to the watch's name. */
	@Query("UPDATE pressed_location SET name = :name WHERE id = :id")
	suspend fun rename(id: Long, name: String?)

	@Query("DELETE FROM pressed_location WHERE watchAddress = :address")
	suspend fun deleteFor(address: String)

	@Query("DELETE FROM pressed_location WHERE id = :id")
	suspend fun delete(id: Long)

	/** @return How many were deleted. */
	@Query("DELETE FROM pressed_location WHERE pressedAt < :before")
	suspend fun deleteBefore(before: Instant): Int

	@Query("DELETE FROM pressed_location")
	suspend fun deleteAll()
}
