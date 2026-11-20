package xyz.d1n0.lib.connection

import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import xyz.d1n0.Log
import xyz.d1n0.sync.WatchConnectionNotifier
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WatchConnectionManagerTest {
	private class RecordingNotifier : WatchConnectionNotifier {
		val events = mutableListOf<String>()
		override fun connectionStarted(name: String?) {
			events += "started"
		}
		override fun connectionNameUpdated(name: String?) = Unit
		override fun timeSyncStarted() = Unit
		override fun timeSyncFinished(name: String?) = Unit
		override fun connectionEnded() {
			events += "ended"
		}
	}

	@BeforeTest
	fun startLog() {
		startKoin { modules(module { single { Log } }) }
	}

	@AfterTest
	fun stopLog() {
		stopKoin()
	}

	@Test
	fun demoEndsTheConnectionItReplaces() = runTest {
		val notifier = RecordingNotifier()
		val manager = WatchConnectionManager(connectionNotifier = notifier, scope = backgroundScope)
		val first = manager.connectDemo()
		// the first watch is awaited until it disconnects, as when the demo replaces a real watch
		runCurrent()

		val second = manager.connectDemo()
		advanceUntilIdle()

		// replace releases the first watch before its wait resumes, so only connectDemo reports its end
		assertEquals(listOf("ended", "ended"), notifier.events)
		assertTrue(second.isDemo)
		assertEquals(second, manager.watch.value)
		assertTrue(first != second)
	}
}
