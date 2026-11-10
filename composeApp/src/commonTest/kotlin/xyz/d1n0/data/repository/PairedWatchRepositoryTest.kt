package xyz.d1n0.data.repository

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import xyz.d1n0.data.database.FakePairedWatchDao
import xyz.d1n0.data.database.PairedWatch
import xyz.d1n0.lib.model.CustomWatchModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class PairedWatchRepositoryTest {
	private companion object {
		const val ADDRESS = "ED:A1:AF:D5:A2:40"
	}

	private val repository = PairedWatchRepository(FakePairedWatchDao())

	private fun pairedWatch(customModel: CustomWatchModel?) = PairedWatch(
		address = ADDRESS,
		advertisedName = "CASIO GA-B2100",
		displayName = "My watch",
		pairedAt = Instant.parse("2026-01-01T00:00:00Z"),
		customModel = customModel,
	)

	@Test
	fun updatesCustomModelOfCustomWatch() = runTest {
		repository.upsert(pairedWatch(customModel = CustomWatchModel()))
		val adjusted = CustomWatchModel(clockCount = 4, reminderCount = 5, hasWeekdayLanguage = true)

		assertTrue(repository.updateCustomModel(ADDRESS, adjusted))
		assertEquals(adjusted, repository.find(ADDRESS)?.customModel)
	}

	@Test
	fun doesNotGiveKnownWatchCustomModel() = runTest {
		repository.upsert(pairedWatch(customModel = null))

		assertFalse(repository.updateCustomModel(ADDRESS, CustomWatchModel()))
		assertNull(repository.find(ADDRESS)?.customModel)
	}

	@Test
	fun doesNotUpdateUnpairedWatch() = runTest {
		assertFalse(repository.updateCustomModel(ADDRESS, CustomWatchModel()))
		assertNull(repository.find(ADDRESS))
	}

	@Test
	fun marksCustomModelIntroShown() = runTest {
		repository.upsert(pairedWatch(customModel = CustomWatchModel()))
		assertFalse(repository.observe(ADDRESS).first()!!.customModelIntroShown)

		repository.markCustomModelIntroShown(ADDRESS)

		assertTrue(repository.observe(ADDRESS).first()!!.customModelIntroShown)
	}
}
