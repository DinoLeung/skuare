package xyz.d1n0.lib.model

import com.juul.kable.NotConnectedException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class TimeWriteFailureTest {
	// what Kable threw for the unanswered time write on the older GMW-B5000, 2026-11-22
	private val gattError = IllegalStateException(
		"OnCharacteristicWrite(characteristic=26eb002d-b012-49a8-b1f8-394fb2032b0f, status=GATT_ERROR(133))",
	)

	@Test
	fun failedWriteFollowedByDisconnectIsUnanswered() = runTest {
		// Android reported the disconnect in the same millisecond as the failed write
		val error = timeWriteFailure(gattError) { delay(1.milliseconds) }

		val unanswered = assertIs<TimeWriteUnansweredException>(error)
		assertSame(gattError, unanswered.cause)
	}

	@Test
	fun failedWriteOnAConnectionThatStaysIsThrownAsItIs() = runTest {
		assertSame(gattError, timeWriteFailure(gattError) { awaitCancellation() })
	}

	@Test
	fun disconnectLongAfterTheFailureDoesNotCount() = runTest {
		assertSame(gattError, timeWriteFailure(gattError) { delay(2.seconds) })
	}

	@Test
	fun linkGoneBeforeTheWriteIsNotUnanswered() = runTest {
		// Kable throws this before sending when the link is already down, the watch is disconnected
		val notConnected = NotConnectedException("Disconnected")
		assertSame(notConnected, timeWriteFailure(notConnected) {})
	}
}
