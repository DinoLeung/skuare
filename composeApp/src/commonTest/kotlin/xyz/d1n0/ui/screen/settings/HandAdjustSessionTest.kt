package xyz.d1n0.ui.screen.settings

import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import xyz.d1n0.Log
import xyz.d1n0.lib.constant.HandGroup
import xyz.d1n0.lib.model.FakeHandAdjustWatch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalStdlibApi::class)
class HandAdjustSessionTest {
	private val exit = listOf("1a0404000000", "1a0406000000", "1a0407000000")

	private fun TestScope.session(watch: FakeHandAdjustWatch, group: HandGroup = HandGroup.CLOCK) =
		HandAdjustSession(group = group, adjuster = watch.adjuster(), scope = this, log = Log)

	/** Replies like the GA-B2100, except to moves of 1 step or more: those hands never stop. */
	private fun neverStopsMoving(packet: ByteArray): ByteArray? {
		val isMove = packet[2].toInt() and 0x0F == 0x08
		val hasSteps = packet[3].toInt() != 0 || packet[4].toInt() != 0
		return if (isMove && hasSteps) null else FakeHandAdjustWatch.ackLikeGaB2100(packet)
	}

	@Test
	fun unlocksOnceTheHandsReachTheAdjustingPosition() = runTest {
		val watch = FakeHandAdjustWatch()
		val session = session(watch)

		session.start()
		// nothing can be set until the watch says the hands have arrived
		assertFalse(session.state.value.isReady)
		assertTrue(session.state.value.isMoving)

		advanceUntilIdle()
		assertTrue(session.state.value.isReady)
		assertFalse(session.state.value.isMoving)
		assertEquals(listOf("1a0406000000", "1a0412000000", "1a0418000000"), watch.written)
	}

	@Test
	fun setMovesThenConfirms() = runTest {
		val watch = FakeHandAdjustWatch()
		val session = session(watch).apply { start() }
		advanceUntilIdle()

		session.setSteps(-2)
		session.set()
		// everything stays disabled until the hands stop
		assertTrue(session.state.value.isMoving)

		advanceUntilIdle()
		assertFalse(session.state.value.isMoving)
		// -2 is sent as 2160 - 2
		assertEquals(listOf("1a04186e0800", "1a0419000000"), watch.written.takeLast(2))
	}

	@Test
	fun ignoresSetBeforeTheHandsAreReady() = runTest {
		val watch = FakeHandAdjustWatch()
		val session = session(watch)

		session.start()
		session.setSteps(5)
		session.set()
		advanceUntilIdle()

		assertEquals(listOf("1a0406000000", "1a0412000000", "1a0418000000"), watch.written)
	}

	@Test
	fun ignoresSetWhileTheHandsMove() = runTest {
		val watch = FakeHandAdjustWatch(reply = ::neverStopsMoving)
		val session = session(watch).apply { start() }
		advanceUntilIdle()

		session.setSteps(5)
		session.set()
		runCurrent()
		session.setSteps(7)
		session.set()
		runCurrent()

		assertEquals(1, watch.written.count { it.startsWith("1a0418") && it != "1a0418000000" })
	}

	@Test
	fun showsTheErrorAndStopsMoving() = runTest {
		// the watch rejects the move to the adjusting position
		val watch = FakeHandAdjustWatch(reply = {
			if (it[2].toInt() == 0x18) "ff811a".hexToByteArray() else FakeHandAdjustWatch.ackLikeGaB2100(it)
		})
		val session = session(watch).apply { start() }
		advanceUntilIdle()

		assertNotNull(session.state.value.error)
		assertFalse(session.state.value.isMoving)
		assertFalse(session.state.value.isReady)
	}

	@Test
	fun clearsTheErrorOnTheNextSet() = runTest {
		var failNextMove = true
		val watch = FakeHandAdjustWatch(reply = {
			val isStep = it[2].toInt() == 0x18 && it[3].toInt() != 0
			if (isStep && failNextMove) {
				failNextMove = false
				"ff811a".hexToByteArray()
			} else FakeHandAdjustWatch.ackLikeGaB2100(it)
		})
		val session = session(watch).apply { start() }
		advanceUntilIdle()

		session.setSteps(1)
		session.set()
		advanceUntilIdle()
		assertNotNull(session.state.value.error)

		session.setSteps(1)
		session.set()
		assertNull(session.state.value.error)
		advanceUntilIdle()
		assertNull(session.state.value.error)
	}

	@Test
	fun finishSendsTheExitSequence() = runTest {
		val watch = FakeHandAdjustWatch()
		val session = session(watch).apply { start() }
		advanceUntilIdle()

		session.finish()
		advanceUntilIdle()

		assertEquals(exit, watch.written.takeLast(3))
	}

