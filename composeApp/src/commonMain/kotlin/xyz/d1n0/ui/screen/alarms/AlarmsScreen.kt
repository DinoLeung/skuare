package xyz.d1n0.ui.screen.alarms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalTime
import xyz.d1n0.lib.model.Alarm
import xyz.d1n0.lib.model.HourlySignal
import xyz.d1n0.ui.component.AlarmField
import xyz.d1n0.ui.component.CardView
import xyz.d1n0.ui.component.SwitchField
import xyz.d1n0.ui.theme.SkuareTheme

@Composable
fun AlarmsScreen(viewModel: AlarmsScreenViewModel) {
	val state by viewModel.uiState.collectAsState()

	LaunchedEffect(Unit) {
		if (state.isInitialized == false) viewModel.onEvent(AlarmsUiEvent.RequestAlarms)
	}

	AlarmsScreenContent(
		state = state,
		onEvent = { viewModel.onEvent(it) },
	)
}

@Composable
private fun AlarmsScreenContent(
	state: AlarmsUiState,
	onEvent: (AlarmsUiEvent) -> Unit,
) {
	LazyColumn(
		state = rememberLazyListState(),
		modifier = Modifier.fillMaxSize(),
		contentPadding = PaddingValues(8.dp),
		verticalArrangement = Arrangement.spacedBy(8.dp),
	) {
		item {
			CardView(
				modifier = Modifier.fillMaxWidth(),
				title = { Text("Alarms") }
			) {
				Column(
					modifier = Modifier.fillMaxWidth(),
					verticalArrangement = Arrangement.spacedBy(8.dp)
				) {
					state.pendingAlarms.forEachIndexed { index, alarm ->
						AlarmField(
							alarm = alarm,
							label = "Alarm ${index + 1}",
							enabled = state.isAlarmsInitialized && state.isLoading == false,
							onTimeChange = { onEvent(AlarmsUiEvent.AlarmTimeChange(index = index, time = it)) },
							onToggle = { onEvent(AlarmsUiEvent.AlarmToggle(index = index, enable = it)) },
						)
					}
					// without a snooze alarm, the fifth slot is a normal alarm
					if (state.hasSnoozeAlarm == false) {
						FifthAlarmField(state = state, onEvent = onEvent)
					}
				}
			}
		}
		if (state.hasSnoozeAlarm) {
			item {
				CardView(
					modifier = Modifier.fillMaxWidth(),
					title = { Text("Snooze Alarm") }
				) {
					FifthAlarmField(state = state, onEvent = onEvent)
				}
			}
		}
		item {
			CardView(
				modifier = Modifier.fillMaxWidth(),
				title = { Text("Others") }
			) {
				SwitchField(
					title = "Hourly Signal",
					check = state.pendingHourlySignal.enable,
					onCheckedChange = {
						onEvent(AlarmsUiEvent.HourlySignalToggle(enable = it))
					},
					enabled = state.isHourlySignalInitialized && state.isLoading == false,
					modifier = Modifier.fillMaxWidth()
				)
			}
		}
	}
}

/**
 * The fifth alarm slot, the snooze alarm on watches with one, see [AlarmsUiState.hasSnoozeAlarm].
 * It keeps its own state and events either way, the packets are the same.
 */
@Composable
private fun FifthAlarmField(
	state: AlarmsUiState,
	onEvent: (AlarmsUiEvent) -> Unit,
) = AlarmField(
	alarm = state.pendingSnoozeAlarm,
	label = if (state.hasSnoozeAlarm) "Snooze Alarm" else "Alarm ${state.pendingAlarms.size + 1}",
	isSnooze = state.hasSnoozeAlarm,
	enabled = state.isSnoozeAlarmInitialized && state.isLoading == false,
	onTimeChange = { onEvent(AlarmsUiEvent.SnoozeAlarmTimeChange(time = it)) },
	onToggle = { onEvent(AlarmsUiEvent.SnoozeAlarmToggle(enable = it)) },
)

@Preview(showBackground = true)
@Composable
private fun AlarmsScreenWithoutSnoozePreview() {
	SkuareTheme {
		val alarm = Alarm(enable = false, time = LocalTime(hour = 0, minute = 0))
		val fifthAlarm = Alarm(enable = true, time = LocalTime(hour = 13, minute = 0))
		MaterialTheme {
			AlarmsScreenContent(
				state = AlarmsUiState(
					hasSnoozeAlarm = false,
					isHourlySignalInitialized = true,
					isAlarmsInitialized = true,
					savedAlarms = List(4) { alarm },
					pendingAlarms = List(4) { alarm },
					isSnoozeAlarmInitialized = true,
					savedSnoozeAlarm = fifthAlarm,
					pendingSnoozeAlarm = fifthAlarm,
				),
				onEvent = {},
			)
		}
	}
}

@Preview(showBackground = true)
@Composable
private fun AlarmsScreenPreview() {
	SkuareTheme {
		val alarms = listOf(
			Alarm(enable = true, time = LocalTime(hour = 6, minute = 30)),
			Alarm(enable = false, time = LocalTime(hour = 7, minute = 0)),
			Alarm(enable = true, time = LocalTime(hour = 8, minute = 15)),
			Alarm(enable = false, time = LocalTime(hour = 9, minute = 45)),
		)
		val snoozeAlarm = Alarm(enable = true, time = LocalTime(hour = 0, minute = 10))

		MaterialTheme {
			AlarmsScreenContent(
				state = AlarmsUiState(
					isHourlySignalInitialized = true,
					isHourlySignalLoading = false,
					savedHourlySignal = HourlySignal(enable = false),
					pendingHourlySignal = HourlySignal(enable = true),
					isAlarmsInitialized = true,
					isAlarmsLoading = false,
					savedAlarms = alarms,
					pendingAlarms = alarms,
					isSnoozeAlarmInitialized = true,
					isSnoozeAlarmLoading = false,
					savedSnoozeAlarm = snoozeAlarm,
					pendingSnoozeAlarm = snoozeAlarm,
				),
				onEvent = {},
			)
		}
	}
}
