package xyz.d1n0.model

import xyz.d1n0.constant.Command
import kotlin.experimental.or

class AlarmsSettings {
    lateinit var hourlySignal: SignalAlarm
    lateinit var alarm1: Alarm
    lateinit var alarm2: Alarm
    lateinit var alarm3: Alarm
    lateinit var alarm4: Alarm
    lateinit var alarmSnooze: Alarm

    fun isInitialized() = ::hourlySignal.isInitialized
            &&::alarm1.isInitialized
            &&::alarm2.isInitialized
            &&::alarm3.isInitialized
            &&::alarm4.isInitialized
            &&::alarmSnooze.isInitialized

    @OptIn(ExperimentalStdlibApi::class)
    fun parseAlarmAPacket(packet: ByteArray) {
        require(packet.first() == Command.ALARM_A.value.toByte()) {
            "Alarm A packet must starts with command code ${Command.ALARM_A.value.toHexString(HexFormat.UpperCase)}"
        }
        require(packet.size == 5) {
            "Alarm A packet must be exactly 5 bytes long, e.g. 15 C0 00 0C 1E"
        }
        hourlySignal = SignalAlarm.fromByte(packet[0])
        alarm1 = Alarm.fromBytes(packet.sliceArray(1..4))
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun parseAlarmBPacket(packet: ByteArray) {
        require(packet.first() == Command.ALARM_B.value.toByte()) {
            "Alarm B packet must starts with command code ${Command.ALARM_B.value.toHexString(HexFormat.UpperCase)}"
        }
        require(packet.size == 17) {
            "Alarm B packet must be exactly 17 bytes long, e.g. 16 00 00 01 1E 00 00 0E 0F 00 00 0F 2D 40 00 17 3B"
        }
        alarm2 = Alarm.fromBytes(packet.sliceArray(1..4))
        alarm3 = Alarm.fromBytes(packet.sliceArray(5..8))
        alarm4 = Alarm.fromBytes(packet.sliceArray(9..12))
        alarmSnooze = Alarm.fromBytes(packet.sliceArray(13..16))
    }

    val alarmAPacket: ByteArray
        get() {
            require(isInitialized()) { "Alarms must be initialized" }
            val signalBytes = hourlySignal.byte
            val alarmBytes = alarm1.bytes.apply {
                this[0] = this[0] or hourlySignal.byte
            }
            return byteArrayOf(
                Command.ALARM_A.value.toByte(),
                *alarmBytes,
            )
        }

    val alarmBPacket: ByteArray
        get() {
            require(isInitialized()) { "Alarms must be initialized" }
            return byteArrayOf(
                Command.ALARM_B.value.toByte(),
                *alarm2.bytes,
                *alarm3.bytes,
                *alarm4.bytes,
                *alarmSnooze.bytes,
            )
        }
}

// ALARM SCREEN
//110F0F0F0600500004000100001E03 ???
//15 00 00 00 1E disable 1230am
//15 80 00 00 1E disable 1230am sig
//15 40 00 00 1E enable 1230am
//15 C0 00 00 1E enable 1230am sig
//15 00 00 0C 1E disable 1230am
//15 80 00 0C 1E disable 1230am sig
//15 40 00 0C 1E enabled 1230pm
//15 C0 00 0C 1E enabled 1230pm sig

//16 00 00 01 1E 00 00 0E 0F 00 00 0F 2D 40 00 17 3B

//15 C0 00 0E 0F
//15 80 00 0E 0F

// 15 40 00 00 1E 12:30am en no sig

// 40 00 00 1E true 00 30

// 16 00 00 01 1E 40 00 0E 0F 00 00 0F 2D 40 00 11 00

// 00 00 01 1E false 01 30 (2)
// 40 00 0E 0F true 14 15 (3)
// 00 00 0F 2D false 15 45 (4)
// 40 00 11 00 true 17 00 (snooze)
// 40 00 17 3B true 23 59 (snooze)
