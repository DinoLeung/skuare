package xyz.d1n0.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.datetime.LocalDate
import xyz.d1n0.lib.constant.ReminderDayOfWeek
import xyz.d1n0.lib.constant.ReminderRecurrence
import xyz.d1n0.lib.model.ReminderConfig
import xyz.d1n0.lib.model.ReminderTitle

@Preview
@Composable
fun ReminderPreview() {
    Column {
        ReminderCard(
            reminder = xyz.d1n0.lib.model.Reminder(
                ReminderTitle("Reminder once"),
                ReminderConfig(
                    enable = true,
                    recurrence = ReminderRecurrence.ONCE_SAME_DAY,
                    startDate = LocalDate(year = 2025, monthNumber = 1, dayOfMonth = 1),
                    endDate = LocalDate(year = 2025, monthNumber = 1, dayOfMonth = 1),
                    daysOfWeek = setOf<ReminderDayOfWeek>(),
                )
            )
        )
        ReminderCard(
            reminder = xyz.d1n0.lib.model.Reminder(
                ReminderTitle("Reminder multi-days"),
                ReminderConfig(
                    enable = true,
                    recurrence = ReminderRecurrence.REPEAT_DAYS,
                    startDate = LocalDate(year = 2025, monthNumber = 1, dayOfMonth = 1),
                    endDate = LocalDate(year = 2025, monthNumber = 12, dayOfMonth = 31),
                    daysOfWeek = setOf<ReminderDayOfWeek>(),
                )
            )
        )
        ReminderCard(
            reminder = xyz.d1n0.lib.model.Reminder(
                ReminderTitle("Reminder weekly"),
                ReminderConfig(
                    enable = true,
                    recurrence = ReminderRecurrence.REPEAT_WEEKLY,
                    startDate = LocalDate(year = 2025, monthNumber = 1, dayOfMonth = 1),
                    endDate = LocalDate(year = 2025, monthNumber = 1, dayOfMonth = 1),
                    daysOfWeek = setOf(ReminderDayOfWeek.MONDAY, ReminderDayOfWeek.WEDNESDAY, ReminderDayOfWeek.FRIDAY)
                )
            )
        )
        ReminderCard(
            reminder = xyz.d1n0.lib.model.Reminder(
                ReminderTitle("Reminder monthly"),
                ReminderConfig(
                    enable = true,
                    recurrence = ReminderRecurrence.REPEAT_MONTHLY,
                    startDate = LocalDate(year = 2025, monthNumber = 6, dayOfMonth = 13),
                    endDate = LocalDate(year = 2025, monthNumber = 6, dayOfMonth = 13),
                    daysOfWeek = setOf<ReminderDayOfWeek>(),
                )
            )
        )
        ReminderCard(
            reminder = xyz.d1n0.lib.model.Reminder(
                ReminderTitle("Reminder yearly"),
                ReminderConfig(
                    enable = true,
                    recurrence = ReminderRecurrence.REPEAT_YEARLY,
                    startDate = LocalDate(year = 2025, monthNumber = 6, dayOfMonth = 9),
                    endDate = LocalDate(year = 2025, monthNumber = 6, dayOfMonth = 9),
                    daysOfWeek = setOf<ReminderDayOfWeek>(),
                )
            )
        )
    }
}