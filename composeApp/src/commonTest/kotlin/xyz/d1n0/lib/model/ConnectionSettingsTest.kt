package xyz.d1n0.lib.model

import xyz.d1n0.lib.constant.AutoSyncDelay
import xyz.d1n0.lib.constant.ConnectionTimeout
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalStdlibApi::class)
class ConnectionSettingsTest {
	private val enabledPacket = "110F0F0F0600500004000100002003".hexToByteArray()

	@Test
	fun parsesEnabledSettings() {
		val settings = ConnectionSettings.fromPacket(enabledPacket)

		assertTrue(settings.autoSyncEnable)
		assertEquals(AutoSyncDelay(32), settings.autoSyncDelay)
		assertEquals(ConnectionTimeout.MINUTES_3, settings.connectionTimeout)
	}

	@Test
	fun roundTripsCapturedPacket() {
		assertContentEquals(enabledPacket, ConnectionSettings.fromPacket(enabledPacket).packet)
	}

	@Test
	fun writesDisabledSettings() {
		val settings = ConnectionSettings(
			autoSyncEnable = false,
			autoSyncDelay = AutoSyncDelay(59),
			connectionTimeout = ConnectionTimeout.MINUTES_10,
		)
		val packet = settings.packet

		assertEquals(0x80.toByte(), packet[12])
		assertEquals(59.toByte(), packet[13])
		assertEquals(10.toByte(), packet[14])
		assertFalse(ConnectionSettings.fromPacket(packet).autoSyncEnable)
	}

	@Test
	fun validatesDelayRange() {
		assertEquals(0.toByte(), AutoSyncDelay(0).byte)
		assertEquals(59.toByte(), AutoSyncDelay.fromByte(59).byte)
		assertFailsWith<IllegalArgumentException> { AutoSyncDelay(-1) }
		assertFailsWith<IllegalArgumentException> { AutoSyncDelay(60) }
	}

	@Test
	fun rejectsMalformedPackets() {
		assertFailsWith<IllegalArgumentException> {
			ConnectionSettings.fromPacket(enabledPacket.copyOf().also { it[0] = 0 })
		}
		assertFailsWith<IllegalArgumentException> {
			ConnectionSettings.fromPacket(enabledPacket.copyOf(14))
		}
		assertFailsWith<IllegalArgumentException> {
			ConnectionSettings.fromPacket(enabledPacket.copyOf().also { it[13] = 60 })
		}
		assertFailsWith<IllegalStateException> {
			ConnectionSettings.fromPacket(enabledPacket.copyOf().also { it[14] = 4 })
		}
	}
}
