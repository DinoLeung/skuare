package xyz.d1n0.model

import kotlinx.datetime.LocalDateTime
import xyz.d1n0.lib.constant.BatteryLevel
import xyz.d1n0.lib.model.WatchStatus
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@OptIn(ExperimentalStdlibApi::class)
class WatchStatusTest {
	// captured from the official app, battery level as shown in the official app
	private val twoBars = "26101616240101118f0002001111111111111111111111118a84807e7e7e808184878a8a03000400010000000000000000000000000000000000000011111111111111111111111111111111111180808a7e7b877c7a827b7c8381838b7f7e85000000000000010000000000010000000000000000000000000000000000000000000000000000000000ffff26101605588026101605588026101605548126101605548026101605468103".hexToByteArray()
	private val threeBars = "26101622410101128600000012121212121212121212121286898b8d8984807e7d7e7f810000000000000000000000000000000000000000000000001212121212121212121212121212121212128b807f897e7b867d79827b7b8280828a807d000000000000000000000000000001000000000000000000000000000000000000000000000000000000ffff261016121280261016121280261016121181261016121180260913090881bb".hexToByteArray()
	private val fourBars = "26101622360101138a0000001313131313131313131313138a8c9c928e888482818182840100170032000100010001000300020000000000000000001313131313131313131313131313131313139c8482997e7e89817d857f7f8684858d84810d0001000000060000000000060003000000000000000000000000000000000000000000000000000000ffff26101612078026101612078026101612068126101612068026101611258103".hexToByteArray()

	@Test
	fun parsesBatteryLevel() {
		assertEquals(BatteryLevel.LEVEL_2, WatchStatus.fromPacket(twoBars).batteryLevel)
		assertEquals(BatteryLevel.LEVEL_3, WatchStatus.fromPacket(threeBars).batteryLevel)
		assertEquals(BatteryLevel.LEVEL_4, WatchStatus.fromPacket(fourBars).batteryLevel)
	}

	@Test
	fun parsesTimestamp() {
		assertEquals(LocalDateTime(2026, 10, 16, 22, 41), WatchStatus.fromPacket(threeBars).timestamp)
	}

	@Test
	fun rejectsUnknownBatteryLevel() {
		val packet = threeBars.copyOf().also { it[7] = 0x0A }
		assertFailsWith<IllegalStateException> { WatchStatus.fromPacket(packet) }
	}

	@Test
	fun rejectsTruncatedPacket() {
		assertFailsWith<IllegalArgumentException> { WatchStatus.fromPacket(threeBars.copyOf(20)) }
	}

	@Test
	fun buildsRequestPackets() {
		assertContentEquals("0005ab0000".hexToByteArray(), WatchStatus.requestPacket)
		assertContentEquals("0405ab0000".hexToByteArray(), WatchStatus.endPacket)
	}
}
