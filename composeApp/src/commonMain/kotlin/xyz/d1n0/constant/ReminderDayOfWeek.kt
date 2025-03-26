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
        fun daysFromValue(value: Int): MutableSet<ReminderDayOfWeek> {
            val days = mutableSetOf<ReminderDayOfWeek>()
            if ((value and ReminderDayOfWeekBitmask.SUNDAY) != 0) days.add(SUNDAY)
            if ((value and ReminderDayOfWeekBitmask.MONDAY) != 0) days.add(MONDAY)
            if ((value and ReminderDayOfWeekBitmask.TUESDAY) != 0) days.add(TUESDAY)
            if ((value and ReminderDayOfWeekBitmask.WEDNESDAY) != 0) days.add(WEDNESDAY)
            if ((value and ReminderDayOfWeekBitmask.THURSDAY) != 0) days.add(THURSDAY)
            if ((value and ReminderDayOfWeekBitmask.FRIDAY) != 0) days.add(FRIDAY)
            if ((value and ReminderDayOfWeekBitmask.SATURDAY) != 0) days.add(SATURDAY)
            return days
        }
        fun valueFromDays(days: Set<ReminderDayOfWeek>): Int {
            var value = 0
            if (SUNDAY in days) value = value or ReminderDayOfWeekBitmask.SUNDAY
            if (MONDAY in days) value = value or ReminderDayOfWeekBitmask.MONDAY
            if (TUESDAY in days) value = value or ReminderDayOfWeekBitmask.TUESDAY
            if (WEDNESDAY in days) value = value or ReminderDayOfWeekBitmask.WEDNESDAY
            if (THURSDAY in days) value = value or ReminderDayOfWeekBitmask.THURSDAY
            if (FRIDAY in days) value = value or ReminderDayOfWeekBitmask.FRIDAY
            if (SATURDAY in days) value = value or ReminderDayOfWeekBitmask.SATURDAY
            return value
        }
    }
}