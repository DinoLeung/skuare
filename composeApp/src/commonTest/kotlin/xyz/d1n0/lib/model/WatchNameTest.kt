package xyz.d1n0.lib.model

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@OptIn(ExperimentalStdlibApi::class)
class WatchNameTest {
	private val packet = "23434153494F2047572D42353630302300000000".hexToByteArray()

	@Test
	fun parsesNullTerminatedName() {
		assertEquals("CASIO GW-B5600#", WatchName.fromPacket(packet).value)
	}

	@Test
	fun roundTripsNameWithZeroPadding() {
		assertContentEquals(packet, WatchName("CASIO GW-B5600#").packet)
	}

	@Test
	fun supportsMaximumLength() {
		val name = WatchName("1234567890123456789")

		assertEquals(20, name.packet.size)
		assertEquals(name, WatchName.fromPacket(name.packet))
	}

	@Test
	fun rejectsInvalidNameAndPackets() {
		assertFailsWith<IllegalArgumentException> { WatchName("12345678901234567890") }
		assertFailsWith<IllegalArgumentException> { WatchName.fromPacket(packet.copyOf(19)) }
		assertFailsWith<IllegalArgumentException> {
			WatchName.fromPacket(packet.copyOf().also { it[0] = 0 })
		}
	}
}
