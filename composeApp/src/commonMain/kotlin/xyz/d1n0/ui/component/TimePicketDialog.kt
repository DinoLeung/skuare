package xyz.d1n0.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import kotlinx.datetime.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePicketDialog(
	time: LocalTime,
	visible: Boolean,
	onConfirm: (LocalTime) -> Unit,
	onDismiss: () -> Unit,
) {
	val state = rememberTimePickerState(
		initialHour = time.hour,
		initialMinute = time.minute,
		is24Hour = true
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