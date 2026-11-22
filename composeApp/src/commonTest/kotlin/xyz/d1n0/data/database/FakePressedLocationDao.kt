package xyz.d1n0.data.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * In-memory [PressedLocationDao] that follows the same rules as the Room queries, joining the
 * watch names from [pairedWatchDao].
 */
internal class FakePressedLocationDao(private val pairedWatchDao: PairedWatchDao) : PressedLocationDao {
	private val locations = MutableStateFlow<List<PressedLocation>>(emptyList())
	private var nextId = 1L

	val all: List<PressedLocation> get() = locations.value

	override suspend fun insert(location: PressedLocation) {
		val stored = location.copy(id = nextId++)
		locations.update { it + stored }
	}

	override fun observeAll(): Flow<List<PressedLocationWithWatch>> =
		combine(locations, pairedWatchDao.observeAll()) { stored, watches ->
			stored
				.sortedWith(compareByDescending<PressedLocation> { it.pressedAt }.thenByDescending { it.id })
				.map { location ->
					PressedLocationWithWatch(
						location = location,
						pairedDisplayName = watches.firstOrNull { it.address == location.watchAddress }?.displayName,
					)
				}
		}

	override fun observeCounts(): Flow<List<PressedLocationCount>> =
		locations.map { stored ->
			stored.groupingBy { it.watchAddress }.eachCount().map { (address, count) -> PressedLocationCount(address, count) }
		}

	override suspend fun deleteFor(address: String) {
		locations.update { stored -> stored.filterNot { it.watchAddress == address } }
	}
}