	@Test
	fun finishWhileTheHandsMoveStillSendsTheExitSequence() = runTest {
		val watch = FakeHandAdjustWatch(reply = ::neverStopsMoving)
		val session = session(watch).apply { start() }
		advanceUntilIdle()

		session.setSteps(5)
		session.set()
		advanceTimeBy(1.seconds)
		session.finish()
		advanceUntilIdle()

		// the wait for the hands is dropped, so no confirm, straight to the exit sequence
		assertEquals(listOf("1a0418050000") + exit, watch.written.takeLast(4))
	}

	@Test
	fun finishesOnlyOnce() = runTest {
		val watch = FakeHandAdjustWatch()
		val session = session(watch).apply { start() }
		advanceUntilIdle()

		assertNotNull(session.finish())
		assertNull(session.finish())
		advanceUntilIdle()

		assertEquals(1, watch.written.count { it == "1a0407000000" })
		// and nothing can be set afterwards
		session.setSteps(1)
		session.set()
		advanceUntilIdle()
		assertEquals(exit, watch.written.takeLast(3))
	}

	@Test
	fun buttonsCountTheStepsFromZero() = runTest {
		val session = session(FakeHandAdjustWatch()).apply { start() }
		advanceUntilIdle()

		assertEquals(0, session.state.value.steps)
		session.stepAnticlockwise()
		session.stepAnticlockwise()
		session.stepClockwise()
		assertEquals(-1, session.state.value.steps)
	}

	@Test
	fun stepsStopOneShortOfTheModulus() = runTest {
		val session = session(FakeHandAdjustWatch(), HandGroup.MODE).apply { start() }
		advanceUntilIdle()

		session.setSteps(179)
		assertFalse(session.state.value.canStepClockwise)
		session.stepClockwise()
		assertEquals(179, session.state.value.steps)

		session.setSteps(-179)
		assertFalse(session.state.value.canStepAnticlockwise)
		session.stepAnticlockwise()
		assertEquals(-179, session.state.value.steps)
	}

	@Test
	fun cannotSetZeroOrOutOfRangeSteps() = runTest {
		val session = session(FakeHandAdjustWatch(), HandGroup.MODE).apply { start() }
		advanceUntilIdle()

		// 0 would be the move to the adjusting position
		assertFalse(session.state.value.canSet)
		// a typed value out of range is kept for the field, but can't be sent
		session.setSteps(180)
		assertEquals(180, session.state.value.steps)
		assertFalse(session.state.value.isStepsValid)
		assertFalse(session.state.value.canSet)

		session.setSteps(-90)
		assertTrue(session.state.value.canSet)
	}

	@Test
	fun stepsCannotChangeBeforeTheHandsAreReadyOrWhileTheyMove() = runTest {
		val session = session(FakeHandAdjustWatch(reply = ::neverStopsMoving)).apply { start() }
		session.stepClockwise()
		session.setSteps(4)
		assertEquals(0, session.state.value.steps)

		advanceUntilIdle()
		session.setSteps(4)
		session.set()
		runCurrent()
		session.stepClockwise()
		session.setSteps(9)
		assertEquals(4, session.state.value.steps)
		assertFalse(session.state.value.canFinish)
	}

	@Test
	fun stepsGoBackToZeroOnceTheHandsStop() = runTest {
		val session = session(FakeHandAdjustWatch()).apply { start() }
		advanceUntilIdle()

		session.setSteps(15)
		session.set()
		// kept while moving
		assertEquals(15, session.state.value.steps)
		advanceUntilIdle()
		assertEquals(0, session.state.value.steps)
	}

	@Test
	fun stepsAreKeptWhenTheMoveFails() = runTest {
		val watch = FakeHandAdjustWatch(reply = {
			val isStep = it[2].toInt() == 0x18 && it[3].toInt() != 0
			if (isStep) "ff811a".hexToByteArray() else FakeHandAdjustWatch.ackLikeGaB2100(it)
		})
		val session = session(watch).apply { start() }
		advanceUntilIdle()

		session.setSteps(15)
		session.set()
		advanceUntilIdle()

		assertNotNull(session.state.value.error)
		assertEquals(15, session.state.value.steps)
		assertTrue(session.state.value.canSet)
	}

	@Test
	fun sendsTheModeHandsCommands() = runTest {
		val watch = FakeHandAdjustWatch()
		val session = session(watch, HandGroup.MODE).apply { start() }
		advanceUntilIdle()
		session.setSteps(-2)
		session.set()
		advanceUntilIdle()

		assertEquals(
			listOf("1a0406000000", "1a0422000000", "1a0428000000", "1a0428b20000", "1a0429000000"),
			watch.written,
		)
	}
}
