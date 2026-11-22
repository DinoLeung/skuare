package xyz.d1n0.sync

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import xyz.d1n0.data.database.AppSettings
import xyz.d1n0.data.database.FakeAppConfigDao
import xyz.d1n0.data.database.FakeAppSettingsDao
import xyz.d1n0.data.database.FakePairedWatchDao
import xyz.d1n0.data.database.FakePressedLocationDao
import xyz.d1n0.data.database.PairedWatch
import xyz.d1n0.data.database.PressedLocation
import xyz.d1n0.data.database.SyncFailure
import xyz.d1n0.data.repository.AppSettingsRepository
import xyz.d1n0.data.repository.InstallationRepository
import xyz.d1n0.data.repository.PairedWatchRepository
import xyz.d1n0.data.repository.PressedLocationRepository
import xyz.d1n0.phone.FakeLocationProvider
import xyz.d1n0.phone.FakePhoneRinger
import xyz.d1n0.phone.GeoPoint
import xyz.d1n0.phone.LocationResult
import xyz.d1n0.phone.PhoneFinder
import xyz.d1n0.lib.connection.WatchConnectionManager
import xyz.d1n0.lib.constant.ConnectReason
import xyz.d1n0.lib.constant.OpCode
import xyz.d1n0.lib.model.AppInfo
import xyz.d1n0.lib.model.CustomWatchModel
import xyz.d1n0.lib.model.WatchErrorPacket
import xyz.d1n0.lib.model.WatchFeatures
import xyz.d1n0.lib.model.WatchInfo
import xyz.d1n0.lib.model.WatchModel
import xyz.d1n0.lib.model.WatchName
import xyz.d1n0.lib.model.resolveWatchModel
import xyz.d1n0.lib.model.shouldSwitchToCustomModel
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class) // advanceTimeBy, runCurrent, currentTime
class WatchSessionHandlerTest {
	private companion object {
		const val ADDRESS = "ED:A1:AF:D5:A2:40"
		val TONE = byteArrayOf(1, 2, 3)
		val OUR_APP_INFO = AppInfo(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10))
		val OTHER_APP_INFO = AppInfo(byteArrayOf(10, 9, 8, 7, 6, 5, 4, 3, 2, 1))
		val PAIRED_AT = Instant.parse("2026-01-01T00:00:00Z")
		const val UNKNOWN_NAME = "CASIO GBD-H2000"
		val ADJUSTED_MODEL = CustomWatchModel(clockCount = 4, reminderCount = 5, hasWeekdayLanguage = true)
	}

	private class FakePresenceMonitor : WatchPresenceMonitor {
		override val isSupported = true
		val registered = mutableListOf<PairedWatch>()
		val unregistered = mutableListOf<PairedWatch>()

		override suspend fun register(watch: PairedWatch): PairedWatch {
			registered += watch
			return watch.copy(platformAssociationId = 42)
		}

		override suspend fun unregister(watch: PairedWatch) {
			unregistered += watch
		}
	}

	private class FakeConnectionNotifier : WatchConnectionNotifier {
		val events = mutableListOf<String>()

		override fun connectionStarted(name: String?) {
			events += "connected:$name"
		}

		override fun connectionNameUpdated(name: String?) {
			events += "connected:$name"
		}

		override fun timeSyncStarted() {
			events += "syncing"
		}

		override fun timeSyncFinished(name: String?) {
			events += "connected:$name"
		}

		override fun connectionEnded() {
			events += "disconnected"
		}

		override fun savingLocation() {
			events += "saving location"
		}
	}

	private class FakeConnectedWatch(
		override val advertisedName: String? = "CASIO GMW-B5000#",
		address: String? = ADDRESS,
		connectReason: ConnectReason? = ConnectReason.AUTO_SYNC,
		var appInfo: AppInfo = OUR_APP_INFO,
		private val reportedName: String? = "Test watch",
		var adjustResult: Result<Unit> = Result.success(Unit),
		customModel: CustomWatchModel? = null,
	) : ConnectedWatch {
		private val mutableInfo = MutableStateFlow(
			WatchInfo(address = address, connectReason = connectReason),
		)
		override val info: StateFlow<WatchInfo> = mutableInfo

		// like the Watch constructor
		override var model: WatchFeatures = resolveWatchModel(advertisedName, customModel)
			private set

		/** The model when [adjustTime] was last called, null if it wasn't. */
		var modelAtAdjust: WatchFeatures? = null

		/** Every model passed to [useCustomModel], in order, including ones that changed nothing. */
		val customModelsUsed = mutableListOf<CustomWatchModel>()

		override fun useCustomModel(customModel: CustomWatchModel) {
			customModelsUsed += customModel
			if (shouldSwitchToCustomModel(model, customModel)) model = customModel
		}

		val disconnected = CompletableDeferred<Unit>()
		var queryFailure: Pair<OpCode, Throwable>? = null
		var blockedQuery: OpCode? = null
		var timedOutQuery: OpCode? = null
		var ignoreAppInfoWrites = false
		var appInfoWriteCount = 0
		var statusRequestCount = 0
		var adjustCount = 0
		var disconnectCount = 0

		override suspend fun query(opCode: OpCode): ByteArray {
			if (blockedQuery == opCode) awaitCancellation()
			if (timedOutQuery == opCode) withTimeout(1.seconds) { awaitCancellation() }
			queryFailure?.takeIf { it.first == opCode }?.let { throw it.second }
			return when (opCode) {
				OpCode.APP_INFO -> appInfo.packet
				OpCode.WATCH_NAME -> {
					mutableInfo.update { it.copy(name = reportedName?.let(::WatchName)) }
					byteArrayOf(opCode.byte)
				}
				else -> byteArrayOf(opCode.byte)
			}
		}

		override suspend fun writeAppInfo(appInfo: AppInfo) {
			appInfoWriteCount++
			if (!ignoreAppInfoWrites) this.appInfo = appInfo
		}

		override suspend fun requestWatchStatus() {
			statusRequestCount++
		}

		/** How long the time adjustment takes, in the test's virtual time. */
		var adjustDuration: Duration = Duration.ZERO

		override suspend fun adjustTime(): Result<Unit> {
			adjustCount++
			modelAtAdjust = model
			delay(adjustDuration)
			return adjustResult
		}

		override suspend fun disconnect() {
			disconnectCount++
			disconnected.complete(Unit)
		}

		override suspend fun awaitDisconnected() {
			disconnected.await()
		}
	}

	/**
	 * @param phoneFinderScope Where the phone rings, find phone tests pass the test's backgroundScope
	 *        for virtual time and so a ring left running doesn't keep the test waiting.
	 */
	private class Fixture(
		val locationProvider: FakeLocationProvider = FakeLocationProvider(),
		val ringer: FakePhoneRinger = FakePhoneRinger(),
		phoneFinderScope: CoroutineScope = CoroutineScope(SupervisorJob()),
	) {
		val dao = FakePairedWatchDao()
		val repository = PairedWatchRepository(dao)
		val presenceMonitor = FakePresenceMonitor()
		val connectionNotifier = FakeConnectionNotifier()
		val installationRepository = InstallationRepository(FakeAppConfigDao(OUR_APP_INFO.id))
		val settingsDao = FakeAppSettingsDao()
		val appSettingsRepository = AppSettingsRepository(settingsDao)
		val locationDao = FakePressedLocationDao(dao)
		val phoneFinder = PhoneFinder(ringer, loadSound = { TONE }, scope = phoneFinderScope)
		val handler = WatchSessionHandler(
			connectionManager = WatchConnectionManager(),
			pairedWatchRepository = repository,
			installationRepository = installationRepository,
			presenceMonitor = presenceMonitor,
			appSettingsRepository = appSettingsRepository,
			pressedLocationRepository = PressedLocationRepository(locationDao),
			locationProvider = locationProvider,
			phoneFinder = phoneFinder,
			connectionNotifier = connectionNotifier,
		)

		suspend fun enableLocationRecording() = appSettingsRepository.setLocationRecordingEnabled(true)

		suspend fun enableFindPhone(ringLength: Duration = 60.seconds, untilStopped: Boolean = false) {
			appSettingsRepository.setFindPhoneEnabled(true)
			appSettingsRepository.setFindPhoneRingLength(ringLength)
			appSettingsRepository.setFindPhoneRingUntilStopped(untilStopped)
		}

		suspend fun pair(
			address: String = ADDRESS,
			advertisedName: String = "CASIO GMW-B5000#",
			displayName: String = advertisedName,
			customModel: CustomWatchModel? = null,
			customModelIntroShown: Boolean = false,
		): PairedWatch = PairedWatch(
			address = address,
			advertisedName = advertisedName,
			displayName = displayName,
			pairedAt = PAIRED_AT,
			customModel = customModel,
			customModelIntroShown = customModelIntroShown,
		).also { repository.upsert(it) }
	}

	@Test
	fun foregroundPairsRegistersSynchronizesAndRecordsResult() = runTest {
		val fixture = Fixture()
		val watch = FakeConnectedWatch(appInfo = OTHER_APP_INFO, reportedName = "My square")

		val result = fixture.handler.runForeground(watch)

		assertTrue(result.isSuccess)
		assertEquals(1, watch.appInfoWriteCount)
		assertEquals(OUR_APP_INFO, watch.appInfo)
		assertEquals(1, watch.statusRequestCount)
		assertEquals(1, watch.adjustCount)
		assertEquals(1, fixture.presenceMonitor.registered.size)
		val stored = assertNotNull(fixture.repository.find(ADDRESS))
		assertEquals("CASIO GMW-B5000#", stored.advertisedName)
		assertEquals("My square", stored.displayName)
		assertEquals(42, stored.platformAssociationId)
		assertNotNull(stored.lastSyncAt)
		assertNull(stored.lastSyncFailure)
		assertEquals(
			listOf("connected:My square", "syncing", "connected:My square"),
			fixture.connectionNotifier.events,
		)
	}

	@Test
	fun foregroundReportsPairingFailureButStillAttemptsStatusAndSync() = runTest {
		val fixture = Fixture()
		val failure = IllegalStateException("app info unavailable")
		val watch = FakeConnectedWatch(address = null).also {
			it.queryFailure = OpCode.APP_INFO to failure
		}

		val result = fixture.handler.runForeground(watch)

		assertTrue(result.exceptionOrNull() === failure)
		assertEquals(1, watch.statusRequestCount)
		assertEquals(1, watch.adjustCount)
		assertNull(fixture.repository.find(ADDRESS))
	}

	@Test
	fun foregroundPairsUnknownWatchWithDefaultCustomModel() = runTest {
		val fixture = Fixture()
		val watch = FakeConnectedWatch(advertisedName = UNKNOWN_NAME, appInfo = OTHER_APP_INFO)

		val result = fixture.handler.runForeground(watch)

		assertTrue(result.isSuccess)
		val stored = assertNotNull(fixture.repository.find(ADDRESS))
		assertEquals(CustomWatchModel(), stored.customModel)
		assertFalse(stored.customModelIntroShown)
		assertEquals(CustomWatchModel(), watch.modelAtAdjust)
	}

	@Test
	fun foregroundPairsKnownWatchWithoutCustomModel() = runTest {
		val fixture = Fixture()
		val watch = FakeConnectedWatch(advertisedName = "CASIO GW-BX5600", appInfo = OTHER_APP_INFO)

		fixture.handler.runForeground(watch)

		assertNull(assertNotNull(fixture.repository.find(ADDRESS)).customModel)
		assertTrue(watch.customModelsUsed.isEmpty())
		assertEquals(WatchModel.MIP_SQUARE, watch.modelAtAdjust)
	}

	@Test
	fun foregroundUsesStoredCustomModelBeforeAdjustingTime() = runTest {
		val fixture = Fixture()
		fixture.pair(advertisedName = UNKNOWN_NAME, customModel = ADJUSTED_MODEL, customModelIntroShown = true)
		val watch = FakeConnectedWatch(advertisedName = UNKNOWN_NAME)

		fixture.handler.runForeground(watch)

		assertEquals(listOf(ADJUSTED_MODEL), watch.customModelsUsed)
		assertEquals(ADJUSTED_MODEL, watch.modelAtAdjust)
	}

	@Test
	fun repairingKeepsAdjustedCustomModelAndIntroFlag() = runTest {
		val fixture = Fixture()
		fixture.pair(advertisedName = UNKNOWN_NAME, customModel = ADJUSTED_MODEL, customModelIntroShown = true)
		// the watch's app info was reset, so it is paired again
		val watch = FakeConnectedWatch(advertisedName = UNKNOWN_NAME, appInfo = OTHER_APP_INFO)

		fixture.handler.runForeground(watch)

		assertEquals(1, watch.appInfoWriteCount)
		val stored = assertNotNull(fixture.repository.find(ADDRESS))
		assertEquals(ADJUSTED_MODEL, stored.customModel)
		assertTrue(stored.customModelIntroShown)
	}

	@Test
	fun foregroundGivesExistingRowWithoutCustomModelTheDefaultWhenWatchIsUnknown() = runTest {
		val fixture = Fixture()
		// stored while the watch advertised a known name
		fixture.pair(advertisedName = "CASIO GW-BX5600")
		val watch = FakeConnectedWatch(advertisedName = null)

		fixture.handler.runForeground(watch)

		assertEquals(CustomWatchModel(), assertNotNull(fixture.repository.find(ADDRESS)).customModel)
	}

	@Test
	fun foregroundClearsCustomModelOfWatchThatIsNowKnown() = runTest {
		val fixture = Fixture()
		// stored while GW-BX5600 wasn't recognised yet
		fixture.pair(advertisedName = "CASIO GW-BX5600", customModel = ADJUSTED_MODEL, customModelIntroShown = true)
		val watch = FakeConnectedWatch(advertisedName = "CASIO GW-BX5600")

		fixture.handler.runForeground(watch)

		val stored = assertNotNull(fixture.repository.find(ADDRESS))
		assertNull(stored.customModel)
		assertFalse(stored.customModelIntroShown)
		assertTrue(watch.customModelsUsed.isEmpty())
		assertEquals(WatchModel.MIP_SQUARE, watch.modelAtAdjust)
		// the guard now refuses it too
		assertFalse(fixture.repository.updateCustomModel(ADDRESS, ADJUSTED_MODEL))
	}

	@Test
	fun backgroundClearsCustomModelOfWatchThatIsNowKnown() = runTest {
		val fixture = Fixture()
		fixture.pair(
			advertisedName = "CASIO GW-BX5600",
			displayName = "Old name",
			customModel = ADJUSTED_MODEL,
			customModelIntroShown = true,
		)
		var watch: FakeConnectedWatch? = null

		val result = fixture.handler.runBackground(ADDRESS) { stored ->
			FakeConnectedWatch(advertisedName = "CASIO GW-BX5600", reportedName = "New name", customModel = stored?.customModel)
				.also { watch = it }
		}

		assertIs<BackgroundSessionResult.Synced>(result)
		assertEquals(WatchModel.MIP_SQUARE, watch?.modelAtAdjust)
		val stored = assertNotNull(fixture.repository.find(ADDRESS))
		assertNull(stored.customModel)
		// only the custom model is cleared, the rest of the row is still updated
		assertEquals("New name", stored.displayName)
		assertNotNull(stored.lastSyncAt)
		assertFalse(stored.customModelIntroShown)
	}

	@Test
	fun watchButtonSessionClearsCustomModelOfWatchThatIsNowKnown() = runTest {
		val fixture = Fixture()
		fixture.pair(advertisedName = "CASIO GW-BX5600", customModel = ADJUSTED_MODEL, customModelIntroShown = true)
		val watch = FakeConnectedWatch(advertisedName = "CASIO GW-BX5600", connectReason = ConnectReason.DEFAULT)

		val result = fixture.handler.runBackground(ADDRESS) { watch }

		// the app takes this watch over without running the foreground session
		assertIs<BackgroundSessionResult.OpenAppRequested>(result)
		val stored = assertNotNull(fixture.repository.find(ADDRESS))
		assertNull(stored.customModel)
		assertFalse(stored.customModelIntroShown)
		fixture.handler.dropPendingForegroundWatch()
	}

	@Test
	fun watchButtonSessionGivesWatchThatIsNowUnknownTheDefaultCustomModel() = runTest {
		val fixture = Fixture()
		// stored while the watch advertised a known name
		fixture.pair(advertisedName = "CASIO GW-BX5600")
		val watch = FakeConnectedWatch(advertisedName = UNKNOWN_NAME, connectReason = ConnectReason.DEFAULT)

		val result = fixture.handler.runBackground(ADDRESS) { watch }

		// the app takes this watch over without running the foreground session
		assertIs<BackgroundSessionResult.OpenAppRequested>(result)
		val stored = assertNotNull(fixture.repository.find(ADDRESS))
		assertEquals(CustomWatchModel(), stored.customModel)
		assertFalse(stored.customModelIntroShown)
		// the Adjust button can store changes now
		assertTrue(fixture.repository.updateCustomModel(ADDRESS, ADJUSTED_MODEL))
		fixture.handler.dropPendingForegroundWatch()
	}

	@Test
	fun backgroundKeepsCustomModelOfUnknownWatch() = runTest {
		val fixture = Fixture()
		fixture.pair(advertisedName = UNKNOWN_NAME, customModel = ADJUSTED_MODEL)

		fixture.handler.runBackground(ADDRESS) { stored ->
			FakeConnectedWatch(advertisedName = UNKNOWN_NAME, customModel = stored?.customModel)
		}

		assertEquals(ADJUSTED_MODEL, assertNotNull(fixture.repository.find(ADDRESS)).customModel)
	}

	@Test
	fun foregroundKeepsDefaultModelWhenPairingFails() = runTest {
		val fixture = Fixture()
		val watch = FakeConnectedWatch(advertisedName = UNKNOWN_NAME, address = null)

		val result = fixture.handler.runForeground(watch)

		assertTrue(result.isFailure)
		assertTrue(watch.customModelsUsed.isEmpty())
		assertEquals(1, watch.adjustCount)
		assertEquals(CustomWatchModel(), watch.modelAtAdjust)
	}

	@Test
	fun backgroundConnectsWithStoredCustomModel() = runTest {
		val fixture = Fixture()
		fixture.pair(advertisedName = UNKNOWN_NAME, customModel = ADJUSTED_MODEL)
		var watch: FakeConnectedWatch? = null

		val result = fixture.handler.runBackground(ADDRESS) { stored ->
			FakeConnectedWatch(advertisedName = UNKNOWN_NAME, customModel = stored?.customModel)
				.also { watch = it }
		}

		assertIs<BackgroundSessionResult.Synced>(result)
		assertEquals(ADJUSTED_MODEL, watch?.modelAtAdjust)
	}

	@Test
	fun initialNotificationUsesStoredDisplayNameFoundByPeripheralIdentifier() = runTest {
		val fixture = Fixture()
		fixture.pair(displayName = "Stored watch")
		val manager = WatchConnectionManager(pairedWatchRepository = fixture.repository)

		val name = manager.resolveInitialNotificationName(
			identifier = ADDRESS.lowercase(),
			peripheralName = "CASIO GMW-B5000#",
		)

		assertEquals("Stored watch", name)
	}

	@Test
	fun initialNotificationFallsBackToPeripheralNameForUnknownWatch() = runTest {
		val fixture = Fixture()
		val manager = WatchConnectionManager(pairedWatchRepository = fixture.repository)

		val name = manager.resolveInitialNotificationName(
			identifier = ADDRESS,
			peripheralName = "CASIO GMW-B5000#",
		)

		assertEquals("CASIO GMW-B5000#", name)
	}

	@Test
	fun backgroundReturnsBusyWithoutStartingSession() = runTest {
		val fixture = Fixture()
		var called = false
		var stored: PairedWatch? = null

		val result = fixture.handler.runBackground(ADDRESS) {
			called = true
			stored = it
			null
		}

		assertEquals(BackgroundSessionResult.Busy, result)
		assertTrue(called)
		assertNull(stored)
	}

	@Test
	fun backgroundSkipsWatchPairedWithAnotherInstallAndDisconnects() = runTest {
		val fixture = Fixture()
		fixture.pair()
		val watch = FakeConnectedWatch(appInfo = OTHER_APP_INFO)

		val result = fixture.handler.runBackground(ADDRESS) { watch }

		val skipped = assertIs<BackgroundSessionResult.Skipped>(result)
		assertTrue(skipped.reason.contains("another app"))
		assertEquals(0, watch.adjustCount)
		assertEquals(1, watch.disconnectCount)
	}

	@Test
	fun backgroundSkipsWatchMissingFromPairedDatabase() = runTest {
		val fixture = Fixture()
		val watch = FakeConnectedWatch()

		val result = fixture.handler.runBackground(ADDRESS) { watch }

		val skipped = assertIs<BackgroundSessionResult.Skipped>(result)
		assertTrue(skipped.reason.contains("not paired"))
		assertEquals(1, watch.disconnectCount)
	}

	@Test
	fun automaticSyncUpdatesNameRecordsSuccessAndDisconnects() = runTest {
		val fixture = Fixture()
		fixture.pair(displayName = "Old name")
		// the last sync failed, a successful one must clear it
		fixture.repository.recordSync(ADDRESS, failure = SyncFailure.TIMEOUT, errorDetail = "Timed out")
		val watch = FakeConnectedWatch(reportedName = "New name")
		var receivedStored: PairedWatch? = null

		val result = fixture.handler.runBackground(ADDRESS) { stored ->
			receivedStored = stored
			watch
		}

		val synced = assertIs<BackgroundSessionResult.Synced>(result)
		assertNull(synced.error)
		assertEquals("CASIO GMW-B5000#", receivedStored?.advertisedName)
		assertEquals(1, watch.adjustCount)
		assertEquals(1, watch.disconnectCount)
		val stored = assertNotNull(fixture.repository.find(ADDRESS))
		assertEquals("New name", stored.displayName)
		assertNotNull(stored.lastSyncAt)
		assertNull(stored.lastSyncFailure)
		assertNull(stored.lastSyncErrorDetail)
		assertEquals(
			listOf("connected:New name", "syncing", "connected:New name"),
			fixture.connectionNotifier.events,
		)
	}

	@Test
	fun failedTimeAdjustmentIsReturnedAndRecorded() = runTest {
		val fixture = Fixture()
		fixture.pair(displayName = "Stored watch")
		val failure = IllegalStateException("write failed")
		val watch = FakeConnectedWatch(
			connectReason = ConnectReason.MANUAL_SYNC,
			reportedName = null,
			adjustResult = Result.failure(failure),
		)

		val result = fixture.handler.runBackground(ADDRESS) { watch }

		val synced = assertIs<BackgroundSessionResult.Synced>(result)
		assertTrue(synced.error === failure)
		val stored = assertNotNull(fixture.repository.find(ADDRESS))
		assertEquals(SyncFailure.OTHER, stored.lastSyncFailure)
		assertEquals("write failed", stored.lastSyncErrorDetail)
		assertEquals(1, watch.disconnectCount)
		assertEquals(
			listOf(
				"connected:Stored watch",
				"syncing",
				"connected:Stored watch",
			),
			fixture.connectionNotifier.events,
		)
	}

	@Test
	fun watchErrorDuringTimeAdjustmentIsRecordedWithItsBytes() = runTest {
		val fixture = Fixture()
		fixture.pair()
		val watch = FakeConnectedWatch(
			connectReason = ConnectReason.AUTO_SYNC,
			adjustResult = Result.failure(WatchErrorPacket("FF811D".hexToByteArray())),
		)

		fixture.handler.runBackground(ADDRESS) { watch }

		val stored = assertNotNull(fixture.repository.find(ADDRESS))
		assertEquals(SyncFailure.WATCH_ERROR, stored.lastSyncFailure)
		assertEquals("FF 81 1D", stored.lastSyncErrorDetail)
	}

	@Test
	fun setupRequestHoldsConnectionUntilPromptIsDropped() = runTest {
		val fixture = Fixture()
		fixture.pair()
		val watch = FakeConnectedWatch(connectReason = ConnectReason.SETUP)

		val result = fixture.handler.runBackground(ADDRESS) { watch }

		val open = assertIs<BackgroundSessionResult.OpenAppRequested>(result)
		assertTrue(open.watch === watch)
		assertTrue(fixture.handler.pendingForegroundWatch.value === watch)
		assertEquals(0, watch.disconnectCount)

		fixture.handler.dropPendingForegroundWatch()

		assertNull(fixture.handler.pendingForegroundWatch.value)
		assertEquals(1, watch.disconnectCount)
	}

	@Test
	fun disconnectedHeldWatchClearsPendingHandoff() = runTest {
		val fixture = Fixture()
		fixture.pair()
		val watch = FakeConnectedWatch(connectReason = ConnectReason.DEFAULT)
		fixture.handler.runBackground(ADDRESS) { watch }

		watch.disconnected.complete(Unit)
		fixture.handler.pendingForegroundWatch.first { it == null }

		assertNull(fixture.handler.pendingForegroundWatch.value)
	}

	@Test
	fun findWithFindPhoneOffOnlyDisconnects() = runTest {
		val fixture = Fixture(phoneFinderScope = backgroundScope)
		fixture.pair()
		val watch = FakeConnectedWatch(connectReason = ConnectReason.FIND)

		val result = fixture.handler.runBackground(ADDRESS) { watch }
		runCurrent()

		assertEquals(BackgroundSessionResult.FoundPhone(rang = false), result)
		assertTrue(fixture.ringer.sounds.isEmpty())
		assertEquals(0, watch.adjustCount)
		assertEquals(1, watch.disconnectCount)
	}

	@Test
	fun findRingsForTheSetLengthAfterTheWatchIsLetGo() = runTest {
		val fixture = Fixture(phoneFinderScope = backgroundScope)
		fixture.pair()
		fixture.enableFindPhone(ringLength = 90.seconds)
		val watch = FakeConnectedWatch(connectReason = ConnectReason.FIND)

		val result = fixture.handler.runBackground(ADDRESS) { watch }
		runCurrent()

		// the session is over and the watch disconnected, the phone still rings, with no time sync
		assertEquals(BackgroundSessionResult.FoundPhone(rang = true), result)
		assertEquals(1, watch.disconnectCount)
		assertEquals(0, watch.adjustCount)
		assertTrue(fixture.phoneFinder.ringing.value)
		assertContentEquals(TONE, fixture.ringer.sounds.single())

		advanceTimeBy(89.seconds)
		assertTrue(fixture.phoneFinder.ringing.value)
		advanceTimeBy(2.seconds)
		assertFalse(fixture.phoneFinder.ringing.value)
	}

	@Test
	fun findUntilStoppedRingsUntilStopped() = runTest {
		val fixture = Fixture(phoneFinderScope = backgroundScope)
		fixture.pair()
		fixture.enableFindPhone(untilStopped = true)

		fixture.handler.runBackground(ADDRESS) { FakeConnectedWatch(connectReason = ConnectReason.FIND) }
		advanceTimeBy(1.days)

		assertTrue(fixture.phoneFinder.ringing.value)
		fixture.phoneFinder.stop()
		assertFalse(fixture.phoneFinder.ringing.value)
	}

	@Test
	fun findWithAStoredLengthThatIsntPositiveRingsForAMinute() = runTest {
		val fixture = Fixture(phoneFinderScope = backgroundScope)
		fixture.pair()
		fixture.settingsDao.store(AppSettings(findPhoneEnabled = true, findPhoneRingSeconds = 0))

		fixture.handler.runBackground(ADDRESS) { FakeConnectedWatch(connectReason = ConnectReason.FIND) }
		advanceTimeBy(59.seconds)
		assertTrue(fixture.phoneFinder.ringing.value)
		advanceTimeBy(2.seconds)

		assertFalse(fixture.phoneFinder.ringing.value)
	}

	@Test
	fun aSecondFindWhileRingingRestartsTheTimer() = runTest {
		val fixture = Fixture(phoneFinderScope = backgroundScope)
		fixture.pair()
		fixture.enableFindPhone(ringLength = 60.seconds)

		fixture.handler.runBackground(ADDRESS) { FakeConnectedWatch(connectReason = ConnectReason.FIND) }
		advanceTimeBy(40.seconds)
		fixture.handler.runBackground(ADDRESS) { FakeConnectedWatch(connectReason = ConnectReason.FIND) }
		advanceTimeBy(40.seconds)

		// 80 s after the first press, 40 s after the second
		assertTrue(fixture.phoneFinder.ringing.value)
		assertEquals(1, fixture.ringer.sounds.size)
		advanceTimeBy(21.seconds)
		assertFalse(fixture.phoneFinder.ringing.value)
	}

	@Test
	fun findOnAPlatformThatCantRingDoesNothing() = runTest {
		val fixture = Fixture(ringer = FakePhoneRinger(isSupported = false), phoneFinderScope = backgroundScope)
		fixture.pair()
		fixture.enableFindPhone()

		val result = fixture.handler.runBackground(ADDRESS) { FakeConnectedWatch(connectReason = ConnectReason.FIND) }
		runCurrent()

		assertEquals(BackgroundSessionResult.FoundPhone(rang = false), result)
		assertTrue(fixture.ringer.sounds.isEmpty())
	}

	@Test
	fun findFromAWatchPairedWithAnotherAppDoesntRing() = runTest {
		val fixture = Fixture(phoneFinderScope = backgroundScope)
		fixture.pair()
		fixture.enableFindPhone()
		val watch = FakeConnectedWatch(connectReason = ConnectReason.FIND, appInfo = OTHER_APP_INFO)

		val result = fixture.handler.runBackground(ADDRESS) { watch }
		runCurrent()

		assertIs<BackgroundSessionResult.Skipped>(result)
		assertTrue(fixture.ringer.sounds.isEmpty())
	}

	@Test
	fun findFromAnUnpairedWatchDoesntRing() = runTest {
		val fixture = Fixture(phoneFinderScope = backgroundScope)
		fixture.enableFindPhone()

		val result = fixture.handler.runBackground(ADDRESS) { FakeConnectedWatch(connectReason = ConnectReason.FIND) }
		runCurrent()

		assertIs<BackgroundSessionResult.Skipped>(result)
		assertTrue(fixture.ringer.sounds.isEmpty())
	}

	@Test
	fun connectingFromTheAppWithALongPressDoesntRing() = runTest {
		val fixture = Fixture(phoneFinderScope = backgroundScope)
		fixture.enableFindPhone()

		fixture.handler.runForeground(FakeConnectedWatch(connectReason = ConnectReason.FIND))
		runCurrent()

		assertTrue(fixture.ringer.sounds.isEmpty())
	}

	@Test
	fun backgroundFailureIsReturnedAndDisconnected() = runTest {
		val fixture = Fixture()
		fixture.pair()
		val failure = IllegalStateException("read failed")
		val watch = FakeConnectedWatch().also { it.queryFailure = OpCode.APP_INFO to failure }

		val result = fixture.handler.runBackground(ADDRESS) { watch }

		val failed = assertIs<BackgroundSessionResult.Failed>(result)
		assertEquals(failure::class, failed.error::class)
		assertEquals(failure.message, failed.error.message)
		assertEquals(1, watch.disconnectCount)
	}

	@Test
	fun wholeBackgroundSessionTimeoutIsReturnedAndDisconnected() = runTest {
		val fixture = Fixture()
		fixture.pair()
		val watch = FakeConnectedWatch().also { it.blockedQuery = OpCode.APP_INFO }

		val result = fixture.handler.runBackground(ADDRESS, timeout = 1.seconds) { watch }

		val failed = assertIs<BackgroundSessionResult.Failed>(result)
		assertTrue(failed.error.message?.contains("Timed out") == true)
		assertEquals(1, watch.disconnectCount)
	}

	@Test
	fun nestedWatchQueryTimeoutIsReturnedAndDisconnected() = runTest {
		val fixture = Fixture()
		fixture.pair()
		val watch = FakeConnectedWatch().also { it.timedOutQuery = OpCode.APP_INFO }

		val result = fixture.handler.runBackground(ADDRESS, timeout = 30.seconds) { watch }

		val failed = assertIs<BackgroundSessionResult.Failed>(result)
		assertTrue(failed.error is kotlinx.coroutines.TimeoutCancellationException)
		assertEquals(1, watch.disconnectCount)
	}

	@Test
	fun forgetUnregistersDeletesAndDisconnectsMatchingWatch() = runTest {
		val fixture = Fixture()
		val paired = fixture.pair()
		val watch = FakeConnectedWatch()

		fixture.handler.forget(ADDRESS, deleteLocations = false, connectedWatch = watch)

		assertEquals(listOf(paired), fixture.presenceMonitor.unregistered)
		assertNull(fixture.repository.find(ADDRESS))
		assertEquals(1, watch.disconnectCount)
	}

	@Test
	fun forgetDoesNotDisconnectDifferentWatch() = runTest {
		val fixture = Fixture()
		fixture.pair()
		val other = FakeConnectedWatch(address = "00:11:22:33:44:55")

		fixture.handler.forget(ADDRESS, deleteLocations = false, connectedWatch = other)

		assertEquals(0, other.disconnectCount)
		assertFalse(fixture.presenceMonitor.unregistered.isEmpty())
	}

	@Test
	fun manualSyncRecordsLocationAfterTheWatchDisconnected() = runTest {
		val fixture = Fixture()
		fixture.pair()
		fixture.enableLocationRecording()
		val answer = CompletableDeferred<Unit>()
		fixture.locationProvider.answer = answer
		val watch = FakeConnectedWatch(connectReason = ConnectReason.MANUAL_SYNC)
		val before = Clock.System.now()

		val session = async { fixture.handler.runBackground(ADDRESS) { watch } }
		// the time is adjusted and the watch let go while the location read is still open
		watch.disconnected.await()
		assertEquals(1, watch.adjustCount)
		assertEquals(1, fixture.locationProvider.requestCount)
		assertTrue(fixture.locationDao.all.isEmpty())
		// the notification stops saying the watch is connected while the location is saved
		fixture.connectionNotifier.events.first { it == "saving location" }
		answer.complete(Unit)
		val result = session.await()

		val synced = assertIs<BackgroundSessionResult.Synced>(result)
		assertNull(synced.error)
		assertIs<LocationResult.Fix>(synced.location)
		val stored = fixture.locationDao.all.single()
		assertEquals(ADDRESS, stored.watchAddress)
		assertEquals("Test watch", stored.watchName)
		assertEquals(22.2793, stored.latitude)
		assertEquals(114.16281, stored.longitude)
		assertEquals(12.0, stored.accuracyMeters)
		assertTrue(stored.pressedAt in before..Clock.System.now())
		assertEquals(1, watch.disconnectCount)
		assertEquals("saving location", fixture.connectionNotifier.events.last())
	}

	@Test
	fun locationRecordingOffNeverShowsSavingLocation() = runTest {
		val fixture = Fixture()
		fixture.pair()
		val watch = FakeConnectedWatch(connectReason = ConnectReason.MANUAL_SYNC)

		fixture.handler.runBackground(ADDRESS) { watch }

		assertFalse("saving location" in fixture.connectionNotifier.events)
	}

	@Test
	fun manualSyncWithLocationRecordingOffDoesNotReadLocation() = runTest {
		val fixture = Fixture()
		fixture.pair()
		val watch = FakeConnectedWatch(connectReason = ConnectReason.MANUAL_SYNC)

		val result = fixture.handler.runBackground(ADDRESS) { watch }

		assertNull(assertIs<BackgroundSessionResult.Synced>(result).location)
		assertEquals(1, watch.adjustCount)
		assertEquals(0, fixture.locationProvider.requestCount)
		assertTrue(fixture.locationDao.all.isEmpty())
	}

	@Test
	fun autoSyncDoesNotReadLocation() = runTest {
		val fixture = Fixture()
		fixture.pair()
		fixture.enableLocationRecording()
		val watch = FakeConnectedWatch(connectReason = ConnectReason.AUTO_SYNC)

		val result = fixture.handler.runBackground(ADDRESS) { watch }

		assertNull(assertIs<BackgroundSessionResult.Synced>(result).location)
		assertEquals(0, fixture.locationProvider.requestCount)
	}

	@Test
	fun unsupportedPlatformDoesNotReadLocation() = runTest {
		val fixture = Fixture(FakeLocationProvider(isSupported = false))
		fixture.pair()
		fixture.enableLocationRecording()
		val watch = FakeConnectedWatch(connectReason = ConnectReason.MANUAL_SYNC)

		fixture.handler.runBackground(ADDRESS) { watch }

		assertEquals(0, fixture.locationProvider.requestCount)
		assertEquals(1, watch.adjustCount)
	}

	@Test
	fun locationFailureKeepsTheSyncAndStoresNothing() = runTest {
		listOf(
			LocationResult.PermissionDenied,
			LocationResult.Disabled,
			LocationResult.TimedOut,
			LocationResult.Failed(IllegalStateException("no provider")),
		).forEach { failure ->
			val fixture = Fixture(FakeLocationProvider(result = failure))
			fixture.pair()
			fixture.enableLocationRecording()
			val watch = FakeConnectedWatch(connectReason = ConnectReason.MANUAL_SYNC)

			val result = fixture.handler.runBackground(ADDRESS) { watch }

			val synced = assertIs<BackgroundSessionResult.Synced>(result)
			assertNull(synced.error, "$failure")
			assertEquals(failure, synced.location)
			assertNotNull(fixture.repository.find(ADDRESS)?.lastSyncAt, "$failure")
			assertTrue(fixture.locationDao.all.isEmpty(), "$failure")
			assertEquals(1, watch.disconnectCount, "$failure")
		}
	}

	@Test
	fun locationReadThatNeverAnswersTimesOut() = runTest {
		val fixture = Fixture()
		fixture.pair()
		fixture.enableLocationRecording()
		// never completed
		fixture.locationProvider.answer = CompletableDeferred()
		val watch = FakeConnectedWatch(connectReason = ConnectReason.MANUAL_SYNC)

		val result = fixture.handler.runBackground(ADDRESS) { watch }

		assertEquals(LocationResult.TimedOut, assertIs<BackgroundSessionResult.Synced>(result).location)
		assertTrue(fixture.locationProvider.cancelled)
		assertTrue(fixture.locationDao.all.isEmpty())
		// 10 s after the disconnect, the session itself took no time
		assertEquals(10.seconds, testScheduler.currentTime.milliseconds)
	}

	@Test
	fun fixArrivingWithinTenSecondsOfTheDisconnectIsKeptAfterALongSession() = runTest {
		val fixture = Fixture()
		fixture.pair()
		fixture.enableLocationRecording()
		val answer = CompletableDeferred<Unit>()
		fixture.locationProvider.answer = answer
		// the read starts at the press, the session takes 30 s, the fix comes 8 s after it ended
		val watch = FakeConnectedWatch(connectReason = ConnectReason.MANUAL_SYNC).also { it.adjustDuration = 30.seconds }
		launch {
			delay(38.seconds)
			answer.complete(Unit)
		}

		val result = fixture.handler.runBackground(ADDRESS) { watch }

		assertIs<LocationResult.Fix>(assertIs<BackgroundSessionResult.Synced>(result).location)
		assertFalse(fixture.locationProvider.cancelled)
		assertEquals(22.2793, fixture.locationDao.all.single().latitude)
	}

	@Test
	fun withoutAFixTenSecondsAfterTheDisconnectTheLastKnownLocationIsUsed() = runTest {
		val fixture = Fixture(FakeLocationProvider(lastKnown = GeoPoint(51.5, -0.12, accuracyMeters = 30.0)))
		fixture.pair()
		fixture.enableLocationRecording()
		val answer = CompletableDeferred<Unit>()
		fixture.locationProvider.answer = answer
		val watch = FakeConnectedWatch(connectReason = ConnectReason.MANUAL_SYNC).also { it.adjustDuration = 30.seconds }
		// 11 s after the session ended, too late
		launch {
			delay(41.seconds)
			answer.complete(Unit)
		}

		val result = fixture.handler.runBackground(ADDRESS) { watch }

		assertIs<LocationResult.Fix>(assertIs<BackgroundSessionResult.Synced>(result).location)
		assertTrue(fixture.locationProvider.cancelled)
		assertEquals(40.seconds, testScheduler.currentTime.milliseconds)
		val stored = fixture.locationDao.all.single()
		assertEquals(51.5, stored.latitude)
		assertEquals(30.0, stored.accuracyMeters)
	}

	@Test
	fun platformGivingUpFallsBackToTheLastKnownLocation() = runTest {
		val fixture = Fixture(FakeLocationProvider(result = LocationResult.TimedOut, lastKnown = GeoPoint(51.5, -0.12)))
		fixture.pair()
		fixture.enableLocationRecording()
		val watch = FakeConnectedWatch(connectReason = ConnectReason.MANUAL_SYNC)

		val result = fixture.handler.runBackground(ADDRESS) { watch }

		assertIs<LocationResult.Fix>(assertIs<BackgroundSessionResult.Synced>(result).location)
		assertEquals(51.5, fixture.locationDao.all.single().latitude)
	}

	@Test
	fun deniedPermissionDoesNotFallBackToTheLastKnownLocation() = runTest {
		val fixture = Fixture(FakeLocationProvider(result = LocationResult.PermissionDenied, lastKnown = GeoPoint(51.5, -0.12)))
		fixture.pair()
		fixture.enableLocationRecording()
		val watch = FakeConnectedWatch(connectReason = ConnectReason.MANUAL_SYNC)

		val result = fixture.handler.runBackground(ADDRESS) { watch }

		assertEquals(LocationResult.PermissionDenied, assertIs<BackgroundSessionResult.Synced>(result).location)
		assertTrue(fixture.locationDao.all.isEmpty())
	}

	@Test
	fun watchPairedWithAnotherAppRecordsNoLocation() = runTest {
		val fixture = Fixture()
		fixture.pair()
		fixture.enableLocationRecording()
		val watch = FakeConnectedWatch(connectReason = ConnectReason.MANUAL_SYNC, appInfo = OTHER_APP_INFO)

		val result = fixture.handler.runBackground(ADDRESS) { watch }

		assertIs<BackgroundSessionResult.Skipped>(result)
		assertEquals(0, fixture.locationProvider.requestCount)
		assertTrue(fixture.locationDao.all.isEmpty())
	}

	@Test
	fun forgetDeletesLocationsOnlyWhenAsked() = runTest {
		suspend fun Fixture.recordFor(address: String) = PressedLocationRepository(locationDao).record(
			watchAddress = address,
			watchName = "Watch $address",
			pressedAt = PAIRED_AT,
			point = GeoPoint(1.0, 2.0),
		)

		val keeping = Fixture()
		keeping.pair()
		keeping.recordFor(ADDRESS)
		keeping.handler.forget(ADDRESS, deleteLocations = false, connectedWatch = null)
		assertEquals(listOf(ADDRESS), keeping.locationDao.all.map(PressedLocation::watchAddress))

		val deleting = Fixture()
		deleting.pair()
		deleting.recordFor(ADDRESS)
		deleting.recordFor("00:11:22:33:44:55")
		deleting.handler.forget(ADDRESS, deleteLocations = true, connectedWatch = null)
		// another watch's locations stay
		assertEquals(listOf("00:11:22:33:44:55"), deleting.locationDao.all.map(PressedLocation::watchAddress))
	}

	@Test
	fun forgettingAWatchWhileItsLocationIsReadLeavesNoLocation() = runTest {
		listOf(true, false).forEach { deleteLocations ->
			val fixture = Fixture()
			fixture.pair()
			fixture.enableLocationRecording()
			val answer = CompletableDeferred<Unit>()
			fixture.locationProvider.answer = answer
			val watch = FakeConnectedWatch(connectReason = ConnectReason.MANUAL_SYNC)

			val session = async { fixture.handler.runBackground(ADDRESS) { watch } }
			// the watch is let go, the read still runs, and the user forgets the watch meanwhile
			watch.disconnected.await()
			fixture.handler.forget(ADDRESS, deleteLocations = deleteLocations, connectedWatch = null)
			answer.complete(Unit)
			session.await()

			assertTrue(fixture.locationDao.all.isEmpty(), "deleteLocations = $deleteLocations")
		}
	}
}
