package xyz.d1n0.lib.model

import xyz.d1n0.lib.constant.OpCode
import kotlin.experimental.or

data class AlarmsSettings(
	val hourlySignal: HourlySignal? = null,
	val alarms: List<Alarm?> = List(4) { null },
	val snoozeAlarm: Alarm? = null,
) {
	private val allAlarms: List<Alarm>
		get() = listOfNotNull(
			*alarms.toTypedArray(),
			snoozeAlarm
		)

	val isInitialized: Boolean get() = hourlySignal != null && allAlarms.size == 5

	@OptIn(ExperimentalStdlibApi::class)
	fun parseAlarmAPacket(packet: ByteArray): AlarmsSettings {
		require(packet.first() == OpCode.ALARM_A.byte) {
			"Alarm A packet must starts with command code ${
				OpCode.ALARM_A.byte.toHexString(
					HexFormat.UpperCase
				)
			}"
		}
		require(packet.size == 5) {
			"Alarm A packet must be exactly 5 bytes long, e.g. 15 C0 00 0C 1E"
		}
		return this.copy(
			hourlySignal = HourlySignal.fromByte(packet[0]),
			alarms = alarms.toMutableList()
				.also { it[0] = Alarm.fromBytes(packet.sliceArray(1..packet.lastIndex)) })
	}

	@OptIn(ExperimentalStdlibApi::class)
	fun parseAlarmBPacket(packet: ByteArray): AlarmsSettings {
		require(packet.first() == OpCode.ALARM_B.byte) {
			"Alarm B packet must starts with command code ${
				OpCode.ALARM_B.byte.toHexString(
					HexFormat.UpperCase
				)
			}"
		}
		require(packet.size == 17) {
			"Alarm B packet must be exactly 17 bytes long, e.g. 16 00 00 01 1E 00 00 0E 0F 00 00 0F 2D 40 00 17 3B"
		}
		return this.copy(
			alarms = alarms.toMutableList()
				.also { it[1] = Alarm.fromBytes(packet.sliceArray(1..4)) }
				.also { it[2] = Alarm.fromBytes(packet.sliceArray(5..8)) }
				.also { it[3] = Alarm.fromBytes(packet.sliceArray(9..12)) },
			snoozeAlarm = Alarm.fromBytes(packet.sliceArray(13..packet.lastIndex))
		)
	}

	val alarmAPacket: ByteArray
		get() {
			val signal = requireNotNull(hourlySignal) { "HourlySignal must be initialized" }
			val firstAlarm =
				requireNotNull(alarms.first()) { "Alarm at position 0 must be initialized" }
			val signalBytes = signal.byte
			val alarmBytes = firstAlarm.bytes.apply {
				this[0] = this[0] or signal.byte
			}
			return byteArrayOf(
				OpCode.ALARM_A.byte,
				*alarmBytes,
			)
		}

	val alarmBPacket: ByteArray
		get() {
			val alarmList = listOf<Alarm?>(
				*alarms.drop(1).toTypedArray(),
				snoozeAlarm,
			)
			return alarmList.mapIndexed { index, alarm ->
				requireNotNull(alarm) {
					if (index == alarmList.lastIndex) "SnoozeAlarm must be initialized"
					else "Alarm at position ${index + 1} must be initialized"
				}
			}
				.fold(byteArrayOf(OpCode.ALARM_B.byte)) { acc, alarm -> acc + alarm.bytes }
		}
}
