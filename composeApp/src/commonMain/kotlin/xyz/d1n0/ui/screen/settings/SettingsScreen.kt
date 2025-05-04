package xyz.d1n0.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import org.koin.compose.viewmodel.koinViewModel
import xyz.d1n0.ui.component.CardView

@Composable
fun SettingsScreen(
	innerPadding: PaddingValues,
) {
	val viewModel = koinViewModel<SettingsScreenViewModel>()
	val state by viewModel.uiState.collectAsState()

	LaunchedEffect(Unit) {
		if (state.isNameInitialized == false) viewModel.onEvent(SettingsUiEvent.RequestName)
		if (state.isWatchSettingsInitialized == false) viewModel.onEvent(SettingsUiEvent.RequestWatchSettings)
		if (state.isConnectionSettingsInitialized == false) viewModel.onEvent(SettingsUiEvent.RequestConnectionSettings)
	}

	var nameFieldValue by remember { mutableStateOf(TextFieldValue(text = state.savedName.value)) }
	LaunchedEffect(state.savedName) {
		nameFieldValue = nameFieldValue.copy(state.savedName.value)
	}


	Column(
		modifier = Modifier.fillMaxSize().padding(innerPadding),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.Center,
	) {
		OutlinedTextField(
			label = { Text("Name") },
			value = nameFieldValue,
			enabled = state.waitingUpdates != true,
			isError = state.pendingNameError != null,
			supportingText = {
				state.pendingNameError?.let {
					Text(it.message ?: "Unknown errors")
				}
			},
			onValueChange = {
				nameFieldValue = it
				viewModel.onEvent(SettingsUiEvent.NameInputChange(it.text))
			}
		)

		CardView(
			leadingIcon = {},
			title = {
				Text(
					text = "24 Hour format",
					textAlign = TextAlign.Start,
					style = MaterialTheme.typography.bodyMedium
				)
			},
			indicator = {
				Switch(
					checked = state.pendingWatchSettings.preferences.is24HourTime,
					onCheckedChange = { viewModel.onEvent(SettingsUiEvent.Is24HourChange(it)) },
					enabled = true,
				)
			},
			content = {},
		)

		Text(state.savedWatchSettings.toString(), color = MaterialTheme.colorScheme.onBackground)
		Text(
			state.savedConnectionSettings.toString(),
			color = MaterialTheme.colorScheme.onBackground
		)
	}
}