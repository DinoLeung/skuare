package xyz.d1n0.lib.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import xyz.d1n0.lib.constant.OpCode
import xyz.d1n0.lib.helper.requireIn
import kotlin.text.toByte

data class Reminder(
    var title: ReminderTitle? = null,
    var config: ReminderConfig? = null,
)
class RemindersSettings {
    val _reminders = MutableStateFlow<List<Reminder>>(List(5) { Reminder() })
    val reminders: StateFlow<List<Reminder>> = _reminders.asStateFlow()

    val isTitlesInitialized: StateFlow<Boolean> get() = _isTitlesInitialized.asStateFlow()
    private val _isTitlesInitialized = MutableStateFlow(false)

    val isConfigsInitialized: StateFlow<Boolean> get() = _isConfigsInitialized.asStateFlow()
    private val _isConfigsInitialized = MutableStateFlow(false)

    private fun updateIsInitialized() {
        _isTitlesInitialized.update { reminders.value.all { it.title != null } }
        _isConfigsInitialized.update { reminders.value.all { it.config != null } }
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun parseReminderTitlePacket(packet: ByteArray) {
        require(packet.first() == OpCode.REMINDER_TITLE.byte) {
            "Reminder title packet must starts with command code ${OpCode.REMINDER_TITLE.byte.toHexString(HexFormat.UpperCase)}"
        }
        require(packet.size == 20) {
            "Reminder title packet must be exactly 20 bytes long, e.g. 30 01 8D 8C 90 91 92 93 80 87 82 8B 2D 2C 00 00 00 00 00 00"
        }
        _reminders.update {
            val index = packet[1].toInt()
                .requireIn(1..5) { "Reminder position must be in 1..5" }
                .minus(1)
            val title = ReminderTitle.fromBytes(packet.sliceArray(2..packet.lastIndex))
            it.toMutableList().also { it[index].title = title }
        }.also { updateIsInitialized() }
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun parseReminderConfigPacket(packet: ByteArray) {
        require(packet.first() == OpCode.REMINDER_CONFIG.byte) {
            "Reminder config packet must starts with command code ${OpCode.REMINDER_CONFIG.byte.toHexString(HexFormat.UpperCase)}"
        }
        require(packet.size == 11) {
            "Reminder config packet must be exactly 11 bytes long, e.g. 31 01 11 25 03 25 25 03 25 12 00"
        }
        _reminders.update {
            val index = packet[1].toInt()
                .requireIn(1..5) { "Reminder position must be in 1..5" }
                .minus(1)
            val config = ReminderConfig.fromBytes(packet.sliceArray(2..packet.lastIndex))
            it.toMutableList().also { it[index].config = config }
        }.also { updateIsInitialized() }
    }

    val reminderTitlePackets: List<ByteArray>
        get() = reminders.value.mapIndexed { index, reminder ->
            val title = requireNotNull(reminder.title) { "Reminder title at position $index must be initialized" }
            byteArrayOf(OpCode.REMINDER_TITLE.byte, index.plus(1).toByte()) + title.bytes
        }

    val reminderConfigPackets: List<ByteArray>
        get() = reminders.value.mapIndexed { index, reminder ->
            val config = requireNotNull(reminder.config) { "Reminder config at position $index must be initialized" }
            byteArrayOf(OpCode.REMINDER_TITLE.byte, index.plus(1).toByte()) + config.bytes
        }
}
