package xyz.d1n0.data.repository

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import xyz.d1n0.data.database.AppSettings
import xyz.d1n0.data.database.FakeAppSettingsDao
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppSettingsRepositoryTest {
	@Test
	fun noRowReadsAsTheDefaults() = runTest {
		val dao = FakeAppSettingsDao()
		val repository = AppSettingsRepository(dao)

		// a fresh install, or one upgraded from before app_settings existed
		assertFalse(repository.isLocationRecordingEnabled())
		assertFalse(repository.observeLocationRecordingEnabled().first())
		assertNull(dao.get())
	}

	@Test
	fun firstChangeCreatesTheRow() = runTest {
		val dao = FakeAppSettingsDao()
		val repository = AppSettingsRepository(dao)

		repository.setLocationRecordingEnabled(true)

		assertTrue(repository.isLocationRecordingEnabled())
		assertTrue(repository.observeLocationRecordingEnabled().first())
		assertEquals(AppSettings(locationRecordingEnabled = true), dao.get())
	}

	@Test
	fun turningItOffAgainKeepsTheRow() = runTest {
		val dao = FakeAppSettingsDao()
		val repository = AppSettingsRepository(dao)

		repository.setLocationRecordingEnabled(true)
		repository.setLocationRecordingEnabled(false)

		assertFalse(repository.isLocationRecordingEnabled())
		assertEquals(AppSettings(locationRecordingEnabled = false), dao.get())
	}
}
