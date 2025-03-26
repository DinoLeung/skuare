package xyz.d1n0.constant

import xyz.d1n0.constant.ReminderBitmask.MULTIPLE_DAYS
import xyz.d1n0.constant.ReminderBitmask.WEEKLY
import xyz.d1n0.constant.ReminderBitmask.YEARLY
import xyz.d1n0.constant.ReminderBitmask.MONTHLY

enum class ReminderRecurrence {
    ONCE_SAME_DAY,
    ONCE_MULTIPLE_DAYS,
    REPEAT_WEEKLY,
    REPEAT_MONTHLY,
    REPEAT_YEARLY;

    companion object {
        fun fromValue(value: Int) =
            when {
                value and MULTIPLE_DAYS == MULTIPLE_DAYS -> ONCE_MULTIPLE_DAYS
                value and WEEKLY == WEEKLY -> REPEAT_WEEKLY
                value and YEARLY == YEARLY -> REPEAT_YEARLY
                value and MONTHLY == MONTHLY -> REPEAT_MONTHLY
                else -> ONCE_SAME_DAY
            }
        }

    val value: Int
        get() = when(this) {
            ONCE_SAME_DAY -> 0
            ONCE_MULTIPLE_DAYS -> MULTIPLE_DAYS
            REPEAT_WEEKLY -> WEEKLY
            REPEAT_MONTHLY -> MONTHLY
            REPEAT_YEARLY -> YEARLY
        }
}
