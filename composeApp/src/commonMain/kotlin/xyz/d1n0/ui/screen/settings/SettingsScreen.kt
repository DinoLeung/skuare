package xyz.d1n0.ui.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import xyz.d1n0.lib.constant.BacklightDuration
import xyz.d1n0.lib.constant.ConnectionTimeout
import xyz.d1n0.lib.constant.DateFormat
import xyz.d1n0.lib.constant.WeekdayLanguage
import xyz.d1n0.ui.component.*

@Composable
fun SettingsScreen(viewModel: SettingsScreenViewModel) {
	val state by viewModel.uiState.collectAsState()

	LaunchedEffect(Unit) {
		if (state.isNameInitialized == false) viewModel.onEvent(SettingsUiEvent.RequestName)
		if (state.isWatchSettingsInitialized == false) viewModel.onEvent(SettingsUiEvent.RequestWatchSettings)
		if (state.isConnectionSettingsInitialized == false) viewModel.onEvent(SettingsUiEvent.RequestConnectionSettings)
	}

	SettingsScreenContent(
		state = state,
		onEvent = { viewModel.onEvent(it) },
	)
}

@Composable
private fun SettingsScreenContent(
	state: SettingsUiState,
	onEvent: (SettingsUiEvent) -> Unit,
) {
	// Seed from pendingName so unsaved edits survive switching tabs
	var nameFieldValue by remember { mutableStateOf(TextFieldValue(text = state.pendingName.value)) }
	LaunchedEffect(state.pendingName) {
		if (nameFieldValue.text != state.pendingName.value)
			nameFieldValue = nameFieldValue.copy(state.pendingName.value)
	}

	LazyColumn(
		state = rememberLazyListState(),
		modifier = Modifier.fillMaxSize(),
		contentPadding = PaddingValues(8.dp),
		verticalArrangement = Arrangement.spacedBy(8.dp),
	) {
		item {
			CardView(
				modifier = Modifier.fillMaxWidth(),
				title = { Text("Display") }
			) {
				Column(
					modifier = Modifier.fillMaxWidth(),
					verticalArrangement = Arrangement.spacedBy(8.dp)
				) {
					SwitchField(
						title = "24 Hour format",
						check = state.pendingWatchSettings.preferences.is24HourTime,
						onCheckedChange = { onEvent(SettingsUiEvent.Is24HourChange(it)) },
						enabled = state.isWatchSettingsInitialized && state.isLoading == false,
						modifier = Modifier.fillMaxWidth()
					)
					Row(
						modifier = Modifier.fillMaxWidth(),
						horizontalArrangement = Arrangement.spacedBy(8.dp)
					) {
						EnumDropdown(
							label = "Date Format",
							selectedOption = state.pendingWatchSettings.dateFormat,
							onOptionSelected = {
								onEvent(SettingsUiEvent.DateFormatChange(it))
							},
							enabled = state.isWatchSettingsInitialized && state.isLoading == false,
							options = DateFormat.values().toSet(),
							modifier = Modifier.weight(1f)
						)
						EnumDropdown(
							label = "Weekday Language",
							selectedOption = state.pendingWatchSettings.weekdayLanguage,
							onOptionSelected = {
								onEvent(SettingsUiEvent.WeekdayLanguageChange(it))
							},
							enabled = state.isWatchSettingsInitialized && state.isLoading == false,
							options = WeekdayLanguage.values().toSet(),
							modifier = Modifier.weight(1f)
						)
					}
				}
			}
		}

		item {
			CardView(
				modifier = Modifier.fillMaxWidth(),
				title = { Text("Backlight") }
			) {
				Column(
					modifier = Modifier.fillMaxWidth(),
					verticalArrangement = Arrangement.spacedBy(8.dp)
				) {
					SwitchField(
						title = "Auto Backlight",
						check = state.pendingWatchSettings.preferences.autoBacklight,
						onCheckedChange = {
							onEvent(
								SettingsUiEvent.AutoBacklightChange(
									it
								)
							)
						},
						enabled = state.isWatchSettingsInitialized && state.isLoading == false,
						modifier = Modifier.fillMaxWidth(),
					)
					EnumDropdown(
						label = "Backlight Duration",
						selectedOption = state.pendingWatchSettings.backlightDuration,
						onOptionSelected = {
							onEvent(
								SettingsUiEvent.BacklightDurationChange(
									it
								)
							)
						},
						enabled = state.isWatchSettingsInitialized && state.isLoading == false,
						options = BacklightDuration.values().toSet(),
						modifier = Modifier.fillMaxWidth()
					)
				}
			}
		}

		item {
			CardView(
				modifier = Modifier.fillMaxWidth(),
				title = { Text(("Timer Adjustment")) }
			) {
				Column {
					SwitchField(
						title = "Auto Time Adjustment",
						check = state.pendingConnectionSettings.autoSyncEnable,
						onCheckedChange = { onEvent(SettingsUiEvent.AutoSyncChange(it)) },
						enabled = state.isConnectionSettingsInitialized && state.isLoading == false,
						modifier = Modifier.fillMaxWidth()
					)
					SliderField(
						label = "Auto time Adjustment Delay",
						value = state.pendingConnectionSettings.autoSyncDelay.minutes,
						enabled = state.isConnectionSettingsInitialized && state.isLoading == false,
						range = 0..59,
						onValueChange = {
							onEvent(
								SettingsUiEvent.AutoSyncDelayChange(
									it
								)
							)
						},
						error = state.pendingConnectionSettingsError,
						modifier = Modifier.fillMaxWidth(),
					)
				}
			}
		}

		item {
			CardView(
				modifier = Modifier.fillMaxWidth(),
				title = { Text("Others") }
			) {
				Column(
					modifier = Modifier.fillMaxWidth(),
					verticalArrangement = Arrangement.spacedBy(8.dp)
				) {
					OutlinedTextField(
						label = { Text("Watch Name") },
						value = nameFieldValue,
						enabled = state.isLoading != true,
						isError = state.pendingNameError != null,
						supportingText = {
							state.pendingNameError?.let {
								Text(it.message ?: "Unknown errors")
							}
						},
						onValueChange = {
							nameFieldValue = it
							onEvent(SettingsUiEvent.NameInputChange(it.text))
						},
						modifier = Modifier.fillMaxWidth()
					)
					SwitchField(
						title = "Power Saving Mode",
						check = state.pendingWatchSettings.preferences.powerSaving,
						onCheckedChange = {
							onEvent(
								SettingsUiEvent.PowerSavingChange(
									it
								)
							)
						},
						enabled = state.isWatchSettingsInitialized && state.isLoading == false,
						modifier = Modifier.fillMaxWidth()
					)
					SwitchField(
						title = "Mute Button Tone",
						check = state.pendingWatchSettings.preferences.isToneMuted,
						onCheckedChange = { onEvent(SettingsUiEvent.IsMutedChange(it)) },
						enabled = state.isWatchSettingsInitialized && state.isLoading == false,
						modifier = Modifier.fillMaxWidth()
					)
					EnumDropdown(
						label = "Connection Timeout",
						selectedOption = state.pendingConnectionSettings.connectionTimeout,
						onOptionSelected = {
							onEvent(
								SettingsUiEvent.ConnectionTimeoutChange(
									it
								)
							)
						},
						enabled = state.isConnectionSettingsInitialized && state.isLoading == false,
						options = ConnectionTimeout.values().toSet(),
						modifier = Modifier.fillMaxWidth()
					)
				}
			}
		}
	}
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
	MaterialTheme {
		SettingsScreenContent(
			state = SettingsUiState(
				isNameInitialized = true,
				isNameLoading = false,
				isWatchSettingsInitialized = true,
				isWatchSettingsLoading = false,
				isConnectionSettingsInitialized = true,
				isConnectionSettingsLoading = false,
			),
			onEvent = {},
		)
	}
}
