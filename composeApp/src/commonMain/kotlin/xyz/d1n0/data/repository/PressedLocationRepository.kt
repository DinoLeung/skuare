package xyz.d1n0.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import xyz.d1n0.data.database.PressedLocation
import xyz.d1n0.data.database.PressedLocationDao
import xyz.d1n0.data.database.PressedLocationWithWatch
import xyz.d1n0.phone.GeoPoint
import kotlin.time.Instant

class PressedLocationRepository(private val dao: PressedLocationDao) {
	/** Newest first, see [PressedLocationDao.observeAll]. */
	fun observeAll(): Flow<List<PressedLocationWithWatch>> = dao.observeAll()

	/** Number of locations per watch address, watches without any are missing. */
	fun observeCounts(): Flow<Map<String, Int>> =
		dao.observeCounts().map { counts -> counts.associate { it.watchAddress to it.count } }

	suspend fun record(watchAddress: String, watchName: String, pressedAt: Instant, point: GeoPoint) =
		dao.insert(
			PressedLocation(
				watchAddress = watchAddress,
				watchName = watchName,
				pressedAt = pressedAt,
				latitude = point.latitude,
				longitude = point.longitude,
				accuracyMeters = point.accuracyMeters,
			)
		)

	/** Names [location], a blank [name] clears it, so the watch's name shows again. */
	suspend fun rename(location: PressedLocation, name: String) =
		dao.rename(location.id, name.trim().takeIf { it.isNotEmpty() })

	suspend fun deleteFor(watchAddress: String) = dao.deleteFor(watchAddress)

	suspend fun delete(location: PressedLocation) = dao.delete(location.id)

	/** Puts back a [delete]d location, with its id, so the list shows it where it was. */
	suspend fun restore(location: PressedLocation) = dao.insert(location)

	/** Deletes the locations recorded before [before], @return how many. */
	suspend fun deleteBefore(before: Instant): Int = dao.deleteBefore(before)

	suspend fun deleteAll() = dao.deleteAll()
}
