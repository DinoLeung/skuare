package xyz.d1n0.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Snooze
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalTime
import xyz.d1n0.lib.model.Alarm
import xyz.d1n0.ui.theme.SkuareTheme

/**
 * One alarm: its time and an on/off switch.
 *
 * @param label Read out for the icon, e.g. Alarm 3 or Snooze Alarm.
 * @param isSnooze Shows the snooze icon, for the snooze alarm of watches with one, see
 *        [xyz.d1n0.lib.model.WatchFeatures.hasSnoozeAlarm].
 */
@Composable
fun AlarmField(
	alarm: Alarm,
	label: String,
	onTimeChange: (LocalTime) -> Unit,
	onToggle: (Boolean) -> Unit,
	modifier: Modifier = Modifier,
	isSnooze: Boolean = false,
	enabled: Boolean = true,
) {
	SwitchField(
		title = {
			TimePickerField(
				value = alarm.time,
				onValueChange = onTimeChange,
				leadingIcon = {
					Icon(
						imageVector = if (isSnooze) Icons.Outlined.Snooze else Icons.Outlined.Alarm,
						contentDescription = label,
					)
				},
				enabled = enabled,
				modifier = Modifier.fillMaxWidth()
			)
		},
		check = alarm.enable,
		onCheckedChange = onToggle,
		enabled = enabled,
		modifier = modifier.fillMaxWidth()
	)
}

@Preview
@Composable
private fun AlarmFieldPreview() {
	SkuareTheme {
		Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
			AlarmField(
				alarm = Alarm(enable = true, time = LocalTime(hour = 6, minute = 30)),
				label = "Alarm 1",
				onTimeChange = {},
				onToggle = {},
			)
			AlarmField(
				alarm = Alarm(enable = false, time = LocalTime(hour = 0, minute = 10)),
				label = "Snooze Alarm",
				isSnooze = true,
				onTimeChange = {},
				onToggle = {},
			)
			AlarmField(
				alarm = Alarm(enable = true, time = LocalTime(hour = 13, minute = 0)),
				label = "Alarm 5",
				enabled = false,
				onTimeChange = {},
				onToggle = {},
			)
		}
	}
}
