package xyz.d1n0.ui.screen.reminders

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
fun RemindersScreen(
    innerPadding: PaddingValues,
) {
    val viewModel = koinViewModel<RemindersScreenViewModel>()
    val log = koinInject<Log>()

    val isTitlesInitialized = viewModel.isTitlesInitialized.collectAsState(initial = false)
    val isConfigsInitialized = viewModel.isConfigsInitialized.collectAsState(initial = false)

    LaunchedEffect(Unit) {
        if (isTitlesInitialized.value == false)
            viewModel.requestTitles()
        if (isConfigsInitialized.value == false)
            viewModel.requestConfigs()
    }

    Column (
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (isTitlesInitialized.value == true && isConfigsInitialized.value == true) {
            Text(viewModel.reminders.reminderTitle1.toString(), color = MaterialTheme.colorScheme.onBackground)
            Text(viewModel.reminders.reminderConfig1.toString(), color = MaterialTheme.colorScheme.onBackground)
            Text(viewModel.reminders.reminderTitle2.toString(), color = MaterialTheme.colorScheme.onBackground)
            Text(viewModel.reminders.reminderConfig2.toString(), color = MaterialTheme.colorScheme.onBackground)
            Text(viewModel.reminders.reminderTitle3.toString(), color = MaterialTheme.colorScheme.onBackground)
            Text(viewModel.reminders.reminderConfig3.toString(), color = MaterialTheme.colorScheme.onBackground)
            Text(viewModel.reminders.reminderTitle4.toString(), color = MaterialTheme.colorScheme.onBackground)
            Text(viewModel.reminders.reminderConfig4.toString(), color = MaterialTheme.colorScheme.onBackground)
            Text(viewModel.reminders.reminderTitle5.toString(), color = MaterialTheme.colorScheme.onBackground)
            Text(viewModel.reminders.reminderConfig5.toString(), color = MaterialTheme.colorScheme.onBackground)
        }
    }
}