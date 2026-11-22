package xyz.d1n0.ui.screen.appsettings

import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.location.BACKGROUND_LOCATION
import dev.icerock.moko.permissions.location.LOCATION
import dev.icerock.moko.permissions.notifications.REMOTE_NOTIFICATION
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import xyz.d1n0.data.database.FakeAppSettingsDao
import xyz.d1n0.data.database.PairedWatch
import xyz.d1n0.data.repository.AppSettingsRepository
import xyz.d1n0.phone.FakeCustomSoundStore
import xyz.d1n0.phone.FakePhoneRinger
import xyz.d1n0.phone.FindPhoneSounds
import xyz.d1n0.phone.GeoPoint
import xyz.d1n0.phone.LocationProvider
import xyz.d1n0.phone.LocationResult
import xyz.d1n0.phone.PhoneFinder
import xyz.d1n0.phone.PickedAudio
import xyz.d1n0.sync.WatchPresenceMonitor
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class) // setMain, UnconfinedTestDispatcher, runCurrent
class AppSettingsScreenViewModelTest {
	private companion object {
		val LOCATION_PERMISSIONS = listOf(Permission.LOCATION, Permission.BACKGROUND_LOCATION)
	}

	/** The system's side: what's granted, and how the user answers a prompt. */
	private class FakePermissions : PermissionRequester {
		val granted = mutableSetOf<Permission>()
		var userAllows = true
		val requested = mutableListOf<Permission>()

		override suspend fun isGranted(permission: Permission) = permission in granted

		override suspend fun request(permission: Permission) {
			requested += permission
			if (userAllows) granted += permission else throw IllegalStateException("$permission denied")
		}

		override fun openAppSettings() = Unit
	}

	private class FakeLocation(private val permissions: FakePermissions) : LocationProvider {
		override val isSupported = true
		override val requiredPermissions = LOCATION_PERMISSIONS
		override fun isPermissionGranted() = permissions.granted.containsAll(LOCATION_PERMISSIONS)
		override suspend fun currentLocation() = LocationResult.TimedOut
		override suspend fun lastKnownLocation(maxAge: Duration): GeoPoint? = null
	}

	private class FakePresence(override val isSupported: Boolean) : WatchPresenceMonitor {
		override suspend fun register(watch: PairedWatch) = watch
		override suspend fun unregister(watch: PairedWatch) = Unit
	}

	private val permissions = FakePermissions()
	private val repository = AppSettingsRepository(FakeAppSettingsDao())

	@BeforeTest
	fun setUp() {
		Dispatchers.setMain(UnconfinedTestDispatcher())
	}

	@AfterTest
	fun tearDown() {
		Dispatchers.resetMain()
	}

	private val store = FakeCustomSoundStore()

	private fun TestScope.viewModel(
		ringer: FakePhoneRinger = FakePhoneRinger(),
		backgroundSync: Boolean = true,
	) = AppSettingsScreenViewModel(
		permissions = permissions,
		appSettingsRepository = repository,
		locationProvider = FakeLocation(permissions),
		phoneFinder = PhoneFinder(ringer, loadSound = { byteArrayOf() }, scope = backgroundScope),
		sounds = FindPhoneSounds(store, repository, ringer, loadBuiltIn = { byteArrayOf(0) }),
		presenceMonitor = FakePresence(backgroundSync),
	)

	private fun picked(name: String = "Song.mp3", bytes: ByteArray = byteArrayOf(1, 2, 3)) =
		PickedAudio(displayName = name, sizeBytes = bytes.size.toLong(), read = { bytes })

	@Test
	fun choosingASoundShowsItsName() = runTest {
		val viewModel = viewModel()

		viewModel.chooseSound(picked())

		assertEquals("Song.mp3", viewModel.findPhone.soundName)
		assertNull(viewModel.state.value.soundRejection)
		assertFalse(viewModel.state.value.isTakingSound)
	}

	@Test
	fun resettingWhileASoundIsTakenEndsWithTheBuiltInTone() = runTest {
		val viewModel = viewModel()
		val reading = CompletableDeferred<Unit>()
		val bytes = byteArrayOf(1, 2, 3)
		viewModel.chooseSound(PickedAudio("Song.mp3", bytes.size.toLong()) { reading.await(); bytes })
		assertTrue(viewModel.state.value.isTakingSound)

		viewModel.resetSound()
		reading.complete(Unit)
		runCurrent()

		assertNull(viewModel.findPhone.soundName)
		assertNull(store.stored)
		assertFalse(viewModel.state.value.isTakingSound)
	}

