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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
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

	SaveScreenScaffold(
		saveFabVisible = state.hasUpdates &&
				state.isLoading == false &&
				state.hasErrors == false,
		saveFabOnClick = { viewModel.onEvent(AlarmsUiEvent.SaveAlarms) }
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
							.forEachIndexed { index, (alarm, err) ->
								SwitchField(
									title = {
										TimePickerField(
											value = alarm.time,
											onValueChange = {
												viewModel.onEvent(
													AlarmsUiEvent.AlarmTimeChange(
														index = index,
														time = it
													)
												)
											},
											trailingIcon = {
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
										viewModel.onEvent(
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
									viewModel.onEvent(AlarmsUiEvent.SnoozeAlarmTimeChange(time = it))
								}, trailingIcon = {
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
							viewModel.onEvent(AlarmsUiEvent.SnoozeAlarmToggle(enable = it))
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
							viewModel.onEvent(AlarmsUiEvent.HourlySignalToggle(enable = it))
						},
						enabled = state.isHourlySignalInitialized && state.isLoading == false,
						modifier = Modifier.fillMaxWidth()
					)
				}
			}
		}
	}
}