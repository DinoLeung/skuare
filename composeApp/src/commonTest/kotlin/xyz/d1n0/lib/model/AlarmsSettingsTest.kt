package xyz.d1n0.lib.model

import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalStdlibApi::class)
class AlarmsSettingsTest {
	private val alarmAPacket = "15C0000C1E".hexToByteArray()
	private val alarmBPacket = "160000011E40000E0F00000F2D4000173B".hexToByteArray()

	@Test
	fun alarmRoundTrips() {
		val alarm = Alarm.fromBytes("4000173B".hexToByteArray())

		assertTrue(alarm.enable)
		assertEquals(LocalTime(23, 59), alarm.time)
		assertContentEquals("4000173B".hexToByteArray(), alarm.bytes)
	}

	@Test
	fun disabledAlarmWritesNoEnableBit() {
		val alarm = Alarm(enable = false, time = LocalTime(1, 30))

		assertContentEquals("0000011E".hexToByteArray(), alarm.bytes)
		assertFalse(Alarm.fromBytes(alarm.bytes).enable)
	}

	@Test
	fun hourlySignalUsesItsOwnBit() {
		assertTrue(HourlySignal.fromByte(0xC0.toByte()).enable)
		assertFalse(HourlySignal.fromByte(0x40).enable)
		assertEquals(0x80.toByte(), HourlySignal(enable = true).byte)
		assertEquals(0x00.toByte(), HourlySignal(enable = false).byte)
	}

	@Test
	fun parsesBothAlarmPackets() {
		val settings = AlarmsSettings()
			.parseAlarmAPacket(alarmAPacket)
			.parseAlarmBPacket(alarmBPacket)

		assertTrue(settings.isInitialized)
		assertEquals(HourlySignal(enable = true), settings.hourlySignal)
		assertEquals(
			listOf(
				Alarm(enable = true, time = LocalTime(12, 30)),
				Alarm(enable = false, time = LocalTime(1, 30)),
				Alarm(enable = true, time = LocalTime(14, 15)),
				Alarm(enable = false, time = LocalTime(15, 45)),
			),
			settings.alarms,
		)
		assertEquals(Alarm(enable = true, time = LocalTime(23, 59)), settings.snoozeAlarm)
	}

	@Test
	fun parsedSettingsWriteCapturedPackets() {
		val settings = AlarmsSettings()
			.parseAlarmAPacket(alarmAPacket)
			.parseAlarmBPacket(alarmBPacket)

		assertContentEquals(alarmAPacket, settings.alarmAPacket)
		assertContentEquals(alarmBPacket, settings.alarmBPacket)
	}

	@Test
	fun incompleteSettingsCannotBuildPackets() {
		assertFailsWith<IllegalArgumentException> { AlarmsSettings().alarmAPacket }
		assertFailsWith<IllegalArgumentException> { AlarmsSettings().alarmBPacket }
	}

	@Test
	fun rejectsMalformedPackets() {
		assertFailsWith<IllegalArgumentException> { Alarm.fromBytes(ByteArray(3)) }
		assertFailsWith<IllegalArgumentException> {
			AlarmsSettings().parseAlarmAPacket(alarmAPacket.copyOf().also { it[0] = 0 })
		}
		assertFailsWith<IllegalArgumentException> {
			AlarmsSettings().parseAlarmAPacket(alarmAPacket.copyOf(4))
		}
		assertFailsWith<IllegalArgumentException> {
			AlarmsSettings().parseAlarmBPacket(alarmBPacket.copyOf().also { it[0] = 0 })
		}
		assertFailsWith<IllegalArgumentException> {
			AlarmsSettings().parseAlarmBPacket(alarmBPacket.copyOf(16))
		}
	}
}
