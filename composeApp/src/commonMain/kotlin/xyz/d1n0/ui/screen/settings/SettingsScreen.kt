package xyz.d1n0.ui.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsScreen(
    innerPadding: PaddingValues,
) {
    val viewModel = koinViewModel<SettingsScreenViewModel>()
    val isNameInitialized = viewModel.isNameInitialized.collectAsState(initial = false)
    val isWatchSettingsInitialized = viewModel.isWatchSettingsInitialized.collectAsState(initial = false)
    val isConnectionSettingsInitialized = viewModel.isConnectionSettingsInitialized.collectAsState(initial = false)

    LaunchedEffect(Unit) {
        if (isNameInitialized.value == false)
            viewModel.requestName()
        if (isWatchSettingsInitialized.value == false)
            viewModel.requestWatchSettings()
        if (isConnectionSettingsInitialized.value == false)
            viewModel.requestConnectionSettings()
    }

//    val name by remember(viewModel.info.name) { mutableStateOf(viewModel.info.name) }

    Column (
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (isNameInitialized.value) {
            Text(viewModel.info.name.value, color = MaterialTheme.colorScheme.onBackground)
            Button(onClick = viewModel::writeName) {
                Text("WRITE NAME", color = MaterialTheme.colorScheme.onBackground)
            }
        }
        if (isWatchSettingsInitialized.value)
            Text(viewModel.info.watchSettings.toString(), color = MaterialTheme.colorScheme.onBackground)
        if (isConnectionSettingsInitialized.value)
            Text(viewModel.info.connectionSettings.toString(), color = MaterialTheme.colorScheme.onBackground)
    }
}