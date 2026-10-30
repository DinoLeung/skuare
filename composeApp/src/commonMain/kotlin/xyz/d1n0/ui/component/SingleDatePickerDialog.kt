package xyz.d1n0.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.datetime.LocalDate
import xyz.d1n0.lib.helper.fromMillis
import xyz.d1n0.lib.helper.toMillis
import xyz.d1n0.ui.boilerplate.DatePickerCustomerFormatter
import xyz.d1n0.ui.theme.SkuareTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleDatePickerDialog(
	date: LocalDate,
	visible: Boolean,
	onConfirm: (LocalDate) -> Unit,
	onDismiss: () -> Unit,
) {
	if (visible) {
		// created on open so the picker starts from the current date, not the one from the first composition
		val state = rememberDatePickerState(
			initialSelectedDateMillis = date.toMillis(),
			yearRange = IntRange(2000, 2099)
		)

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
			dismissButton = { TextButton(onClick = { onDismiss() }) { Text("Cancel") } },
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

@Preview(showBackground = true, widthDp = 420, heightDp = 720)
@Composable
private fun SingleDatePickerDialogPreview() {
	SkuareTheme {
		SingleDatePickerDialog(
			date = LocalDate(
				year = 2025,
				month = 6,
				day = 9
			),
			visible = true,
			onConfirm = { },
			onDismiss = { },
		)
	}
}
