package xyz.d1n0.lib.model

import xyz.d1n0.lib.constant.TimerStatus
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalStdlibApi::class)
class TimerTest {
	private val classicPacket = "18170F1E00000002".hexToByteArray()
	private val paddedPacket = "18170F1E0000000200000000000000".hexToByteArray()

	@Test
	fun parsesClassicPacket() {
		val timer = Timer.fromPacket(classicPacket)

		assertEquals(23.hours + 15.minutes + 30.seconds, timer.duration)
		assertEquals(TimerStatus.SUSPENDED, timer.status)
	}

	@Test
	fun roundTripsClassicPacket() {
		assertContentEquals(classicPacket, Timer.fromPacket(classicPacket).packet())
	}

	@Test
	fun roundTripsPaddedPacket() {
		assertContentEquals(paddedPacket, Timer.fromPacket(paddedPacket).packet(Timer.PADDED_PACKET_SIZE))
	}

	@Test
	fun buildsCapturedCasioakWrite() {
		// timer_2100.pcapng: the official app sets the GA-B2100's timer to 3 minutes
		val timer = Timer(duration = 3.minutes, status = TimerStatus.NOT_STARTED)
		assertContentEquals(
			"180003000000000000000000000000".hexToByteArray(),
			timer.packet(Timer.PADDED_PACKET_SIZE),
		)
	}

	@Test
	fun rejectsOtherPacketSizes() {
		val timer = Timer(duration = 3.minutes, status = TimerStatus.NOT_STARTED)
		assertFailsWith<IllegalArgumentException> { timer.packet(9) }
	}

	@Test
	fun timerSettingsStoresReplySize() {
		assertEquals(Timer.CLASSIC_PACKET_SIZE, TimerSettings().packetSize)
		assertEquals(Timer.CLASSIC_PACKET_SIZE, TimerSettings().parseTimerPacket(classicPacket).packetSize)
		assertEquals(Timer.PADDED_PACKET_SIZE, TimerSettings().parseTimerPacket(paddedPacket).packetSize)
	}

	@Test
	fun paddedPacketUsesTheStatusFieldBeforePadding() {
		val timer = Timer.fromPacket(paddedPacket)

		assertEquals(23.hours + 15.minutes + 30.seconds, timer.duration)
		assertEquals(TimerStatus.SUSPENDED, timer.status)
	}

	@Test
	fun timerSettingsStoresParsedTimer() {
		val settings = TimerSettings().parseTimerPacket(classicPacket)

		assertEquals(Timer.fromPacket(classicPacket), settings.timer)
	}

	@Test
	fun rejectsDurationOfOneDay() {
		assertFailsWith<IllegalArgumentException> { Timer(1.days, TimerStatus.NOT_STARTED) }
	}

	@Test
	fun rejectsMalformedPackets() {
		assertFailsWith<IllegalArgumentException> {
			Timer.fromPacket(classicPacket.copyOf().also { it[0] = 0 })
		}
		assertFailsWith<IllegalArgumentException> { Timer.fromPacket(classicPacket.copyOf(7)) }
		assertFailsWith<IllegalStateException> {
			Timer.fromPacket(classicPacket.copyOf().also { it[7] = 3 })
		}
	}
}
