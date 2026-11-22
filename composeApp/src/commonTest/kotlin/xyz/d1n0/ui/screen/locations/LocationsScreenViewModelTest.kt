package xyz.d1n0.ui.screen.locations

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import xyz.d1n0.data.database.FakeAppSettingsDao
import xyz.d1n0.data.database.FakePairedWatchDao
import xyz.d1n0.data.database.FakePressedLocationDao
import xyz.d1n0.data.repository.AppSettingsRepository
import xyz.d1n0.data.repository.PressedLocationRepository
import xyz.d1n0.phone.GeoPoint
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class LocationsScreenViewModelTest {
	private companion object {
		const val ADDRESS = "ED:A1:AF:D5:A2:40"
		val NOW = Instant.parse("2026-11-22T12:00:00Z")
	}

	private val dao = FakePressedLocationDao(FakePairedWatchDao())
	private val repository = PressedLocationRepository(dao)

	@BeforeTest
	fun setUp() {
		Dispatchers.setMain(UnconfinedTestDispatcher())
	}

	@AfterTest
	fun tearDown() {
		Dispatchers.resetMain()
	}

	/** The view model, with its state collected like the screen does, so it holds the locations. */
	private fun TestScope.viewModel(): LocationsScreenViewModel =
		LocationsScreenViewModel(
			pressedLocationRepository = repository,
			appSettingsRepository = AppSettingsRepository(FakeAppSettingsDao()),
			clock = object : Clock {
				override fun now() = NOW
			},
			timeZone = { TimeZone.UTC },
		).also { viewModel -> backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} } }

	private suspend fun recordAt(isoTime: String) =
		repository.record(ADDRESS, watchName = "Watch", pressedAt = Instant.parse(isoTime), point = GeoPoint(1.0, 2.0))

	private val storedTimes get() = dao.all.map { it.pressedAt.toString() }.sorted()

	@Test
	fun deletingOneAndUndoingPutsItBack() = runTest {
		recordAt("2026-11-21T08:00:00Z")
		recordAt("2026-11-20T08:00:00Z")
		val viewModel = viewModel()
		val deleted = viewModel.state.value.locations!!.first().location

		viewModel.delete(deleted)
		assertEquals(listOf("2026-11-20T08:00:00Z"), storedTimes)

		viewModel.restore(deleted)
		// back in its place, newest first
		assertEquals(listOf(deleted), viewModel.state.value.locations!!.take(1).map { it.location })
		assertEquals(2, dao.all.size)
	}

	@Test
	fun countsWhatEachAgeWouldDelete() = runTest {
		recordAt("2026-11-21T12:00:00Z") // a day old
		recordAt("2026-10-01T12:00:00Z") // over a month
		recordAt("2026-04-01T12:00:00Z") // over 6 months
		recordAt("2024-11-22T12:00:00Z") // over a year
		val viewModel = viewModel()

		assertEquals(
			mapOf(
				LocationAge.WEEK to 3,
				LocationAge.MONTH to 3,
				LocationAge.THREE_MONTHS to 2,
				LocationAge.SIX_MONTHS to 2,
				LocationAge.YEAR to 1,
			),
			viewModel.countsOlderThan(),
		)
	}

	@Test
	fun deletesOnlyOlderThanTheAge() = runTest {
		recordAt("2026-11-21T12:00:00Z")
		recordAt("2026-10-22T12:00:01Z") // just under a month old, kept
		recordAt("2026-10-22T11:59:59Z") // just over, deleted
		val viewModel = viewModel()

		viewModel.deleteOlderThan(LocationAge.MONTH)

		assertEquals(listOf("2026-10-22T12:00:01Z", "2026-11-21T12:00:00Z"), storedTimes)
	}

	@Test
	fun deletesAll() = runTest {
		recordAt("2026-11-21T12:00:00Z")
		recordAt("2020-01-01T00:00:00Z")
		val viewModel = viewModel()

		viewModel.deleteAll()

		assertEquals(emptyList(), viewModel.state.value.locations)
	}
}
