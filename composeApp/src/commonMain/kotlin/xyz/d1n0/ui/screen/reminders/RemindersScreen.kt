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
import xyz.d1n0.lib.model.Reminder

@Composable
fun RemindersScreen(
    innerPadding: PaddingValues,
) {
    val viewModel = koinViewModel<RemindersScreenViewModel>()
    val log = koinInject<Log>()

    val isTitlesInitialized = viewModel.isTitlesInitialized.collectAsState(initial = false)
    val isConfigsInitialized = viewModel.isConfigsInitialized.collectAsState(initial = false)

    val reminders = viewModel.reminders.collectAsState(List(5) { Reminder() })

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
            reminders.value.forEach { reminder ->
                Text(reminder!!.title.toString(), color = MaterialTheme.colorScheme.onBackground)
                Text(reminder!!.config.toString(), color = MaterialTheme.colorScheme.onBackground)
            }
        }
    }
}