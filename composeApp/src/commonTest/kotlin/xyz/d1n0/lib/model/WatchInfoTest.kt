package xyz.d1n0.model

import xyz.d1n0.lib.constant.ConnectReason
import xyz.d1n0.lib.model.WatchInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@OptIn(ExperimentalStdlibApi::class)
class WatchInfoTest {
	// captured from the official app, with the address the phone connected to
	private val connectReasons = mapOf(
		"1040a2d5afa1ed7f01030fffffffff27000000" to "ED:A1:AF:D5:A2:40", // GMW-B5000 3539
		"10239b50d4f0d77f01030fffffffff27000000" to "D7:F0:D4:50:9B:23", // GMW-B5000 3459
		"102694509070d87f01030fffffffff27000000" to "D8:70:90:50:94:26", // GW-B5600 3540
		"1045463914bafd7f01030fffffffff27000000" to "FD:BA:14:39:46:45", // GW-BX5600 3578
	)

	@Test
	fun parsesAddress() {
		connectReasons.forEach { (packet, address) ->
			assertEquals(address, WatchInfo().parseConnectReasonPacket(packet.hexToByteArray()).address)
		}
	}

	@Test
	fun parsesConnectReason() {
		val packet = connectReasons.keys.first().hexToByteArray()
		assertEquals(ConnectReason.DEFAULT, WatchInfo().parseConnectReasonPacket(packet).connectReason)
		packet[8] = ConnectReason.AUTO_SYNC.byte
		assertEquals(ConnectReason.AUTO_SYNC, WatchInfo().parseConnectReasonPacket(packet).connectReason)
	}

	@Test
	fun rejectsInvalidPackets() {
		val packet = connectReasons.keys.first().hexToByteArray()
		assertFailsWith<IllegalArgumentException> { WatchInfo().parseConnectReasonPacket(packet.copyOf(18)) }
		assertFailsWith<IllegalArgumentException> {
			WatchInfo().parseConnectReasonPacket(packet.copyOf().also { it[0] = 0x22 })
		}
	}
}
