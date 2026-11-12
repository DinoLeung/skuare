package xyz.d1n0.lib.model

import xyz.d1n0.lib.constant.OpCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@OptIn(ExperimentalStdlibApi::class)
class BatchPacketTest {
	// GW-BX5600 connect sync (connect_time_sync_bx.pcapng), frames 208, 213 and 214

	private val clocksRequest = "051D001D00240024012402"

	// 05 | 0F00 1D00… | 0F00 1D02… | 1400 2400… | 1400 2401… | 1400 2402…
	private val clocksReply = (
		"050f001d000107067f767a00ffffffffffff0f001d0203030005000000ffffffffffff1400240001c04170ddf6c0" +
			"bc1d4061561ba908a26600140024010100000000000000000000000000000000031400240201c04176dd0529" +
			"34ad40615332df505d1000"
		).hexToByteArray()

	// 02 | 0F00 1D00… | 0F00 1D02…, the unused 4th slot written as FFFF
	private val clocksWrite =
		"020F001D000107067F767A00FFFFFFFFFFFF0F001D020303000500FFFFFFFFFFFFFFFF"

	private fun ByteArray.hex() = toHexString(HexFormat.UpperCase)

	@Test
	fun encodesRequestsAsOpcodeAndIndex() {
		val request = BatchPacket.request(
			listOf(
				OpCode.CLOCK to 0,
				OpCode.CLOCK to 0,
				OpCode.TIMEZONE_LOCATION_RADIO_ID to 0,
				OpCode.TIMEZONE_LOCATION_RADIO_ID to 1,
				OpCode.TIMEZONE_LOCATION_RADIO_ID to 2,
			)
		)

		assertEquals(clocksRequest, request.hex())
	}

	@Test
	fun decodesRecordsInOrder() {
		val records = BatchPacket.decode(clocksReply)

		assertEquals(listOf(15, 15, 20, 20, 20), records.map { it.size })
		assertEquals(
			listOf("1D00", "1D02", "2400", "2401", "2402"),
			records.map { it.sliceArray(0..1).hex() },
		)
	}

	@Test
	fun encodeIsTheInverseOfDecode() {
		assertEquals(clocksReply.hex(), BatchPacket.encode(BatchPacket.decode(clocksReply)).hex())
	}

	@Test
	fun writesBackTheClocksItReadLikeTheOfficialApp() {
		val clocks = BatchPacket.decode(clocksReply)
			.filter { it.first() == OpCode.CLOCK.byte }
			.fold(ClocksSettings.empty(3)) { clocks, record -> clocks.parseClocksPacket(record) }

		assertEquals(clocksWrite, BatchPacket.encode(clocks.clocksPackets).hex())
	}

	@Test
	fun rejectsABatchCutShort() {
		assertFailsWith<IllegalArgumentException> { BatchPacket.decode(clocksReply.copyOf(40)) }
		assertFailsWith<IllegalArgumentException> { BatchPacket.decode("05".hexToByteArray()) }
	}

	@Test
	fun rejectsBytesAfterTheLastRecord() {
		assertFailsWith<IllegalArgumentException> { BatchPacket.decode(clocksReply + byteArrayOf(0)) }
	}

	@Test
	fun rejectsEmptyBatches() {
		assertFailsWith<IllegalArgumentException> { BatchPacket.request(emptyList()) }
		assertFailsWith<IllegalArgumentException> { BatchPacket.encode(emptyList()) }
		assertFailsWith<IllegalArgumentException> { BatchPacket.decode(ByteArray(0)) }
	}
}
