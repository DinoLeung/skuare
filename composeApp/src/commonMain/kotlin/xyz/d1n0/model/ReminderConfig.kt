package xyz.d1n0.model

import kotlinx.datetime.LocalDate
import xyz.d1n0.constant.ReminderBitmask
import xyz.d1n0.constant.ReminderDayOfWeek
import xyz.d1n0.constant.ReminderRecurrence
import xyz.d1n0.helper.fromBcdByteArray
import xyz.d1n0.helper.toBcdByteArray
import kotlinx.datetime.Clock as KotlinClock
import kotlinx.datetime.TimeZone as KotlinTimeZone
import kotlinx.datetime.toLocalDateTime

data class ReminderConfig(
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
                recurrence = ReminderRecurrence.fromByte(bytes[0]),
                startDate = LocalDate.fromBcdByteArray(bytes.sliceArray(1..3)),
                endDate = LocalDate.fromBcdByteArray(bytes.sliceArray(4..6)),
                daysOfWeek = ReminderDayOfWeek.daysFromByte(bytes[7]),
                )
        }
    }

    // always ensure dates are either current date or later
    // not tested the results of writing a date that is before the current date
    init {
        val today: LocalDate = KotlinClock.System.now().toLocalDateTime(KotlinTimeZone.currentSystemDefault()).date
        if (startDate < today)
            startDate = today
        if (endDate < startDate)
            endDate = startDate
    }

    val bytes: ByteArray
        get() = byteArrayOf(
            (recurrence.byte.toInt() or (if (enable) 1 else 0)).toByte(),
            *startDate.toBcdByteArray(),
            *endDate.toBcdByteArray(),
            ReminderDayOfWeek.byteFromDays(daysOfWeek),
        ).copyOf(9)
}
