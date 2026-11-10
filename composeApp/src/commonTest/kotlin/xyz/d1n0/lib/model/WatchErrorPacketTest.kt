package xyz.d1n0.lib.model

import xyz.d1n0.lib.constant.OpCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

@OptIn(ExperimentalStdlibApi::class)
class WatchErrorPacketTest {
	@Test
	fun readsFailedOpCode() {
		assertEquals(OpCode.CLOCK, WatchErrorPacket("FF811D".hexToByteArray()).failedOpCode)
	}

	@Test
	fun formatsPacketAsSpacedHex() {
		assertEquals("FF 81 1D", WatchErrorPacket("FF811D".hexToByteArray()).hex)
	}

	@Test
	fun hasNoFailedOpCodeWhenTooShortOrUnknown() {
		assertNull(WatchErrorPacket("FF81".hexToByteArray()).failedOpCode)
		assertNull(WatchErrorPacket("FF8199".hexToByteArray()).failedOpCode)
	}

	@Test
	fun rejectsOtherPackets() {
		assertFailsWith<IllegalArgumentException> { WatchErrorPacket("1D0001".hexToByteArray()) }
		assertFailsWith<IllegalArgumentException> { WatchErrorPacket(byteArrayOf()) }
	}
}
