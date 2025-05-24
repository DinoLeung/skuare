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
fun DateRangePickerDialog(
	startDate: LocalDate,
	endDate: LocalDate,
	visible: Boolean,
	onConfirm: (Pair<LocalDate, LocalDate>) -> Unit,
	onDismiss: () -> Unit,
) {
	val state = rememberDateRangePickerState(
		initialSelectedStartDateMillis = startDate.toMillis(),
		initialSelectedEndDateMillis = endDate.toMillis(),
		yearRange = IntRange(2000, 2099)
	)

	if (visible) {
		DatePickerDialog(
			modifier = Modifier.wrapContentSize(),
			confirmButton = {
				TextButton(onClick = {
					listOf(state.selectedStartDateMillis, state.selectedEndDateMillis)
						.filterNotNull()
						.takeIf { it.size == 2 }
						?.map { LocalDate.fromMillis(it) }
						?.let { Pair(it[0], it[1]) }
						?.let { onConfirm(it) }
				}) { Text("Confirm") }
			},
			onDismissRequest = { onDismiss() },
			dismissButton = { TextButton(onClick = { onDismiss() }) { Text("Dismiss") } },
			content = {
				DateRangePicker(
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
private fun DateRangePickerDialogPreview() {
	DateRangePickerDialog(
		startDate = LocalDate(year = 2025, monthNumber = 6, dayOfMonth = 1),
		endDate = LocalDate(year = 2025, monthNumber = 6, dayOfMonth = 9),
		visible = true,
		onConfirm = {},
		onDismiss = {}
	)
}