	@Test
	fun aRejectedSoundSaysWhyAndKeepsThePreviousOne() = runTest {
		val ringer = FakePhoneRinger()
		val viewModel = viewModel(ringer)
		viewModel.chooseSound(picked())

		ringer.playable = { false }
		viewModel.chooseSound(picked(name = "Other.ogg"))

		assertEquals(FindPhoneSounds.Rejection.Unplayable, viewModel.state.value.soundRejection)
		assertEquals("Song.mp3", viewModel.findPhone.soundName)

		// a later pick or a reset clears the message
		viewModel.resetSound()
		assertNull(viewModel.state.value.soundRejection)
		assertNull(viewModel.findPhone.soundName)
	}

	@Test
	fun aMissingCopyShowsAfterComingBack() = runTest {
		val viewModel = viewModel()
		viewModel.chooseSound(picked())
		assertFalse(viewModel.state.value.isCustomSoundMissing)

		store.stored = null
		viewModel.refreshPermissions()

		assertTrue(viewModel.state.value.isCustomSoundMissing)
	}

	@Test
	fun playSoundPlaysAtTheCurrentVolumeUntilToggled() = runTest {
		// the view model's time is this test's, so minutes can pass
		Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
		val ringer = FakePhoneRinger()
		val viewModel = viewModel(ringer)

		viewModel.toggleSoundPlayback()
		assertTrue(viewModel.state.value.isPlayingSound)
		assertEquals(listOf(false), ringer.maxVolumes)

		// a whole track can be heard, it doesn't stop after a few seconds
		advanceTimeBy(5.minutes)
		assertTrue(viewModel.state.value.isPlayingSound)
		assertEquals(0, ringer.cancelCount)

		viewModel.toggleSoundPlayback()
		assertFalse(viewModel.state.value.isPlayingSound)
		assertEquals(1, ringer.cancelCount)
	}

	@Test
	fun playSoundStopsWhenTheScreenGoesToTheBackground() = runTest {
		val ringer = FakePhoneRinger()
		val viewModel = viewModel(ringer)
		viewModel.toggleSoundPlayback()

		viewModel.stopSoundPlayback()

		assertFalse(viewModel.state.value.isPlayingSound)
		assertEquals(1, ringer.cancelCount)
		// nothing to stop is fine too
		viewModel.stopSoundPlayback()
	}

	@Test
	fun playingAgainWhileTheLastOneStopsKeepsPlaying() = runTest {
		Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
		val ringer = FakePhoneRinger().apply { stopTakes = 1.seconds }
		val viewModel = viewModel(ringer)
		viewModel.toggleSoundPlayback()

		// stops, which takes a second, and plays again meanwhile
		viewModel.toggleSoundPlayback()
		viewModel.toggleSoundPlayback()
		advanceTimeBy(2.seconds)

		assertTrue(viewModel.state.value.isPlayingSound)
		assertEquals(2, ringer.sounds.size)
		viewModel.toggleSoundPlayback()
		advanceTimeBy(2.seconds)
		assertFalse(viewModel.state.value.isPlayingSound)
	}

	@Test
	fun playSoundThatFailsAtOnceShowsItStopped() = runTest {
		val ringer = FakePhoneRinger().apply { failure = IllegalStateException("no player") }
		val viewModel = viewModel(ringer)

		viewModel.toggleSoundPlayback()

		assertFalse(viewModel.state.value.isPlayingSound)
	}

	private val AppSettingsScreenViewModel.findPhone get() = state.value.findPhone

	@Test
	fun turningFindPhoneOnAsksForNotifications() = runTest {
		val viewModel = viewModel()

		viewModel.setFindPhone(true)

		assertEquals(listOf<Permission>(Permission.REMOTE_NOTIFICATION), permissions.requested)
		assertTrue(viewModel.findPhone.enabled)
		assertTrue(repository.findPhoneSettings().enabled)
		assertTrue(viewModel.state.value.isNotificationPermissionGranted)
		assertFalse(viewModel.state.value.notificationPermissionDenied)
	}

