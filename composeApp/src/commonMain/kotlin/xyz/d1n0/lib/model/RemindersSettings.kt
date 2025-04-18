package xyz.d1n0.lib.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import xyz.d1n0.lib.constant.OpCode
import xyz.d1n0.lib.helper.requireIn

class RemindersSettings {
    private val _reminderTitles = MutableStateFlow<List<ReminderTitle?>>(List(5) { null })
    val reminderTitles: StateFlow<List<ReminderTitle?>> get() = _reminderTitles.asStateFlow()
    private val _reminderConfigs = MutableStateFlow<List<ReminderConfig?>>(List(5) { null })
    val reminderConfigs: StateFlow<List<ReminderConfig?>> get() = _reminderConfigs.asStateFlow()

    val isTitlesInitialized: StateFlow<Boolean> get() = _isTitlesInitialized.asStateFlow()
    private val _isTitlesInitialized = MutableStateFlow(false)
    private fun updateIsTitlesInitialized() =
        _isTitlesInitialized.update { reminderTitles.value.all { it != null } }
            .also { updateIsInitialized() }

    val isConfigsInitialized: StateFlow<Boolean> get() = _isConfigsInitialized.asStateFlow()
    private val _isConfigsInitialized = MutableStateFlow(false)
    private fun updateIsConfigsInitialized() =
        _isConfigsInitialized.update { reminderConfigs.value.all { it != null } }
            .also { updateIsInitialized() }

    val isInitialized: StateFlow<Boolean> get() = _isInitialized.asStateFlow()
    private val _isInitialized = MutableStateFlow(false)
    private fun updateIsInitialized() =
        _isInitialized.update { isTitlesInitialized.value && isConfigsInitialized.value }

    @OptIn(ExperimentalStdlibApi::class)
    fun parseReminderTitlePacket(packet: ByteArray) {
        require(packet.first() == OpCode.REMINDER_TITLE.byte) {
            "Reminder title packet must starts with command code ${OpCode.REMINDER_TITLE.byte.toHexString(HexFormat.UpperCase)}"
        }
        require(packet.size == 20) {
            "Reminder title packet must be exactly 20 bytes long, e.g. 30 01 8D 8C 90 91 92 93 80 87 82 8B 2D 2C 00 00 00 00 00 00"
        }
        _reminderTitles.update {
            val index = packet[1].toInt()
                .requireIn(1..5) { "Reminder position must be in 1..5" }
                .minus(1)
            val title = ReminderTitle.fromBytes(packet.sliceArray(2..packet.lastIndex))
            it.toMutableList().also { it[index] = title }
        }.also { updateIsTitlesInitialized() }
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun parseReminderConfigPacket(packet: ByteArray) {
        require(packet.first() == OpCode.REMINDER_CONFIG.byte) {
            "Reminder config packet must starts with command code ${OpCode.REMINDER_CONFIG.byte.toHexString(HexFormat.UpperCase)}"
        }
        require(packet.size == 11) {
            "Reminder config packet must be exactly 11 bytes long, e.g. 31 01 11 25 03 25 25 03 25 12 00"
        }
        _reminderConfigs.update {
            val index = packet[1].toInt()
                .requireIn(1..5) { "Reminder position must be in 1..5" }
                .minus(1)
            val config = ReminderConfig.fromBytes(packet.sliceArray(2..packet.lastIndex))
            it.toMutableList().also { it[index] = config }
        }.also { updateIsConfigsInitialized() }
    }

    val reminderTitlePackets: List<ByteArray>
        get() {
            return reminderTitles.value.mapIndexed { index, title ->
                requireNotNull(title) { "Reminder title at position ${index + 1} must be initialized" }
                byteArrayOf(OpCode.REMINDER_TITLE.byte, index.plus(1).toByte()) + title.bytes
            }
        }

    val reminderConfigPackets: List<ByteArray>
        get() {
            return reminderConfigs.value.mapIndexed { index, config ->
                requireNotNull(config) { "Reminder config at position ${index + 1} must be initialized" }
                byteArrayOf(OpCode.REMINDER_CONFIG.byte, index.plus(1).toByte()) + config.bytes
            }
        }
}
