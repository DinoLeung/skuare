package xyz.d1n0.ui.screen.alarms

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import xyz.d1n0.Log

@Composable
fun AlarmsScreen(
    innerPadding: PaddingValues,
) {
    val viewModel = koinViewModel<AlarmsScreenViewModel>()
    val log = koinInject<Log>()

    val isInitialized = viewModel.isInitialized.collectAsState(initial = false)
    val hourlySignal = viewModel.hourlySignal.collectAsState(initial = null)
    val alarms = viewModel.alarms.collectAsState(initial = List(4) { null })
    val alarmSnooze = viewModel.snoozeAlarm.collectAsState(initial = null)

    LaunchedEffect(Unit) {
        if (isInitialized.value == false)
            viewModel.requestAlarms()
    }

    Column (
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,

    ) {
        Text(hourlySignal.value.toString(), color = MaterialTheme.colorScheme.onBackground)

        alarms.value.forEach { alarm ->
            alarm?.let {
                Text(it.toString(), color = MaterialTheme.colorScheme.onBackground)
            }
        }
        Text(alarmSnooze.value.toString(), color = MaterialTheme.colorScheme.onBackground)
    }
}