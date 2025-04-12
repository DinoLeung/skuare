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

    val isAlarmsInitialized = viewModel.isInitialized.collectAsState(initial = false)

    LaunchedEffect(Unit) {
        if (isAlarmsInitialized.value == false)
            viewModel.requestAlarms()
    }

    Column (
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,

    ) {
        if (isAlarmsInitialized.value == true) {
            Text(viewModel.alarms.hourlySignal.toString(), color = MaterialTheme.colorScheme.onBackground)
            Text(viewModel.alarms.alarm1.toString(), color = MaterialTheme.colorScheme.onBackground)
            Text(viewModel.alarms.alarm2.toString(), color = MaterialTheme.colorScheme.onBackground)
            Text(viewModel.alarms.alarm3.toString(), color = MaterialTheme.colorScheme.onBackground)
            Text(viewModel.alarms.alarm4.toString(), color = MaterialTheme.colorScheme.onBackground)
            Text(viewModel.alarms.alarmSnooze.toString(), color = MaterialTheme.colorScheme.onBackground)
        }
    }
}