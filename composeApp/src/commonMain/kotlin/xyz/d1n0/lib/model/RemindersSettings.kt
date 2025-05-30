package xyz.d1n0.lib.model

import xyz.d1n0.lib.constant.OpCode
import xyz.d1n0.lib.helper.requireIn

data class RemindersSettings(
	val reminderTitles: List<ReminderTitle?> = List(5) { null },
	val reminderConfigs: List<ReminderConfig?> = List(5) { null },
) {
	@OptIn(ExperimentalStdlibApi::class)
	fun parseReminderTitlePacket(packet: ByteArray): RemindersSettings {
		require(packet.first() == OpCode.REMINDER_TITLE.byte) {
			"Reminder title packet must start with command code ${
				OpCode.REMINDER_TITLE.byte.toHexString(
					HexFormat.UpperCase
				)
			}"
		}
		require(packet.size == 20) {
			"Reminder title packet must be exactly 20 bytes long, e.g. 30 01 8D 8C 90 91 92 93 80 87 82 8B 2D 2C 00 00 00 00 00 00"
		}
		val index =
			packet[1].toInt().requireIn(1..5) { "Reminder position must be in 1..5" }.minus(1)
		return this.copy(
			reminderTitles = reminderTitles.toMutableList()
				.also {
					it[index] = ReminderTitle.fromBytes(packet.sliceArray(2..packet.lastIndex))
				})
	}

	@OptIn(ExperimentalStdlibApi::class)
	fun parseReminderConfigPacket(packet: ByteArray): RemindersSettings {
		require(packet.first() == OpCode.REMINDER_CONFIG.byte) {
			"Reminder config packet must start with command code ${
				OpCode.REMINDER_CONFIG.byte.toHexString(
					HexFormat.UpperCase
				)
			}"
		}
		require(packet.size == 11) {
			"Reminder config packet must be exactly 11 bytes long, e.g. 31 01 11 25 03 25 25 03 25 12 00"
		}
		val index =
			packet[1].toInt().requireIn(1..5) { "Reminder position must be in 1..5" }.minus(1)
		val config = ReminderConfig.fromBytes(packet.sliceArray(2..packet.lastIndex))
		return this.copy(
			reminderConfigs = reminderConfigs.toMutableList()
				.also {
					it[index] = ReminderConfig.fromBytes(packet.sliceArray(2..packet.lastIndex))
				})
	}

	val reminderTitlePackets: List<ByteArray>
		get() = reminderTitles.mapIndexed { index, title ->
			val value = requireNotNull(title) { "Reminder title at position $index must be initialized" }
			byteArrayOf(OpCode.REMINDER_TITLE.byte, index.plus(1).toByte()) + value.bytes
		}

	val reminderConfigPackets: List<ByteArray>
		get() = reminderConfigs.mapIndexed { index, config ->
			val value = requireNotNull(config) { "Reminder config at position $index must be initialized" }
			byteArrayOf(OpCode.REMINDER_CONFIG.byte, index.plus(1).toByte()) + value.bytes
		}
}
