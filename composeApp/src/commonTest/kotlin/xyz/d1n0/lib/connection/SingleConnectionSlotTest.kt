package xyz.d1n0.lib.connection

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SingleConnectionSlotTest {
	private class FakeConnection {
		val disconnected = CompletableDeferred<Unit>()
		var disconnectCalls = 0
		var closeCalls = 0
	}

	private fun kotlinx.coroutines.test.TestScope.slot(onDisconnected: () -> Unit = {}) =
		SingleConnectionSlot<FakeConnection>(
			scope = backgroundScope,
			disconnect = { it.disconnectCalls++ },
			awaitDisconnected = { it.disconnected.await() },
			close = { it.closeCalls++ },
			onDisconnected = onDisconnected,
		)

	@Test
	fun requireValueFailsWhenEmpty() = runTest {
		assertFailsWith<IllegalStateException> { slot().requireValue() }
	}

	@Test
	fun replacePublishesConnectedValue() = runTest {
		val connection = FakeConnection()
		val slot = slot()

		val result = slot.replace { connection }

		assertTrue(result === connection)
		assertTrue(slot.value.value === connection)
		assertTrue(slot.requireValue() === connection)
	}

	@Test
	fun replaceDisconnectsAndClosesPreviousValue() = runTest {
		val first = FakeConnection()
		val second = FakeConnection()
		val slot = slot()
		slot.replace { first }

		slot.replace { second }

		assertEquals(1, first.disconnectCalls)
		assertEquals(1, first.closeCalls)
		assertTrue(slot.value.value === second)
	}

	@Test
	fun connectIfIdleRejectsSecondConnectionWithoutCreatingIt() = runTest {
		val first = FakeConnection()
		val slot = slot()
		var busyCalls = 0
		var connectCalls = 0
		slot.connectIfIdle(onBusy = {}, connect = { first })

		val result = slot.connectIfIdle(
			onBusy = { busyCalls++ },
			connect = {
				connectCalls++
				FakeConnection()
			},
		)

		assertNull(result)
		assertEquals(1, busyCalls)
		assertEquals(0, connectCalls)
		assertTrue(slot.value.value === first)
	}

	@Test
	fun observedDisconnectionClearsAndClosesValue() = runTest {
		val connection = FakeConnection()
		var observed = 0
		val slot = slot { observed++ }
		slot.replace { connection }

		connection.disconnected.complete(Unit)
		runCurrent()

		assertNull(slot.value.value)
		assertEquals(1, connection.closeCalls)
		assertEquals(1, observed)
	}

	@Test
	fun alreadyDisconnectedValueIsReleasedWithoutWaitingForAnotherState() = runTest {
		val connection = FakeConnection().also { it.disconnected.complete(Unit) }
		val slot = slot()

		slot.replace { connection }
		runCurrent()

		assertNull(slot.value.value)
		assertEquals(1, connection.closeCalls)
	}

	@Test
	fun disconnectDelegatesToCurrentValue() = runTest {
		val connection = FakeConnection()
		val slot = slot()
		slot.replace { connection }

		slot.disconnect()

		assertEquals(1, connection.disconnectCalls)
	}

	@Test
	fun failedConnectionLeavesSlotEmpty() = runTest {
		val slot = slot()
		val failure = IllegalStateException("connect failed")

		val thrown = assertFailsWith<IllegalStateException> {
			slot.replace { throw failure }
		}

		assertTrue(thrown === failure)
		assertNull(slot.value.value)
	}
}
