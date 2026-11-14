package xyz.d1n0.lib.model

import xyz.d1n0.lib.constant.HandGroup
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith

@OptIn(ExperimentalStdlibApi::class)
class HandAdjustTest {
	private fun hex(value: String) = value.hexToByteArray()

	@Test
	fun buildsCapturedClockPackets() {
		// hand_adjust_clock_2100.pcapng
		assertContentEquals(hex("1a0406000000"), HandAdjust.openPacket)
		assertContentEquals(hex("1a0412000000"), HandAdjust.selectPacket(HandGroup.CLOCK))
		assertContentEquals(hex("1a0418000000"), HandAdjust.adjustPositionPacket(HandGroup.CLOCK))
		assertContentEquals(hex("1a0418030000"), HandAdjust.movePacket(HandGroup.CLOCK, 3))
		assertContentEquals(hex("1a04186e0800"), HandAdjust.movePacket(HandGroup.CLOCK, -2))
		assertContentEquals(hex("1a04186f0800"), HandAdjust.movePacket(HandGroup.CLOCK, -1))
		assertContentEquals(hex("1a0419000000"), HandAdjust.confirmPacket(HandGroup.CLOCK))
		assertContentEquals(hex("1a0404000000"), HandAdjust.savePacket)
		assertContentEquals(hex("1a0407000000"), HandAdjust.closePacket)
	}

	@Test
	fun buildsCapturedFunctionPackets() {
		// hand_adjust_function_2100.pcapng
		assertContentEquals(hex("1a0422000000"), HandAdjust.selectPacket(HandGroup.FUNCTION))
		assertContentEquals(hex("1a0428000000"), HandAdjust.adjustPositionPacket(HandGroup.FUNCTION))
		assertContentEquals(hex("1a0428030000"), HandAdjust.movePacket(HandGroup.FUNCTION, 3))
		assertContentEquals(hex("1a0428b20000"), HandAdjust.movePacket(HandGroup.FUNCTION, -2))
		assertContentEquals(hex("1a0428b30000"), HandAdjust.movePacket(HandGroup.FUNCTION, -1))
		assertContentEquals(hex("1a0429000000"), HandAdjust.confirmPacket(HandGroup.FUNCTION))
	}

	@Test
	fun rejectsMovesSentAsZero() {
		// 0 and whole revolutions would be the move to the adjusting position
		listOf(0, 2160, -2160, 2200).forEach { steps ->
			assertFailsWith<IllegalArgumentException>("$steps") { HandAdjust.movePacket(HandGroup.CLOCK, steps) }
		}
		assertFailsWith<IllegalArgumentException> { HandAdjust.movePacket(HandGroup.FUNCTION, 180) }
	}

	@Test
	fun acceptsMovesUpToOneStepShortOfARevolution() {
		assertContentEquals(hex("1a04186f0800"), HandAdjust.movePacket(HandGroup.CLOCK, 2159))
		assertContentEquals(hex("1a0418010000"), HandAdjust.movePacket(HandGroup.CLOCK, -2159))
	}
}
