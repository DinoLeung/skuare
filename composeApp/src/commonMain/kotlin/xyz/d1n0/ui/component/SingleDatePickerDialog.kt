package xyz.d1n0.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import xyz.d1n0.lib.helper.fromMillis
import xyz.d1n0.lib.helper.toMillis
import xyz.d1n0.ui.boilerplate.DatePickerCustomerFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleDatePickerDialog(
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
		initialSelectedDateMillis = date.toMillis(),
		yearRange = IntRange(2000, 2099)
	)

	if (visible) {
		DatePickerDialog(
			modifier = Modifier.wrapContentSize(),
			onDismissRequest = { onDismiss() },
			confirmButton = {
				TextButton(onClick = {
					state.selectedDateMillis?.let {
						onConfirm(LocalDate.fromMillis(it))
					}
				}) { Text("Confirm") }
			},
			dismissButton = { TextButton(onClick = { onDismiss() }) { Text("Dismiss") } },
			content = {
				DatePicker(
					state = state,
					showModeToggle = false,
					dateFormatter = DatePickerCustomerFormatter(),
				)
			}
		)
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, widthDp = 360)
@Composable
private fun SingleDatePickerDialogPreview() {
	val state = rememberDatePickerState(
		initialSelectedDateMillis = LocalDate(
			year = 2025,
			month = 6,
			day = 9,
		).toMillis(),
		yearRange = IntRange(2000, 2099),
	)

	MaterialTheme {
		Surface(
			shape = MaterialTheme.shapes.extraLarge,
			tonalElevation = AlertDialogDefaults.TonalElevation,
		) {
			Column {
				DatePicker(
					state = state,
					showModeToggle = false,
					dateFormatter = DatePickerCustomerFormatter(),
				)
				Row(
					modifier = Modifier
						.fillMaxWidth()
						.padding(horizontal = 8.dp, vertical = 8.dp),
					horizontalArrangement = Arrangement.End,
				) {
					TextButton(onClick = {}) { Text("Dismiss") }
					TextButton(onClick = {}) { Text("Confirm") }
				}
			}
		}
	}
}
