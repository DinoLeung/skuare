package xyz.d1n0.constant

enum class ReminderDayOfWeek {
    SUNDAY,
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY;

    companion object {
        fun daysFromByte(byte: Byte): MutableSet<ReminderDayOfWeek> =
            byte.toInt().let {
                listOf(
                    ReminderDayOfWeekBitmask.SUNDAY to SUNDAY,
                    ReminderDayOfWeekBitmask.MONDAY to MONDAY,
                    ReminderDayOfWeekBitmask.TUESDAY to TUESDAY,
                    ReminderDayOfWeekBitmask.WEDNESDAY to WEDNESDAY,
                    ReminderDayOfWeekBitmask.THURSDAY to THURSDAY,
                    ReminderDayOfWeekBitmask.FRIDAY to FRIDAY,
                    ReminderDayOfWeekBitmask.SATURDAY to SATURDAY,
                )
                    .filter { (mask, _) -> it and mask != 0 }
                    .map { it.second }
                    .toMutableSet()
            }

        fun byteFromDays(days: Set<ReminderDayOfWeek>): Byte =
            listOf(
                SUNDAY to ReminderDayOfWeekBitmask.SUNDAY,
                MONDAY to ReminderDayOfWeekBitmask.MONDAY,
                TUESDAY to ReminderDayOfWeekBitmask.TUESDAY,
                WEDNESDAY to ReminderDayOfWeekBitmask.WEDNESDAY,
                THURSDAY to ReminderDayOfWeekBitmask.THURSDAY,
                FRIDAY to ReminderDayOfWeekBitmask.FRIDAY,
                SATURDAY to ReminderDayOfWeekBitmask.SATURDAY,
            )
                .filter { (day, _) -> day in days }
                .fold(0) { acc, (_, mask) -> acc or mask }
                .toByte()
    }
}