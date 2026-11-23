package xyz.d1n0.sync

import com.juul.kable.NotConnectedException
import xyz.d1n0.data.database.SyncFailure
import xyz.d1n0.lib.model.TimeWriteUnansweredException
import xyz.d1n0.lib.model.WatchErrorPacket
import xyz.d1n0.lib.model.WatchTimeoutException
import kotlin.test.Test
import kotlin.test.assertEquals

class SyncFailuresTest {
	@Test
	fun watchErrorKeepsItsBytes() {
		val error = WatchErrorPacket("FF811D".hexToByteArray())
		assertEquals(SyncFailure.WATCH_ERROR, error.syncFailure)
		assertEquals("FF 81 1D", error.syncErrorDetail)
	}

	@Test
	fun timeoutAndLostConnectionHaveTheirOwnKind() {
		assertEquals(SyncFailure.TIMEOUT, WatchTimeoutException("Timeout waiting for clocks initialization").syncFailure)
		assertEquals(SyncFailure.CONNECTION_LOST, NotConnectedException("Disconnected").syncFailure)
	}

	@Test
	fun unansweredTimeWriteIsALostConnection() {
		val error = TimeWriteUnansweredException(IllegalStateException("GATT_ERROR(133)"))
		assertEquals(SyncFailure.CONNECTION_LOST, error.syncFailure)
	}

	@Test
	fun anythingElseKeepsItsMessage() {
		val error = IllegalStateException("write failed")
		assertEquals(SyncFailure.OTHER, error.syncFailure)
		assertEquals("write failed", error.syncErrorDetail)
	}
}
