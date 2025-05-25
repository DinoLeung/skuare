package xyz.d1n0.ui.component

import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.ui.tooling.preview.Preview
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

@Preview
@Composable
private fun SingleDatePickerDialogPreview() {
	SingleDatePickerDialog(
		date = LocalDate(year = 2025, monthNumber = 6, dayOfMonth = 9),
		visible = true,
		onConfirm = {},
		onDismiss = {}
	)
}