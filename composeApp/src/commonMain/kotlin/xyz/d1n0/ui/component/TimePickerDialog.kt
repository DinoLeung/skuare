package xyz.d1n0.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import kotlinx.datetime.LocalTime
import androidx.compose.ui.tooling.preview.Preview
import xyz.d1n0.ui.theme.SkuareTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
	time: LocalTime,
	visible: Boolean,
	onConfirm: (LocalTime) -> Unit,
	onDismiss: () -> Unit,
) {
	if (visible) {
		val state = rememberTimePickerState(
			initialHour = time.hour,
			initialMinute = time.minute,
			is24Hour = true
		)

		BasicAlertDialog(
			onDismissRequest = onDismiss,
			modifier = Modifier.wrapContentSize(),
			properties = DialogProperties()
		) {
			Surface(
				modifier = Modifier.wrapContentSize(),
				shape = MaterialTheme.shapes.large,
				color = AlertDialogDefaults.containerColor,
				tonalElevation = AlertDialogDefaults.TonalElevation
			) {
				Column(modifier = Modifier.padding(16.dp)) {
					TimePicker(state = state)
					Spacer(modifier = Modifier.height(24.dp))
					TextButton(
						onClick = {
							onConfirm(LocalTime(hour = state.hour, minute = state.minute))
						},
						modifier = Modifier.align(Alignment.End)
					) { Text("Confirm") }
				}
			}
		}
	}
}


@Preview(showBackground = true, widthDp = 420, heightDp = 720)
@Composable
private fun TimePicketDialogPreview() {
	SkuareTheme {
		TimePickerDialog(
			time = LocalTime(hour = 10, minute = 10),
			visible = true,
			onConfirm = { },
			onDismiss = { }
		)
	}
}
