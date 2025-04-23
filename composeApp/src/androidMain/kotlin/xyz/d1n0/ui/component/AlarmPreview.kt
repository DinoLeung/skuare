package xyz.d1n0.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.datetime.LocalTime

@Preview
@Composable
fun AlarmPreview() {
	Column {
		AlarmCard(
			alarm = xyz.d1n0.lib.model.Alarm(true, LocalTime(hour = 23, minute = 59))
		)
		AlarmCard(
			alarm = xyz.d1n0.lib.model.Alarm(false, LocalTime(hour = 11, minute = 0))
		)
		AlarmCard(
			alarm = xyz.d1n0.lib.model.Alarm(false, LocalTime(hour = 7, minute = 30)),
			isSnooze = true
		)
		HourlySignalCard(
			hourlySignal = xyz.d1n0.lib.model.HourlySignal(enable = true)
		)
	}
}