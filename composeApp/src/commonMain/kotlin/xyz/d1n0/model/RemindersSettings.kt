package xyz.d1n0.model

import xyz.d1n0.constant.Command

class RemindersSettings {
    lateinit var reminderTitle1: ReminderTitle
    lateinit var reminderTitle2: ReminderTitle
    lateinit var reminderTitle3: ReminderTitle
    lateinit var reminderTitle4: ReminderTitle
    lateinit var reminderTitle5: ReminderTitle

    lateinit var reminderConfig1: ReminderConfig
    lateinit var reminderConfig2: ReminderConfig
    lateinit var reminderConfig3: ReminderConfig
    lateinit var reminderConfig4: ReminderConfig
    lateinit var reminderConfig5: ReminderConfig

    val isTitlesInitialized: Boolean
        get() = ::reminderTitle1.isInitialized
                && ::reminderTitle2.isInitialized
                && ::reminderTitle3.isInitialized
                && ::reminderTitle4.isInitialized
                && ::reminderTitle5.isInitialized

    val isConfigsInitialized: Boolean
        get() = ::reminderConfig1.isInitialized
                && ::reminderConfig2.isInitialized
                && ::reminderConfig3.isInitialized
                && ::reminderConfig4.isInitialized
                && ::reminderConfig5.isInitialized

    @OptIn(ExperimentalStdlibApi::class)
    fun parseReminderTitlePacket(packet: ByteArray) {
        require(packet.first() == Command.REMINDER_TITLE.byte) {
            "Reminder title packet must starts with command code ${Command.REMINDER_TITLE.byte.toHexString(HexFormat.UpperCase)}"
        }
        require(packet.size == 20) {
            "Reminder title packet must be exactly 20 bytes long, e.g. 30 01 8D 8C 90 91 92 93 80 87 82 8B 2D 2C 00 00 00 00 00 00"
        }
        when(packet[1].toInt()) {
            1 -> reminderTitle1 = ReminderTitle.fromBytes(packet.sliceArray(2..packet.lastIndex))
            2 -> reminderTitle2 = ReminderTitle.fromBytes(packet.sliceArray(2..packet.lastIndex))
            3 -> reminderTitle3 = ReminderTitle.fromBytes(packet.sliceArray(2..packet.lastIndex))
            4 -> reminderTitle4 = ReminderTitle.fromBytes(packet.sliceArray(2..packet.lastIndex))
            5 -> reminderTitle5 = ReminderTitle.fromBytes(packet.sliceArray(2..packet.lastIndex))
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun parseReminderConfigPacket(packet: ByteArray) {
        require(packet.first() == Command.REMINDER_CONFIG.byte) {
            "Reminder config packet must starts with command code ${Command.REMINDER_CONFIG.byte.toHexString(HexFormat.UpperCase)}"
        }
        require(packet.size == 11) {
            "Reminder config packet must be exactly 11 bytes long, e.g. 31 01 11 25 03 25 25 03 25 12 00"
        }
        when(packet[1].toInt()) {
            1 -> reminderConfig1 = ReminderConfig.fromBytes(packet.sliceArray(2..packet.lastIndex))
            2 -> reminderConfig2 = ReminderConfig.fromBytes(packet.sliceArray(2..packet.lastIndex))
            3 -> reminderConfig3 = ReminderConfig.fromBytes(packet.sliceArray(2..packet.lastIndex))
            4 -> reminderConfig4 = ReminderConfig.fromBytes(packet.sliceArray(2..packet.lastIndex))
            5 -> reminderConfig5 = ReminderConfig.fromBytes(packet.sliceArray(2..packet.lastIndex))
        }
    }

    val reminderTitlePackets: List<ByteArray>
        get() {
            require(isTitlesInitialized) { "Reminder titles must be initialized" }
            return listOf(
                byteArrayOf(Command.REMINDER_TITLE.byte, 1) + reminderTitle1.bytes,
                byteArrayOf(Command.REMINDER_TITLE.byte, 2) + reminderTitle2.bytes,
                byteArrayOf(Command.REMINDER_TITLE.byte, 3) + reminderTitle3.bytes,
                byteArrayOf(Command.REMINDER_TITLE.byte, 4) + reminderTitle4.bytes,
                byteArrayOf(Command.REMINDER_TITLE.byte, 5) + reminderTitle5.bytes,
            )
        }

    val reminderConfigPackets: List<ByteArray>
        get() {
            require(isConfigsInitialized) { "Reminder config packet must be initialized" }
            return listOf(
                byteArrayOf(Command.REMINDER_CONFIG.byte, 1) + reminderConfig1.bytes,
                byteArrayOf(Command.REMINDER_CONFIG.byte, 2) + reminderConfig2.bytes,
                byteArrayOf(Command.REMINDER_CONFIG.byte, 3) + reminderConfig3.bytes,
                byteArrayOf(Command.REMINDER_CONFIG.byte, 4) + reminderConfig4.bytes,
                byteArrayOf(Command.REMINDER_CONFIG.byte, 5) + reminderConfig5.bytes,
            )
        }
}
