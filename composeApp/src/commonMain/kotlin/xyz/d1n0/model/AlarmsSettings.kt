package xyz.d1n0.model

import xyz.d1n0.constant.OpCode
import kotlin.experimental.or

class AlarmsSettings {
    lateinit var hourlySignal: SignalAlarm
    lateinit var alarm1: Alarm
    lateinit var alarm2: Alarm
    lateinit var alarm3: Alarm
    lateinit var alarm4: Alarm
    lateinit var alarmSnooze: Alarm

    val isInitialized: Boolean
        get() = ::hourlySignal.isInitialized
            &&::alarm1.isInitialized
            &&::alarm2.isInitialized
            &&::alarm3.isInitialized
            &&::alarm4.isInitialized
            &&::alarmSnooze.isInitialized

    @OptIn(ExperimentalStdlibApi::class)
    fun parseAlarmAPacket(packet: ByteArray) {
        require(packet.first() == OpCode.ALARM_A.byte) {
            "Alarm A packet must starts with command code ${OpCode.ALARM_A.byte.toHexString(HexFormat.UpperCase)}"
        }
        require(packet.size == 5) {
            "Alarm A packet must be exactly 5 bytes long, e.g. 15 C0 00 0C 1E"
        }
        hourlySignal = SignalAlarm.fromByte(packet[0])
        alarm1 = Alarm.fromBytes(packet.sliceArray(1..packet.lastIndex))
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun parseAlarmBPacket(packet: ByteArray) {
        require(packet.first() == OpCode.ALARM_B.byte) {
            "Alarm B packet must starts with command code ${OpCode.ALARM_B.byte.toHexString(HexFormat.UpperCase)}"
        }
        require(packet.size == 17) {
            "Alarm B packet must be exactly 17 bytes long, e.g. 16 00 00 01 1E 00 00 0E 0F 00 00 0F 2D 40 00 17 3B"
        }
        alarm2 = Alarm.fromBytes(packet.sliceArray(1..4))
        alarm3 = Alarm.fromBytes(packet.sliceArray(5..8))
        alarm4 = Alarm.fromBytes(packet.sliceArray(9..12))
        alarmSnooze = Alarm.fromBytes(packet.sliceArray(13..packet.lastIndex))
    }

    val alarmAPacket: ByteArray
        get() {
            require(isInitialized) { "Alarms must be initialized" }
            val signalBytes = hourlySignal.byte
            val alarmBytes = alarm1.bytes.apply {
                this[0] = this[0] or hourlySignal.byte
            }
            return byteArrayOf(
                OpCode.ALARM_A.byte,
                *alarmBytes,
            )
        }

    val alarmBPacket: ByteArray
        get() {
            require(isInitialized) { "Alarms must be initialized" }
            return byteArrayOf(
                OpCode.ALARM_B.byte,
                *alarm2.bytes,
                *alarm3.bytes,
                *alarm4.bytes,
                *alarmSnooze.bytes,
            )
        }
}
