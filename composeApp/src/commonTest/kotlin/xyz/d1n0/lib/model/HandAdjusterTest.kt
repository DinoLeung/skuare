package xyz.d1n0.lib.model

import kotlinx.coroutines.test.runTest
import xyz.d1n0.lib.constant.HandGroup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@OptIn(ExperimentalStdlibApi::class)
class HandAdjusterTest {
	private fun hex(value: String) = value.hexToByteArray()

	@Test
	fun runsCapturedClockSequence() = runTest {
		val watch = FakeHandAdjustWatch()
		watch.adjuster().run {
			start()
			select(HandGroup.CLOCK)
			move(HandGroup.CLOCK, 3)
			move(HandGroup.CLOCK, -2)
			move(HandGroup.CLOCK, -1)
			finish()
		}

		// every write of hand_adjust_clock_2100.pcapng, in order
		assertEquals(
			listOf(
				"1a0406000000",
				"1a0412000000", "1a0418000000",
				"1a0418030000", "1a0419000000",
				"1a04186e0800", "1a0419000000",
				"1a04186f0800", "1a0419000000",
				"1a0404000000", "1a0406000000", "1a0407000000",
			),
			watch.written,
		)
	}

	@Test
	fun failsWhenTheWatchDoesNotReply() = runTest {
		val watch = FakeHandAdjustWatch(reply = { null })
		assertFailsWith<IllegalStateException> { watch.adjuster().start() }
	}

	@Test
	fun failsOnErrorReply() = runTest {
		val watch = FakeHandAdjustWatch(reply = { hex("ff811a") })
		assertFailsWith<WatchErrorPacket> { watch.adjuster().select(HandGroup.CLOCK) }
	}

	@Test
	fun ignoresRepliesToOtherOpcodes() = runTest {
		// a clocks reply arriving mid-move isn't the hands stopping
		var moves = 0
		val watch = FakeHandAdjustWatch(reply = {
			if (it[2].toInt() and 0x0F == 0x08 && moves++ == 0) hex("1d000103027f760000ffffffffffff") else null
		})
		assertFailsWith<IllegalStateException> { watch.adjuster().move(HandGroup.CLOCK, 1) }
		// no confirm without the watch's ack
		assertEquals(listOf("1a0418010000"), watch.written)
	}
}
