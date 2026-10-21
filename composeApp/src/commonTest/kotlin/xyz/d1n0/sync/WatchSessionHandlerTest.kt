package xyz.d1n0.sync

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import xyz.d1n0.data.database.AppConfig
import xyz.d1n0.data.database.AppConfigDao
import xyz.d1n0.data.database.PairedWatch
import xyz.d1n0.data.database.PairedWatchDao
import xyz.d1n0.data.repository.InstallationRepository
import xyz.d1n0.data.repository.PairedWatchRepository
import xyz.d1n0.lib.connection.WatchConnectionManager
import xyz.d1n0.lib.constant.ConnectReason
import xyz.d1n0.lib.constant.OpCode
import xyz.d1n0.lib.model.AppInfo
import xyz.d1n0.lib.model.WatchInfo
import xyz.d1n0.lib.model.WatchName
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
	}

	private class FakeAppConfigDao(appInfo: AppInfo) : AppConfigDao {
		private var config: AppConfig? = AppConfig(installationId = appInfo.id)

		override suspend fun get(): AppConfig? = config

		override suspend fun insertIfMissing(config: AppConfig) {
			if (this.config == null) this.config = config
		}
	}

	private class FakePairedWatchDao : PairedWatchDao {
		private val watches = MutableStateFlow<List<PairedWatch>>(emptyList())

		override fun observeAll(): Flow<List<PairedWatch>> = watches

		override suspend fun find(address: String): PairedWatch? =
			watches.value.firstOrNull { it.address == address }

		override suspend fun upsert(watch: PairedWatch) {
			watches.update { current -> current.filterNot { it.address == watch.address } + watch }
		}

		override suspend fun updateLastSync(address: String, at: Instant, error: String?) {
			watches.update { current ->
				current.map { watch ->
					if (watch.address == address) watch.copy(lastSyncAt = at, lastSyncError = error) else watch
				}
			}
		}

		override suspend fun updateDisplayName(address: String, displayName: String) {
			watches.update { current ->
				current.map { watch ->
					if (watch.address == address) watch.copy(displayName = displayName) else watch
				}
			}
		}

		override suspend fun delete(address: String) {
			watches.update { current -> current.filterNot { it.address == address } }
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

		override fun connectionStarted(advertisedName: String?) {
			events += "connected:$advertisedName"
		}

		override fun timeSyncStarted() {
			events += "syncing"
		}

		override fun timeSyncFinished(advertisedName: String?) {
			events += "connected:$advertisedName"
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
	) : ConnectedWatch {
		private val mutableInfo = MutableStateFlow(
			WatchInfo(address = address, connectReason = connectReason),
		)
		override val info: StateFlow<WatchInfo> = mutableInfo

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
		): PairedWatch = PairedWatch(
			address = address,
			advertisedName = advertisedName,
			displayName = displayName,
			pairedAt = PAIRED_AT,
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
			listOf("syncing", "connected:CASIO GMW-B5000#"),
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
	fun backgroundReturnsBusyWithoutStartingSession() = runTest {
		val fixture = Fixture()
		var storedName: String? = "not called"

		val result = fixture.handler.runBackground(ADDRESS) {
			storedName = it
			null
		}

		assertEquals(BackgroundSessionResult.Busy, result)
		assertNull(storedName)
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
		var receivedStoredName: String? = null

		val result = fixture.handler.runBackground(ADDRESS) { storedName ->
			receivedStoredName = storedName
			watch
		}

		val synced = assertIs<BackgroundSessionResult.Synced>(result)
		assertNull(synced.error)
		assertEquals("CASIO GMW-B5000#", receivedStoredName)
		assertEquals(1, watch.adjustCount)
		assertEquals(1, watch.disconnectCount)
		val stored = assertNotNull(fixture.repository.find(ADDRESS))
		assertEquals("New name", stored.displayName)
		assertNotNull(stored.lastSyncAt)
		assertNull(stored.lastSyncError)
	}

	@Test
	fun failedTimeAdjustmentIsReturnedAndRecorded() = runTest {
		val fixture = Fixture()
		fixture.pair()
		val failure = IllegalStateException("write failed")
		val watch = FakeConnectedWatch(
			connectReason = ConnectReason.MANUAL_SYNC,
			adjustResult = Result.failure(failure),
		)

		val result = fixture.handler.runBackground(ADDRESS) { watch }

		val synced = assertIs<BackgroundSessionResult.Synced>(result)
		assertTrue(synced.error === failure)
		assertEquals("write failed", fixture.repository.find(ADDRESS)?.lastSyncError)
		assertEquals(1, watch.disconnectCount)
		assertEquals(
			listOf("syncing", "connected:CASIO GMW-B5000#"),
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
