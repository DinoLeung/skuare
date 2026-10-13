package xyz.d1n0.ui.screen.alarms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Snooze
import androidx.compose.material3.Icon
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
import org.koin.compose.viewmodel.koinViewModel
import xyz.d1n0.lib.model.Alarm
import xyz.d1n0.lib.model.HourlySignal
import xyz.d1n0.ui.component.CardView
import xyz.d1n0.ui.component.SaveScreenScaffold
import xyz.d1n0.ui.component.SwitchField
import xyz.d1n0.ui.component.TimePickerField

@Composable
fun AlarmsScreen() {
	val viewModel = koinViewModel<AlarmsScreenViewModel>()
	val state by viewModel.uiState.collectAsState()

	LaunchedEffect(Unit) {
		if (state.isHourlySignalInitialized == false ||
			state.isAlarmsInitialized == false ||
			state.isSnoozeAlarmInitialized == false
		) viewModel.onEvent(AlarmsUiEvent.RequestAlarms)
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
	SaveScreenScaffold(
		saveFabVisible = state.hasUpdates &&
				state.isLoading == false &&
				state.hasErrors == false,
		saveFabOnClick = { onEvent(AlarmsUiEvent.SaveAlarms) }
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
						state.pendingAlarms.zip(state.pendingAlarmsErrors)
							.forEachIndexed { index, (alarm, _) ->
								SwitchField(
									title = {
										TimePickerField(
											value = alarm.time,
											onValueChange = {
												onEvent(
													AlarmsUiEvent.AlarmTimeChange(
														index = index,
														time = it
													)
												)
											},
											leadingIcon = {
												Icon(
													imageVector = Icons.Outlined.Alarm,
													contentDescription = "Alarm"
												)
											},
											enabled = state.isAlarmsInitialized && state.isLoading == false,
											modifier = Modifier.fillMaxWidth()
										)
									},
									check = alarm.enable,
									onCheckedChange = {
										onEvent(
											AlarmsUiEvent.AlarmToggle(
												index = index,
												enable = it
											)
										)
									},
									enabled = state.isAlarmsInitialized && state.isLoading == false,
									modifier = Modifier.fillMaxWidth()
								)
							}
					}
				}
			}
			item {
				CardView(
					modifier = Modifier.fillMaxWidth(),
					title = { Text("Snooze Alarm") }
				) {
					SwitchField(
						title = {
							TimePickerField(
								value = state.pendingSnoozeAlarm.time,
								onValueChange = {
									onEvent(AlarmsUiEvent.SnoozeAlarmTimeChange(time = it))
								},
								leadingIcon = {
									Icon(
										imageVector = Icons.Outlined.Snooze,
										contentDescription = "Snooze Alarm"
									)
								},
								enabled = state.isSnoozeAlarmInitialized && state.isLoading == false,
								modifier = Modifier.fillMaxWidth()
							)
						},
						check = state.pendingSnoozeAlarm.enable,
						onCheckedChange = {
							onEvent(AlarmsUiEvent.SnoozeAlarmToggle(enable = it))
						},
						enabled = state.isSnoozeAlarmInitialized && state.isLoading == false,
						modifier = Modifier.fillMaxWidth()
					)
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
}

@Preview(showBackground = true)
@Composable
private fun AlarmsScreenPreview() {
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
