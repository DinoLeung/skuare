package xyz.d1n0.sync

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import xyz.d1n0.data.database.AppConfig
import xyz.d1n0.data.database.AppConfigDao
import xyz.d1n0.data.database.FakePairedWatchDao
import xyz.d1n0.data.database.PairedWatch
import xyz.d1n0.data.repository.InstallationRepository
import xyz.d1n0.data.repository.PairedWatchRepository
import xyz.d1n0.lib.connection.WatchConnectionManager
import xyz.d1n0.lib.constant.ConnectReason
import xyz.d1n0.lib.constant.OpCode
import xyz.d1n0.lib.model.AppInfo
import xyz.d1n0.lib.model.CustomWatchModel
import xyz.d1n0.lib.model.WatchFeatures
import xyz.d1n0.lib.model.WatchInfo
import xyz.d1n0.lib.model.WatchModel
import xyz.d1n0.lib.model.WatchName
import xyz.d1n0.lib.model.resolveWatchModel
import xyz.d1n0.lib.model.shouldSwitchToCustomModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class WatchSessionHandlerTest {
	private companion object {
		const val ADDRESS = "ED:A1:AF:D5:A2:40"
		val OUR_APP_INFO = AppInfo(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10))
		val OTHER_APP_INFO = AppInfo(byteArrayOf(10, 9, 8, 7, 6, 5, 4, 3, 2, 1))
		val PAIRED_AT = Instant.parse("2026-01-01T00:00:00Z")
		const val UNKNOWN_NAME = "CASIO GBD-H2000"
		val ADJUSTED_MODEL = CustomWatchModel(clockCount = 4, reminderCount = 5, hasWeekdayLanguage = true)
	}

	private class FakeAppConfigDao(appInfo: AppInfo) : AppConfigDao {
		private var config: AppConfig? = AppConfig(installationId = appInfo.id)

		override suspend fun get(): AppConfig? = config

		override suspend fun insertIfMissing(config: AppConfig) {
			if (this.config == null) this.config = config
		}
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

		override suspend fun adjustTime(): Result<Unit> {
			adjustCount++
			modelAtAdjust = model
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

	private class Fixture {
		val dao = FakePairedWatchDao()
		val repository = PairedWatchRepository(dao)
		val presenceMonitor = FakePresenceMonitor()
		val connectionNotifier = FakeConnectionNotifier()
		val handler = WatchSessionHandler(
			connectionManager = WatchConnectionManager(),
			pairedWatchRepository = repository,
			installationRepository = InstallationRepository(FakeAppConfigDao(OUR_APP_INFO)),
			presenceMonitor = presenceMonitor,
			connectionNotifier = connectionNotifier,
		)

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
		assertNull(stored.lastSyncError)
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
		assertNull(stored.lastSyncError)
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
		assertEquals("write failed", fixture.repository.find(ADDRESS)?.lastSyncError)
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
	fun findRequestIsIgnoredAndDisconnected() = runTest {
		val fixture = Fixture()
		fixture.pair()
		val watch = FakeConnectedWatch(connectReason = ConnectReason.FIND)

		val result = fixture.handler.runBackground(ADDRESS) { watch }

		assertEquals(BackgroundSessionResult.Ignored, result)
		assertEquals(0, watch.adjustCount)
		assertEquals(1, watch.disconnectCount)
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

		fixture.handler.forget(ADDRESS, watch)

		assertEquals(listOf(paired), fixture.presenceMonitor.unregistered)
		assertNull(fixture.repository.find(ADDRESS))
		assertEquals(1, watch.disconnectCount)
	}

	@Test
	fun forgetDoesNotDisconnectDifferentWatch() = runTest {
		val fixture = Fixture()
		fixture.pair()
		val other = FakeConnectedWatch(address = "00:11:22:33:44:55")

		fixture.handler.forget(ADDRESS, other)

		assertEquals(0, other.disconnectCount)
		assertFalse(fixture.presenceMonitor.unregistered.isEmpty())
	}
}
