package xyz.d1n0.ui.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.OutlinedButton
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
import xyz.d1n0.lib.constant.FontStyle
import xyz.d1n0.lib.constant.WeekdayLanguage
import xyz.d1n0.ui.component.*
import xyz.d1n0.ui.text.label
import xyz.d1n0.ui.theme.SkuareTheme
import org.jetbrains.compose.resources.stringResource
import xyz.d1n0.resources.*

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
	var showHandGroupDialog by remember { mutableStateOf(false) }

	LazyColumn(
		state = rememberLazyListState(),
		modifier = Modifier.fillMaxSize(),
		contentPadding = PaddingValues(8.dp),
		verticalArrangement = Arrangement.spacedBy(8.dp),
	) {
		item {
			CardView(
				modifier = Modifier.fillMaxWidth(),
				title = { Text(stringResource(Res.string.settings_display)) }
			) {
				Column(
					modifier = Modifier.fillMaxWidth(),
					verticalArrangement = Arrangement.spacedBy(8.dp)
				) {
					SwitchField(
						title = stringResource(Res.string.settings_24_hour_format),
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
							label = stringResource(Res.string.settings_date_format),
							selectedOption = state.pendingWatchSettings.dateFormat,
							optionLabel = { it.label },
							onOptionSelected = {
								onEvent(SettingsUiEvent.DateFormatChange(it))
							},
							enabled = state.isWatchSettingsInitialized && state.isLoading == false,
							options = DateFormat.values().toSet(),
							modifier = Modifier.weight(1f)
						)
						if (state.hasWeekdayLanguage) {
							EnumDropdown(
								label = stringResource(Res.string.settings_weekday_language),
								selectedOption = state.pendingWatchSettings.weekdayLanguage,
								optionLabel = { stringResource(it.label) },
								onOptionSelected = {
									onEvent(SettingsUiEvent.WeekdayLanguageChange(it))
								},
								enabled = state.isWatchSettingsInitialized && state.isLoading == false,
								options = WeekdayLanguage.values().toSet(),
								modifier = Modifier.weight(1f)
							)
						}
					}
					if (state.hasFontStyle) {
						EnumDropdown(
							label = stringResource(Res.string.settings_font_style),
							selectedOption = state.pendingWatchSettings.fontStyle,
							optionLabel = { stringResource(it.label) },
							onOptionSelected = {
								onEvent(SettingsUiEvent.FontStyleChange(it))
							},
							enabled = state.isWatchSettingsInitialized && state.isLoading == false,
							options = FontStyle.values().toSet(),
							modifier = Modifier.fillMaxWidth()
						)
					}
				}
			}
		}

		item {
			CardView(
				modifier = Modifier.fillMaxWidth(),
				title = { Text(stringResource(Res.string.settings_backlight)) }
			) {
				Column(
					modifier = Modifier.fillMaxWidth(),
					verticalArrangement = Arrangement.spacedBy(8.dp)
				) {
					SwitchField(
						title = stringResource(Res.string.settings_auto_backlight),
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
						label = stringResource(Res.string.settings_backlight_duration),
						selectedOption = state.pendingWatchSettings.backlightDuration,
						optionLabel = { stringResource(it.label) },
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
				title = { Text(stringResource(Res.string.settings_time_adjustment)) }
			) {
				Column {
					SwitchField(
						title = stringResource(Res.string.settings_auto_time_adjustment),
						check = state.pendingConnectionSettings.autoSyncEnable,
						onCheckedChange = { onEvent(SettingsUiEvent.AutoSyncChange(it)) },
						enabled = state.isConnectionSettingsInitialized && state.isLoading == false,
						modifier = Modifier.fillMaxWidth()
					)
					val autoSyncEnabled = state.pendingConnectionSettings.autoSyncEnable
					SliderField(
						label = stringResource(Res.string.settings_minutes_past_hour),
						supportingText = if (autoSyncEnabled) {
							// the string holds the fixed hours, AutoSyncDelay.ADJUSTMENT_HOURS
							stringResource(
								Res.string.settings_auto_time_adjustment_schedule,
								state.pendingConnectionSettings.autoSyncDelay.minutes.toString().padStart(2, '0'),
							)
						} else {
							stringResource(Res.string.settings_auto_time_adjustment_off)
						},
						value = state.pendingConnectionSettings.autoSyncDelay.minutes,
						enabled = autoSyncEnabled && state.isConnectionSettingsInitialized && state.isLoading == false,
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
				title = { Text(stringResource(Res.string.common_others)) }
			) {
				Column(
					modifier = Modifier.fillMaxWidth(),
					verticalArrangement = Arrangement.spacedBy(8.dp)
				) {
					OutlinedTextField(
						label = { Text(stringResource(Res.string.settings_watch_name)) },
						value = nameFieldValue,
						enabled = state.isInitialized && state.isLoading == false,
						isError = state.pendingNameError != null,
						supportingText = {
							state.pendingNameError?.let {
								Text(it.message ?: stringResource(Res.string.common_unknown_error))
							}
						},
						onValueChange = {
							nameFieldValue = it
							onEvent(SettingsUiEvent.NameInputChange(it.text))
						},
						modifier = Modifier.fillMaxWidth()
					)
					SwitchField(
						title = stringResource(Res.string.settings_power_saving_mode),
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
						title = stringResource(Res.string.settings_mute_button_tone),
						check = state.pendingWatchSettings.preferences.isToneMuted,
						onCheckedChange = { onEvent(SettingsUiEvent.IsMutedChange(it)) },
						enabled = state.isWatchSettingsInitialized && state.isLoading == false,
						modifier = Modifier.fillMaxWidth()
					)
					EnumDropdown(
						label = stringResource(Res.string.settings_connection_timeout),
						selectedOption = state.pendingConnectionSettings.connectionTimeout,
						optionLabel = { it.label },
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
					if (state.canAdjustHands) {
						OutlinedButton(
							onClick = { showHandGroupDialog = true },
							enabled = state.isLoading == false && state.handAdjust == null,
							modifier = Modifier.fillMaxWidth(),
						) { Text(stringResource(Res.string.settings_adjust_hands)) }
					}
				}
			}
		}
	}

	if (showHandGroupDialog) {
		HandGroupDialog(
			onSelect = {
				showHandGroupDialog = false
				onEvent(SettingsUiEvent.HandGroupSelected(it))
			},
			onDismiss = { showHandGroupDialog = false },
		)
	}

	state.handAdjust?.let { session ->
		HandAdjustDialog(
			state = session,
			onStepsChange = { onEvent(SettingsUiEvent.HandAdjustStepsChange(it)) },
			onStepAnticlockwise = { onEvent(SettingsUiEvent.HandAdjustStepAnticlockwise) },
			onStepClockwise = { onEvent(SettingsUiEvent.HandAdjustStepClockwise) },
			onSet = { onEvent(SettingsUiEvent.HandAdjustSet) },
			onFinish = { onEvent(SettingsUiEvent.HandAdjustFinish) },
		)
	}
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
	SkuareTheme {
		SettingsScreenContent(
			state = SettingsUiState(
				isNameInitialized = true,
				isNameLoading = false,
				isWatchSettingsInitialized = true,
				isWatchSettingsLoading = false,
				isConnectionSettingsInitialized = true,
				isConnectionSettingsLoading = false,
				canAdjustHands = true,
			),
			onEvent = {},
		)
	}
}
