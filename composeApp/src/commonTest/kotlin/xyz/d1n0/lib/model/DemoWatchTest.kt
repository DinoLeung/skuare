package xyz.d1n0.lib.model

import com.juul.kable.State
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.runTest
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import xyz.d1n0.Log
import xyz.d1n0.lib.constant.HandGroup
import xyz.d1n0.lib.constant.TimerStatus
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

class DemoWatchTest {
	@BeforeTest
	fun startLog() {
		startKoin { modules(module { single { Log } }) }
	}

	@AfterTest
	fun stopLog() {
		stopKoin()
	}

	private suspend fun connectedDemo() = Watch.demo().also { it.connect() }

	@Test
	fun isConnectedOnlyAfterConnect() = runTest {
		val watch = Watch.demo()
		assertIs<State.Disconnected>(watch.state.value)

		watch.connect()

		assertTrue(watch.isDemo)
		assertIs<State.Connected>(watch.state.value)
		assertFalse(watch.hasBatchCharacteristics)
		assertEquals(DemoWatch.model, watch.model)
	}

	@Test
	fun answersEveryScreensFirstRequest() = runTest {
		val watch = connectedDemo()

		assertTrue(watch.readClocks().getOrThrow().isInitialized)
		watch.requestAlarms()
		watch.requestTimer()
		watch.requestReminderTitles()
		watch.requestReminderConfigs()
		watch.requestName()
		watch.requestWatchSettings()
		watch.requestConnectionSettings()

		assertEquals(6, watch.clocks.value.clockCount)
		assertEquals("Australia/Adelaide", assertIs<HomeClock>(watch.clocks.value.pinnedClock).timeZone.timeZone)
		assertEquals("Asia/Hong_Kong", assertIs<WorldClock>(watch.clocks.value.clocks[1]).timeZone.timeZone)
		assertTrue(watch.alarms.value.isInitialized)
		assertNotNull(watch.timer.value.timer)
		assertEquals(ReminderTitle("MEETING"), watch.reminders.value.reminderTitles[0])
		assertTrue(watch.reminders.value.reminderTitles.all { it != null })
		assertTrue(watch.reminders.value.reminderConfigs.all { it != null })
		assertEquals("CASIO GW-B5600#", watch.info.value.name?.value)
		assertNotNull(watch.info.value.watchSettings)
		assertNotNull(watch.info.value.connectionSettings)
	}

	@Test
	fun savedClocksAreReadBack() = runTest {
		val watch = connectedDemo()
		val swapped = watch.readClocks().getOrThrow().pin(1)

		val saved = watch.writeClocksTimezones(swapped).getOrThrow()

		assertEquals(swapped, saved)
		assertEquals(swapped, watch.clocks.value)
	}

	@Test
	fun savedTimerIsReadBack() = runTest {
		val watch = connectedDemo()
		watch.requestTimer()

		watch.writeTimer(Timer(duration = 3.minutes, status = TimerStatus.NOT_STARTED))

		assertEquals(3.minutes, watch.timer.value.timer?.duration)
	}

	@Test
	fun savedNameIsReadBack() = runTest {
		val watch = connectedDemo()

		watch.writeName(WatchName("MY WATCH"))

		assertEquals("MY WATCH", watch.info.value.name?.value)
	}

	@Test
	fun fewerClocksAreReadAfterSwitchingModel() = runTest {
		val watch = connectedDemo()
		watch.useCustomModel(DemoWatch.model.copy(clockCount = 3))

		val clocks = watch.readClocks().getOrThrow()

		assertTrue(clocks.isInitialized)
		assertEquals(3, clocks.clockCount)
	}

	@Test
	fun adjustsHandsWithoutWaiting() = runTest {
		val watch = connectedDemo()

		watch.handAdjuster().run {
			start()
			select(HandGroup.CLOCK)
			move(HandGroup.CLOCK, 3)
			finish()
		}
	}

	@Test
	fun watchStatusFailsWithoutThrowing() = runTest {
		val watch = connectedDemo()

		assertTrue(watch.requestWatchStatus().isFailure)
	}

	@Test
	fun disconnectsAndCloses() = runTest {
		val watch = connectedDemo()

		watch.disconnect()
		assertIs<State.Disconnected>(watch.state.value)

		watch.close()
		assertFalse(watch.scope.isActive)
	}
}