	@Test
	fun refusedNotificationsKeepFindPhoneOff() = runTest {
		permissions.userAllows = false
		val viewModel = viewModel()

		viewModel.setFindPhone(true)

		assertFalse(viewModel.findPhone.enabled)
		assertTrue(viewModel.state.value.notificationPermissionDenied)

		// asking again shows the prompt again, and allowing turns it on
		permissions.userAllows = true
		viewModel.setFindPhone(true)
		assertTrue(viewModel.findPhone.enabled)
		assertFalse(viewModel.state.value.notificationPermissionDenied)
	}

	@Test
	fun grantedNotificationsArentAskedForAgain() = runTest {
		permissions.granted += Permission.REMOTE_NOTIFICATION
		val viewModel = viewModel()

		viewModel.setFindPhone(true)

		assertTrue(permissions.requested.isEmpty())
		assertTrue(viewModel.findPhone.enabled)
	}

	@Test
	fun turningFindPhoneOffAsksForNothing() = runTest {
		val viewModel = viewModel()
		viewModel.setFindPhone(true)
		permissions.requested.clear()

		viewModel.setFindPhone(false)

		assertFalse(viewModel.findPhone.enabled)
		assertTrue(permissions.requested.isEmpty())
	}

	@Test
	fun notificationsTurnedOffSinceShowAWarning() = runTest {
		val viewModel = viewModel()
		viewModel.setFindPhone(true)
		assertFalse(viewModel.state.value.isNotificationPermissionMissing)

		// the user took it away in the system settings, then came back to the screen
		permissions.granted -= Permission.REMOTE_NOTIFICATION
		viewModel.refreshPermissions()

		assertTrue(viewModel.state.value.isNotificationPermissionMissing)
	}

	@Test
	fun ringLengthIsStoredOnlyWhenAtLeastASecond() = runTest {
		val viewModel = viewModel()

		viewModel.setRingLength(2.minutes)
		assertEquals(2.minutes, viewModel.findPhone.ringLength)

		viewModel.setRingLength(Duration.ZERO)
		assertEquals(2.minutes, viewModel.findPhone.ringLength)
		assertEquals(2.minutes, repository.findPhoneSettings().ringLength)

		assertTrue(AppSettingsScreenViewModel.isValidRingLength(1.seconds))
		assertFalse(AppSettingsScreenViewModel.isValidRingLength(Duration.ZERO))
	}

	@Test
	fun ringUntilStoppedKeepsTheLength() = runTest {
		val viewModel = viewModel()
		viewModel.setRingLength(5.minutes)

		viewModel.setRingUntilStopped(true)
		assertTrue(viewModel.findPhone.ringUntilStopped)
		assertEquals(null, viewModel.findPhone.ringDuration)

		viewModel.setRingUntilStopped(false)
		assertEquals(5.minutes, viewModel.findPhone.ringDuration)
	}

	@Test
	fun fullVolumeCanBeTurnedOff() = runTest {
		val viewModel = viewModel()
		assertTrue(viewModel.findPhone.maxVolume)

		viewModel.setMaxVolume(false)

		assertFalse(viewModel.findPhone.maxVolume)
		assertFalse(repository.findPhoneSettings().maxVolume)
	}

	@Test
	fun findPhoneIsUnsupportedWithoutARingerOrBackgroundSync() = runTest {
		assertTrue(viewModel().state.value.isFindPhoneSupported)
		assertFalse(viewModel(ringer = FakePhoneRinger(isSupported = false)).state.value.isFindPhoneSupported)
		assertFalse(viewModel(backgroundSync = false).state.value.isFindPhoneSupported)
	}

	@Test
	fun volumeNoteFollowsTheRinger() = runTest {
		assertTrue(viewModel().state.value.canForceMaxVolume)
		assertFalse(viewModel(ringer = FakePhoneRinger(canForceMaxVolume = false)).state.value.canForceMaxVolume)
	}

	@Test
	fun locationRecordingStillAsksForBothLocationPermissions() = runTest {
		val viewModel = viewModel()

		viewModel.setLocationRecording(true)

		assertEquals(LOCATION_PERMISSIONS, permissions.requested)
		assertTrue(viewModel.state.value.isLocationRecordingEnabled)

		permissions.userAllows = false
		permissions.granted.clear()
		viewModel.setLocationRecording(false)
		viewModel.setLocationRecording(true)
		assertFalse(viewModel.state.value.isLocationRecordingEnabled)
		assertTrue(viewModel.state.value.locationPermissionDenied)
	}
}
