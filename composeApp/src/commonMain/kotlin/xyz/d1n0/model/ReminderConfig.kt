package xyz.d1n0.model

import kotlinx.datetime.LocalDate
import xyz.d1n0.constant.ReminderBitmask
import xyz.d1n0.constant.ReminderDayOfWeek
import xyz.d1n0.constant.ReminderRecurrence
import xyz.d1n0.helper.fromBcdByteArray
import xyz.d1n0.helper.toBcdByteArray

data class ReminderConfig(
//    var name: String,
    val enable: Boolean,
    val recurrence: ReminderRecurrence,
    var startDate: LocalDate,
    var endDate: LocalDate,
    val daysOfWeek: MutableSet<ReminderDayOfWeek>,
) {
    companion object {
        fun fromBytes(bytes: ByteArray): ReminderConfig {
            require(bytes.size == 9) { "Reminder Config bytes must be 9 bytes long, e.g. 11 25 03 25 25 03 25 12 00" }
            return ReminderConfig(
                enable = bytes[0].toInt() and ReminderBitmask.ENABLE != 0,
                recurrence = ReminderRecurrence.fromValue(bytes[1].toInt()),
                startDate = LocalDate.fromBcdByteArray(bytes.sliceArray(2..4)),
                endDate = LocalDate.fromBcdByteArray(bytes.sliceArray(5..7)),
                daysOfWeek = ReminderDayOfWeek.daysFromValue(bytes[8].toInt()),
                )
        }
    }

    fun toBytes() = byteArrayOf(
        (recurrence.value or (if (enable) 1 else 0)).toByte(),
        *startDate.toBcdByteArray(),
        *endDate.toBcdByteArray(),
        ReminderDayOfWeek.valueFromDays(daysOfWeek).toByte(),
    ).copyOf(9)

    // 31 01 01 25 03 23 25 03 23 00 00
    // byte 1 is position
    // byte 2 is bitmask of enable and recurrence
    // byte 3..5 is start date
    // byte 6..8 is end date
    // byte 9 is day of week occurrence, only use for weekly?
    // byte 10 is always 0

    // 01 -> 00001
    // 0B -> 01011
    // 05 -> 00101
    // 09 -> 01001
    // 11 -> 10001
    // 31 01 0B 25 03 25 29 12 31 00 00
    // 31 03 0B 25 03 26 30 01 01 00 00
    // 25/03/25 - 31/12/29 (training period)
    // 31 01 01 25 03 25 25 03 25 00 00 (once)
    // 31 01 05 25 03 25 25 03 25 12 00 (weekly, mon thurs)
    // 31 01 11 25 03 25 25 03 25 12 00 (monthly 25, it preserves weekly selection)
    // 31 01 09 25 03 25 25 03 25 12 00 (yearly mar 25)
    // "♦︎ TEST"
    // 30 01 8C 20 54 45 53 54 000000000000000000000000
    // 20 bytes include command and pos
    // 8C -> ♦︎
    // 20 -> " "
    // 54 -> T
    // 45 -> E
    // 53 -> S
    // 30 03 8D 8C 90 91 92 93 80 87 82 8B 2D 2C 00 00 00 00 00 00
// ♪ -> 8D
// ◆ -> 8C
// 【 -> 90
// 】 -> 91
// ◀ -> 92
// ▶ -> 93
// ¥ -> 80
// ± -> 87
// 《 -> 82
// 》 -> 8B
// - -> 2D
// , -> 2C
}
