package xyz.d1n0.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import xyz.d1n0.lib.constant.AutoSyncDelay
import xyz.d1n0.lib.constant.BacklightDuration
import xyz.d1n0.lib.constant.ConnectionTimeout
import xyz.d1n0.lib.constant.DateFormat
import xyz.d1n0.lib.constant.WeekdayLanguage
import xyz.d1n0.ui.component.EnumDropdown
import xyz.d1n0.ui.component.SwitchCard

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

	LazyColumn(
		state = rememberLazyListState(),
		modifier = Modifier
			.fillMaxSize()
			.padding(innerPadding),
		contentPadding = PaddingValues(8.dp),
//		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.spacedBy(8.dp),
	) {
		item {
			OutlinedTextField(
				label = { Text("Watch Name") },
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
				},
				modifier = Modifier.fillMaxWidth()
			)
		}

		item {
			SwitchCard(
				title = "24 Hour format",
				check = state.pendingWatchSettings.preferences.is24HourTime,
				onCheckedChange = { viewModel.onEvent(SettingsUiEvent.Is24HourChange(it)) },
				enabled = state.isWatchSettingsInitialized && state.waitingUpdates == false,
				modifier = Modifier.fillMaxWidth()
			)
		}

		item {
			EnumDropdown(
				label = "Date Format",
				selectedOption = state.pendingWatchSettings.dateFormat,
				onOptionSelected = { viewModel.onEvent(SettingsUiEvent.DateFormatChange(it)) },
				enabled = state.isWatchSettingsInitialized && state.waitingUpdates == false,
				options = DateFormat.values(),
				modifier = Modifier.fillMaxWidth()
			)
		}

		item {
			EnumDropdown(
				label = "Weekday Language",
				selectedOption = state.pendingWatchSettings.weekdayLanguage,
				onOptionSelected = { viewModel.onEvent(SettingsUiEvent.WeekdayLanguageChange(it)) },
				enabled = state.isWatchSettingsInitialized && state.waitingUpdates == false,
				options = WeekdayLanguage.values(),
				modifier = Modifier.fillMaxWidth()
			)
		}

		item {
			SwitchCard(
				title = "Mute Button Tone",
				check = state.pendingWatchSettings.preferences.isToneMuted,
				onCheckedChange = { viewModel.onEvent(SettingsUiEvent.IsMutedChange(it)) },
				enabled = state.isWatchSettingsInitialized && state.waitingUpdates == false,
				modifier = Modifier.fillMaxWidth()
			)
		}

		item {
			SwitchCard(
				title = "Auto Backlight",
				check = state.pendingWatchSettings.preferences.autoBacklight,
				onCheckedChange = { viewModel.onEvent(SettingsUiEvent.AutoBacklightChange(it)) },
				enabled = state.isWatchSettingsInitialized && state.waitingUpdates == false,
				modifier = Modifier.fillMaxWidth()
			)
		}

		item {
			EnumDropdown(
				label = "Backlight Duration",
				selectedOption = state.pendingWatchSettings.backlightDuration,
				onOptionSelected = { viewModel.onEvent(SettingsUiEvent.BacklightDurationChange(it)) },
				enabled = state.isWatchSettingsInitialized && state.waitingUpdates == false,
				options = BacklightDuration.values(),
				modifier = Modifier.fillMaxWidth()
			)
		}

		item {
			SwitchCard(
				title = "Power Saving Mode",
				check = state.pendingWatchSettings.preferences.powerSaving,
				onCheckedChange = { viewModel.onEvent(SettingsUiEvent.PowerSavingChange(it)) },
				enabled = state.isWatchSettingsInitialized && state.waitingUpdates == false,
				modifier = Modifier.fillMaxWidth()
			)
		}

		item {
			SwitchCard(
				title = "Auto Time Adjustment",
				check = state.pendingConnectionSettings.autoSyncEnable,
				onCheckedChange = { viewModel.onEvent(SettingsUiEvent.AutoSyncChange(it)) },
				enabled = state.isConnectionSettingsInitialized && state.waitingUpdates == false,
				modifier = Modifier.fillMaxWidth()
			)
		}

		item {
			EnumDropdown(
				label = "Auto time Adjustment Delay",
				selectedOption = state.pendingConnectionSettings.autoSyncDelay,
				onOptionSelected = { viewModel.onEvent(SettingsUiEvent.AutoSyncDelayChange(it)) },
				enabled = state.isConnectionSettingsInitialized && state.waitingUpdates == false,
				options = AutoSyncDelay.values(),
				modifier = Modifier.fillMaxWidth()
			)
		}

		item {
			EnumDropdown(
				label = "Connection Timeout",
				selectedOption = state.pendingConnectionSettings.connectionTimeout,
				onOptionSelected = { viewModel.onEvent(SettingsUiEvent.ConnectionTimeoutChange(it)) },
				enabled = state.isConnectionSettingsInitialized && state.waitingUpdates == false,
				options = ConnectionTimeout.values(),
				modifier = Modifier.fillMaxWidth()
			)
		}
	}
}