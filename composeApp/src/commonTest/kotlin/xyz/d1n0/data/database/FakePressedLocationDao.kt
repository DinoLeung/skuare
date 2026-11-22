package xyz.d1n0.data.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlin.time.Instant

/**
 * In-memory [PressedLocationDao] that follows the same rules as the Room queries, joining the
 * watch names from [pairedWatchDao].
 */
internal class FakePressedLocationDao(private val pairedWatchDao: PairedWatchDao) : PressedLocationDao {
	private val locations = MutableStateFlow<List<PressedLocation>>(emptyList())
	private var nextId = 1L

	val all: List<PressedLocation> get() = locations.value

	override suspend fun insert(location: PressedLocation) {
		// like autoGenerate, 0 means "give it an id", any other id is kept, e.g. an undone delete
		val stored = if (location.id == 0L) location.copy(id = nextId++) else location
		nextId = maxOf(nextId, stored.id + 1)
		locations.update { current ->
			check(current.none { it.id == stored.id }) { "id ${stored.id} exists" }
			current + stored
		}
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

	override suspend fun delete(id: Long) {
		locations.update { stored -> stored.filterNot { it.id == id } }
	}

	override suspend fun deleteBefore(before: Instant): Int {
		val count = locations.value.count { it.pressedAt < before }
		locations.update { stored -> stored.filterNot { it.pressedAt < before } }
		return count
	}

	override suspend fun deleteAll() {
		locations.value = emptyList()
	}
}
