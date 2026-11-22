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

	suspend fun deleteFor(watchAddress: String) = dao.deleteFor(watchAddress)
}
