package xyz.d1n0.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import xyz.d1n0.lib.constant.BacklightDuration
import xyz.d1n0.lib.constant.ConnectionTimeout
import xyz.d1n0.lib.constant.DateFormat
import xyz.d1n0.lib.constant.WeekdayLanguage
import xyz.d1n0.ui.component.CardView
import xyz.d1n0.ui.component.EnumDropdown
import xyz.d1n0.ui.component.ScreenContainer
import xyz.d1n0.ui.component.SliderField
import xyz.d1n0.ui.component.SwitchField

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

	ScreenContainer(
		modifier = Modifier
			.fillMaxSize()
			.padding(innerPadding),
		saveVisible = state.hasUpdates &&
				state.loading == false &&
				state.hasErrors == false,
		saveOnClick = { viewModel.onEvent(SettingsUiEvent.SaveSettings) },
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
							enabled = state.isWatchSettingsInitialized && state.loading == false,
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
								enabled = state.isWatchSettingsInitialized && state.loading == false,
								options = DateFormat.values(),
								modifier = Modifier.weight(1f)
							)
							EnumDropdown(
								label = "Weekday Language",
								selectedOption = state.pendingWatchSettings.weekdayLanguage,
								onOptionSelected = {
									viewModel.onEvent(SettingsUiEvent.WeekdayLanguageChange(it))
								},
								enabled = state.isWatchSettingsInitialized && state.loading == false,
								options = WeekdayLanguage.values(),
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
							enabled = state.isWatchSettingsInitialized && state.loading == false,
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
							enabled = state.isWatchSettingsInitialized && state.loading == false,
							options = BacklightDuration.values(),
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
							enabled = state.isConnectionSettingsInitialized && state.loading == false,
							modifier = Modifier.fillMaxWidth()
						)
						SliderField(
							label = "Auto time Adjustment Delay",
							value = state.pendingConnectionSettings.autoSyncDelay.minutes,
							enabled = state.isConnectionSettingsInitialized && state.loading == false,
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
							enabled = state.loading != true,
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
							enabled = state.isWatchSettingsInitialized && state.loading == false,
							modifier = Modifier.fillMaxWidth()
						)
						SwitchField(
							title = "Mute Button Tone",
							check = state.pendingWatchSettings.preferences.isToneMuted,
							onCheckedChange = { viewModel.onEvent(SettingsUiEvent.IsMutedChange(it)) },
							enabled = state.isWatchSettingsInitialized && state.loading == false,
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
							enabled = state.isConnectionSettingsInitialized && state.loading == false,
							options = ConnectionTimeout.values(),
							modifier = Modifier.fillMaxWidth()
						)
					}
				}
			}
		}
	}
}