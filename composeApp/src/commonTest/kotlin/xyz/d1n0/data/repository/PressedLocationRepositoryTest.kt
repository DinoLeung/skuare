package xyz.d1n0.data.repository

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import xyz.d1n0.data.database.FakePairedWatchDao
import xyz.d1n0.data.database.FakePressedLocationDao
import xyz.d1n0.data.database.PairedWatch
import xyz.d1n0.phone.GeoPoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class PressedLocationRepositoryTest {
	private companion object {
		const val PAIRED = "ED:A1:AF:D5:A2:40"
		const val FORGOTTEN = "00:11:22:33:44:55"
		val EARLIER = Instant.parse("2026-11-20T08:00:00Z")
		val LATER = Instant.parse("2026-11-21T08:00:00Z")
	}

	private suspend fun repositoryWithTwoWatches(): PressedLocationRepository {
		val watches = FakePairedWatchDao()
		watches.upsert(
			PairedWatch(
				address = PAIRED,
				advertisedName = "CASIO GW-B5600",
				displayName = "Renamed square",
				pairedAt = EARLIER,
			)
		)
		return PressedLocationRepository(FakePressedLocationDao(watches)).apply {
			record(PAIRED, watchName = "Old name", pressedAt = EARLIER, point = GeoPoint(1.0, 2.0))
			record(FORGOTTEN, watchName = "CASIO GA-B2100", pressedAt = LATER, point = GeoPoint(3.0, 4.0))
		}
	}

	@Test
	fun listsNewestFirstWithCurrentOrStoredWatchName() = runTest {
		val locations = repositoryWithTwoWatches().observeAll().first()

		assertEquals(listOf(LATER, EARLIER), locations.map { it.location.pressedAt })
		// a forgotten watch keeps the name it had at the press, a paired one shows its current name
		assertEquals(listOf("CASIO GA-B2100", "Renamed square"), locations.map { it.watchName })
	}

	@Test
	fun countsLocationsPerWatch() = runTest {
		val repository = repositoryWithTwoWatches()
		repository.record(PAIRED, watchName = "Renamed square", pressedAt = LATER, point = GeoPoint(5.0, 6.0))

		assertEquals(mapOf(PAIRED to 2, FORGOTTEN to 1), repository.observeCounts().first())
	}
}
