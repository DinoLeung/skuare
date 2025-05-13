package xyz.d1n0.ui.screen.alarms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import xyz.d1n0.Log
import xyz.d1n0.ui.component.AlarmCard
import xyz.d1n0.ui.component.HourlySignalCard

@Composable
fun AlarmsScreen() {
	val viewModel = koinViewModel<AlarmsScreenViewModel>()
	val log = koinInject<Log>()

	val isInitialized = viewModel.isInitialized.collectAsState(initial = false)
	val hourlySignal = viewModel.hourlySignal.collectAsState(initial = null)
	val alarms = viewModel.alarms.collectAsState(initial = List(4) { null })
	val alarmSnooze = viewModel.snoozeAlarm.collectAsState(initial = null)

	LaunchedEffect(Unit) {
		if (isInitialized.value == false) viewModel.requestAlarms()
	}

	Column(
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.Center,
		modifier = Modifier.fillMaxSize()
			.verticalScroll(rememberScrollState()),
	) {
		alarms.value.forEach { alarm ->
			alarm?.let {
				AlarmCard(alarm = it)
			}
		}
		alarmSnooze.value?.let {
			AlarmCard(alarm = it, isSnooze = true)
		}
		hourlySignal.value?.let {
			HourlySignalCard(hourlySignal = it)
		}
	}
}