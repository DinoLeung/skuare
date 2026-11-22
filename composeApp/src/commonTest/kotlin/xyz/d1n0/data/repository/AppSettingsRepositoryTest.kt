package xyz.d1n0.data.repository

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import xyz.d1n0.data.database.AppSettings
import xyz.d1n0.data.database.FakeAppSettingsDao
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

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
	fun findPhoneIsOffForAMinuteWithTheBuiltInToneByDefault() = runTest {
		val repository = AppSettingsRepository(FakeAppSettingsDao())

		val settings = repository.findPhoneSettings()

		assertEquals(FindPhoneSettings(), settings)
		assertFalse(settings.enabled)
		assertEquals(60.seconds, settings.ringDuration)
		assertNull(settings.soundName)
		assertEquals(settings, repository.observeFindPhoneSettings().first())
	}

	@Test
	fun eachFindPhoneSettingIsStored() = runTest {
		val dao = FakeAppSettingsDao()
		val repository = AppSettingsRepository(dao)

		repository.setFindPhoneEnabled(true)
		repository.setFindPhoneRingLength(2.minutes + 30.seconds)
		repository.setFindPhoneSoundName("Ringtone.mp3")

		assertEquals(
			FindPhoneSettings(enabled = true, ringLength = 150.seconds, soundName = "Ringtone.mp3"),
			repository.findPhoneSettings(),
		)
		assertEquals(150, dao.get()?.findPhoneRingSeconds)
		// location recording is untouched
		assertFalse(repository.isLocationRecordingEnabled())

		repository.setFindPhoneSoundName(null)
		assertNull(repository.findPhoneSettings().soundName)
	}

	@Test
	fun untilStoppedRingsWithoutALengthAndKeepsTheTypedOne() = runTest {
		val repository = AppSettingsRepository(FakeAppSettingsDao())
		repository.setFindPhoneRingLength(5.minutes)

		repository.setFindPhoneRingUntilStopped(true)
		assertNull(repository.findPhoneSettings().ringDuration)

		repository.setFindPhoneRingUntilStopped(false)
		assertEquals(5.minutes, repository.findPhoneSettings().ringDuration)
	}

	@Test
	fun ringLengthIsStoredInWholeSecondsAndAtLeastOne() = runTest {
		val repository = AppSettingsRepository(FakeAppSettingsDao())

		repository.setFindPhoneRingLength(90.seconds + 999.milliseconds)
		assertEquals(90.seconds, repository.findPhoneSettings().ringLength)

		assertFailsWith<IllegalArgumentException> { repository.setFindPhoneRingLength(Duration.ZERO) }
		assertFailsWith<IllegalArgumentException> { repository.setFindPhoneRingLength(999.milliseconds) }
		assertEquals(90.seconds, repository.findPhoneSettings().ringLength)
	}

	@Test
	fun aStoredLengthThatIsntPositiveRingsForAMinute() = runTest {
		val dao = FakeAppSettingsDao().apply { store(AppSettings(findPhoneEnabled = true, findPhoneRingSeconds = 0)) }

		assertEquals(60.seconds, AppSettingsRepository(dao).findPhoneSettings().ringDuration)
	}

	@Test
	fun fullVolumeIsOnByDefaultAndCanBeTurnedOff() = runTest {
		val repository = AppSettingsRepository(FakeAppSettingsDao())
		assertTrue(repository.findPhoneSettings().maxVolume)

		repository.setFindPhoneMaxVolume(false)

		assertFalse(repository.findPhoneSettings().maxVolume)
		assertFalse(repository.observeFindPhoneSettings().first().maxVolume)
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
