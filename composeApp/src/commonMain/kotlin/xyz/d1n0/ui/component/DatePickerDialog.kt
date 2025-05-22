package xyz.d1n0.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
	date: LocalDate,
	visible: Boolean,
	onConfirm: (LocalDate) -> Unit,
	onDismiss: () -> Unit,
) {
	val state = rememberDatePickerState(
//		initialSelectedDateMillis = LocalDateTime(
//			date = date,
//			time = LocalTime(hour = 0, minute = 0)
//		).toInstant(TimeZone.currentSystemDefault())
//			.toEpochMilliseconds(),
		initialSelectedDateMillis = date.toEpochDays() * 24 * 60 * 60 * 1000L,
		yearRange = IntRange(2000, 2099)
	)

	if (visible) {
		BasicAlertDialog(
			onDismissRequest = { onDismiss() },
			modifier = Modifier.wrapContentSize(),
			properties = DialogProperties()
		) {
			Surface(
				modifier = Modifier.wrapContentSize(),
				shape = MaterialTheme.shapes.large,
				tonalElevation = AlertDialogDefaults.TonalElevation
			) {
				Column(modifier = Modifier.padding(16.dp)) {
					DatePicker(state = state)
					Spacer(modifier = Modifier.height(24.dp))
					TextButton(
						onClick = {
							state.selectedDateMillis?.let {
								Instant.fromEpochMilliseconds(it)
									.toLocalDateTime(timeZone = TimeZone.currentSystemDefault())
									.date
									.let(onConfirm)
							}
						},
						modifier = Modifier.align(Alignment.End)
					) { Text("Confirm") }
				}
			}
		}
	}
}

@Preview
@Composable
private fun DatePickerDialogPreview() {
	DatePickerDialog(
		date = LocalDate(year = 2025, monthNumber = 6, dayOfMonth = 9),
		visible = true,
		onConfirm = {},
		onDismiss = {}
	)
}