package xyz.d1n0.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PressedLocationDao {
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

	@Query("DELETE FROM pressed_location WHERE watchAddress = :address")
	suspend fun deleteFor(address: String)
}
