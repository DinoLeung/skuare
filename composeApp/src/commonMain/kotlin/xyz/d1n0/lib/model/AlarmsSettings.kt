package xyz.d1n0.lib.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import xyz.d1n0.lib.constant.OpCode
import kotlin.experimental.or

class AlarmsSettings{
    private val _hourlySignal = MutableStateFlow<HourlySignal?>(null)
    val hourlySignal: StateFlow<HourlySignal?> get() = _hourlySignal.asStateFlow()

    private val _alarms = MutableStateFlow<List<Alarm?>>(List(4) { null })
    val alarms: StateFlow<List<Alarm?>> get() = _alarms.asStateFlow()

    private val _snoozeAlarm = MutableStateFlow<Alarm?>(null)
    val snoozeAlarm: StateFlow<Alarm?> get() = _snoozeAlarm.asStateFlow()

    private val allAlarms: List<Alarm> get() =
        listOfNotNull(
            *alarms.value.toTypedArray(),
            snoozeAlarm.value
        )

    val isInitialized: StateFlow<Boolean> get() = _isInitialized.asStateFlow()
    private val _isInitialized = MutableStateFlow(false)
    private fun updateIsInitialized() = _isInitialized.update { _hourlySignal.value != null && allAlarms.size == 5 }

    @OptIn(ExperimentalStdlibApi::class)
    fun parseAlarmAPacket(packet: ByteArray) {
        require(packet.first() == OpCode.ALARM_A.byte) {
            "Alarm A packet must starts with command code ${OpCode.ALARM_A.byte.toHexString(HexFormat.UpperCase)}"
        }
        require(packet.size == 5) {
            "Alarm A packet must be exactly 5 bytes long, e.g. 15 C0 00 0C 1E"
        }
        _hourlySignal.update { HourlySignal.fromByte(packet[0]) }
        _alarms.update {
            it.toMutableList().also { it[0] = Alarm.fromBytes(packet.sliceArray(1..packet.lastIndex)) }
        }
        updateIsInitialized()
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun parseAlarmBPacket(packet: ByteArray) {
        require(packet.first() == OpCode.ALARM_B.byte) {
            "Alarm B packet must starts with command code ${OpCode.ALARM_B.byte.toHexString(HexFormat.UpperCase)}"
        }
        require(packet.size == 17) {
            "Alarm B packet must be exactly 17 bytes long, e.g. 16 00 00 01 1E 00 00 0E 0F 00 00 0F 2D 40 00 17 3B"
        }
        _alarms.update {
            it.toMutableList()
                .also { it[1] = Alarm.fromBytes(packet.sliceArray(1..4)) }
                .also { it[2] = Alarm.fromBytes(packet.sliceArray(5..8)) }
                .also { it[3] = Alarm.fromBytes(packet.sliceArray(9..12)) }
        }
        _snoozeAlarm.update { Alarm.fromBytes(packet.sliceArray(13..packet.lastIndex)) }
        updateIsInitialized()
    }

    val alarmAPacket: ByteArray
        get() {
            val signal = requireNotNull(hourlySignal.value) { "HourlySignal must be initialized" }
            val firstAlarm = requireNotNull(alarms.value.first()) { "Alarm at position 0 must be initialized" }
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
                *alarms.value.drop(1).toTypedArray(),
                snoozeAlarm.value,
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
