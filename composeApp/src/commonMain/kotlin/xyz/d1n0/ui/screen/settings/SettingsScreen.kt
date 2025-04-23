package xyz.d1n0.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsScreen(
	innerPadding: PaddingValues,
) {
	val viewModel = koinViewModel<SettingsScreenViewModel>()
	val name = viewModel.name.collectAsState()
	val watchSettings = viewModel.watchSettings.collectAsState()
	val connectionSettings = viewModel.connectionSettings.collectAsState()

	LaunchedEffect(Unit) {
		if (name.value == null) viewModel.requestName()
		if (watchSettings.value == null) viewModel.requestWatchSettings()
		if (connectionSettings.value == null) viewModel.requestConnectionSettings()
	}

	Column(
		modifier = Modifier.fillMaxSize().padding(innerPadding),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.Center,
	) {
		Text(name.value.toString(), color = MaterialTheme.colorScheme.onBackground)
		Button(onClick = viewModel::writeName) {
			Text("WRITE NAME", color = MaterialTheme.colorScheme.onBackground)
		}
		Text(watchSettings.value.toString(), color = MaterialTheme.colorScheme.onBackground)
		Text(connectionSettings.value.toString(), color = MaterialTheme.colorScheme.onBackground)
	}
}