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

    val reminderTitles = viewModel.reminderTitles.collectAsState(List(5) { null })
    val reminderConfigs = viewModel.reminderConfigs.collectAsState(List(5) { null })

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
            for(i in 0..4) {
                reminderTitles.value[i]?.let {
                    Text(it.toString(), color = MaterialTheme.colorScheme.onBackground)
                }
                reminderConfigs.value[i]?.let {
                    Text(it.toString(), color = MaterialTheme.colorScheme.onBackground)
                }
            }
        }
    }
}