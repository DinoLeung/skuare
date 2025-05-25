package xyz.d1n0.ui.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import xyz.d1n0.lib.constant.BacklightDuration
import xyz.d1n0.lib.constant.ConnectionTimeout
import xyz.d1n0.lib.constant.DateFormat
import xyz.d1n0.lib.constant.WeekdayLanguage
import xyz.d1n0.ui.component.*

@Composable
fun SettingsScreen() {
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

	SaveScreenScaffold(
		saveFabVisible = state.hasUpdates &&
				state.isLoading == false &&
				state.hasErrors == false,
		saveFabOnClick = { viewModel.onEvent(SettingsUiEvent.SaveSettings) },
	) {
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
							onCheckedChange = { viewModel.onEvent(SettingsUiEvent.Is24HourChange(it)) },
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
									viewModel.onEvent(SettingsUiEvent.DateFormatChange(it))
								},
								enabled = state.isWatchSettingsInitialized && state.isLoading == false,
								options = DateFormat.values().toSet(),
								modifier = Modifier.weight(1f)
							)
							EnumDropdown(
								label = "Weekday Language",
								selectedOption = state.pendingWatchSettings.weekdayLanguage,
								onOptionSelected = {
									viewModel.onEvent(SettingsUiEvent.WeekdayLanguageChange(it))
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
								viewModel.onEvent(
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
								viewModel.onEvent(
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
							onCheckedChange = { viewModel.onEvent(SettingsUiEvent.AutoSyncChange(it)) },
							enabled = state.isConnectionSettingsInitialized && state.isLoading == false,
							modifier = Modifier.fillMaxWidth()
						)
						SliderField(
							label = "Auto time Adjustment Delay",
							value = state.pendingConnectionSettings.autoSyncDelay.minutes,
							enabled = state.isConnectionSettingsInitialized && state.isLoading == false,
							range = 0..59,
							onValueChange = {
								viewModel.onEvent(
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
								viewModel.onEvent(SettingsUiEvent.NameInputChange(it.text))
							},
							modifier = Modifier.fillMaxWidth()
						)
						SwitchField(
							title = "Power Saving Mode",
							check = state.pendingWatchSettings.preferences.powerSaving,
							onCheckedChange = {
								viewModel.onEvent(
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
							onCheckedChange = { viewModel.onEvent(SettingsUiEvent.IsMutedChange(it)) },
							enabled = state.isWatchSettingsInitialized && state.isLoading == false,
							modifier = Modifier.fillMaxWidth()
						)
						EnumDropdown(
							label = "Connection Timeout",
							selectedOption = state.pendingConnectionSettings.connectionTimeout,
							onOptionSelected = {
								viewModel.onEvent(
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
}