package xyz.d1n0.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.d1n0.lib.constant.ReminderDayOfWeek
import xyz.d1n0.lib.constant.ReminderRecurrence
import xyz.d1n0.lib.model.Reminder
import xyz.d1n0.lib.model.ReminderConfig
import xyz.d1n0.lib.model.ReminderTitle

@Composable
fun ReminderCard(
	modifier: Modifier = Modifier,
	reminder: Reminder,
	onTitleChange: (String) -> Unit = {},
	titleEnabled: Boolean = true,
	titleIsError: Boolean = false,
	titleSupportingText: @Composable (() -> Unit)? = null,

	toggleChange: (Boolean) -> Unit = {},
	recurrenceChange: (ReminderRecurrence) -> Unit = {},
	startDateChange: (LocalDate) -> Unit = {},
	endDateChange: (LocalDate) -> Unit = {},

	configEnabled: Boolean = true,
	configError: Throwable? = null,
) {
	CardView(modifier = Modifier.fillMaxWidth()) {
		Column(
			modifier = Modifier.fillMaxWidth(),
			verticalArrangement = Arrangement.spacedBy(8.dp),
			horizontalAlignment = Alignment.CenterHorizontally
		) {
			SwitchField(
				title = {
					OutlinedTextField(
						modifier = Modifier.fillMaxWidth(),
						value = reminder.title.value,
						label = { Text("Title") },
						onValueChange = onTitleChange,
						enabled = titleEnabled,
						isError = titleIsError,
						supportingText = titleSupportingText
					)
				},
				check = reminder.config.enable,
				onCheckedChange = toggleChange,
				enabled = configEnabled,
				modifier = Modifier.fillMaxWidth()
			)

			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.spacedBy(8.dp),
				verticalAlignment = Alignment.CenterVertically
			) {
				Box(modifier = Modifier.weight(1f)) {
					EnumDropdown(
						selectedOption = reminder.config.recurrence,
						options = ReminderRecurrence.values(),
						label = "Recurrence",
						onOptionSelected = recurrenceChange,
						enabled = configEnabled,
					)
				}
				Box(modifier = Modifier.weight(2f)) {
					when (reminder.config.recurrence) {
						ReminderRecurrence.ONCE -> {
							DateSinglePickerField(
								value = reminder.config.startDate,
								onValueChange = {
									startDateChange(it)
									endDateChange(it)
								},
								modifier = Modifier.fillMaxWidth(),
								label = "On",
								enabled = true
							)
						}

						ReminderRecurrence.REPEAT_DAILY -> {
							Row(
								horizontalArrangement = Arrangement.spacedBy(8.dp),
							) {
								DateSinglePickerField(
									value = reminder.config.startDate,
									onValueChange = { startDateChange(it) },
									modifier = Modifier.weight(1f),
									label = "From",
									enabled = true
								)
								DateSinglePickerField(
									value = reminder.config.endDate,
									onValueChange = { endDateChange(it) },
									modifier = Modifier.weight(1f),
									label = "To",
									enabled = true
								)
							}
						}

						ReminderRecurrence.REPEAT_WEEKLY -> {
							// TODO: weekdays multi-select
							Text(text = reminder.config.daysOfWeekDisplayString)
						}

						ReminderRecurrence.REPEAT_MONTHLY, ReminderRecurrence.REPEAT_YEARLY -> {
							DateSinglePickerField(
								value = reminder.config.startDate,
								onValueChange = {
									startDateChange(it)
									endDateChange(it)
								},
								modifier = Modifier.fillMaxWidth(),
								label = "Starts On",
								enabled = true
							)
						}
					}
				}
			}
			configError?.message?.let { ErrorText(text = it) }
		}
	}
}

@Preview
@Composable
private fun ReminderCardPreview() {
	Column {
		ReminderCard(
			reminder = Reminder(
				ReminderTitle("Reminder once"),
				ReminderConfig(
					enable = true,
					recurrence = ReminderRecurrence.ONCE,
					startDate = LocalDate(year = 2025, monthNumber = 1, dayOfMonth = 1),
					endDate = LocalDate(year = 2025, monthNumber = 1, dayOfMonth = 1),
					daysOfWeek = setOf<ReminderDayOfWeek>(),
				)
			)
		)
		ReminderCard(
			reminder = Reminder(
				ReminderTitle("Reminder Daily"),
				ReminderConfig(
					enable = true,
					recurrence = ReminderRecurrence.REPEAT_DAILY,
					startDate = LocalDate(year = 2025, monthNumber = 1, dayOfMonth = 1),
					endDate = LocalDate(year = 2025, monthNumber = 12, dayOfMonth = 31),
					daysOfWeek = setOf<ReminderDayOfWeek>(),
				)
			)
		)
		ReminderCard(
			reminder = Reminder(
				ReminderTitle("Reminder weekly"),
				ReminderConfig(
					enable = true,
					recurrence = ReminderRecurrence.REPEAT_WEEKLY,
					startDate = LocalDate(year = 2025, monthNumber = 1, dayOfMonth = 1),
					endDate = LocalDate(year = 2025, monthNumber = 1, dayOfMonth = 1),
					daysOfWeek = setOf(
						ReminderDayOfWeek.MONDAY,
						ReminderDayOfWeek.WEDNESDAY,
						ReminderDayOfWeek.FRIDAY
					)
				)
			)
		)
		ReminderCard(
			reminder = Reminder(
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
			reminder = Reminder(
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