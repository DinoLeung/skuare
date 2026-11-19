package xyz.d1n0.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import xyz.d1n0.lib.model.CustomWatchModel
import xyz.d1n0.ui.theme.SkuareTheme
import org.jetbrains.compose.resources.stringResource
import xyz.d1n0.resources.*

/**
 * Edits the features of a watch skuare doesn't recognise.
 *
 * @param watchName Advertised name of the watch, e.g. CASIO GBD-H2000.
 * @param initial The watch's current custom model.
 * @param hasUnsavedChanges Whether any tab has unsaved changes, saving rebuilds the tabs and
 *        discards them.
 * @param disconnectsOnSave Whether saving disconnects the watch, after it errored on this
 *        connection, see [xyz.d1n0.ui.navigation.NavBarViewModel.saveCustomModel].
 * @param onSave Called with the edited model, only if it differs from [initial].
 */
@Composable
fun CustomWatchModelDialog(
	watchName: String,
	initial: CustomWatchModel,
	hasUnsavedChanges: Boolean,
	disconnectsOnSave: Boolean = false,
	onSave: (CustomWatchModel) -> Unit,
	onDismiss: () -> Unit,
) {
	var draft by remember(initial) { mutableStateOf(initial) }

	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text(stringResource(Res.string.custom_model_title, watchName)) },
		text = {
			Column(
				modifier = Modifier.verticalScroll(rememberScrollState()),
				verticalArrangement = Arrangement.spacedBy(12.dp),
			) {
				FeatureHint {
					SliderField(
						label = stringResource(Res.string.custom_model_clocks),
						value = draft.clockCount,
						range = CustomWatchModel.CLOCK_COUNT_RANGE,
						onValueChange = {
							draft = draft.copy(clockCount = it.coerceIn(CustomWatchModel.CLOCK_COUNT_RANGE))
						},
						error = null,
					)
					Hint(stringResource(Res.string.custom_model_clocks_hint))
				}
				FeatureHint {
					SliderField(
						label = stringResource(Res.string.custom_model_reminders),
						value = draft.reminderCount,
						range = CustomWatchModel.REMINDER_COUNT_RANGE,
						onValueChange = {
							draft = draft.copy(reminderCount = it.coerceIn(CustomWatchModel.REMINDER_COUNT_RANGE))
						},
						error = null,
					)
					Hint(stringResource(Res.string.custom_model_reminders_hint))
				}
				FeatureHint {
					SwitchField(
						modifier = Modifier.fillMaxWidth(),
						title = stringResource(Res.string.custom_model_font_style),
						check = draft.hasFontStyle,
						onCheckedChange = { draft = draft.copy(hasFontStyle = it) },
					)
					Hint(stringResource(Res.string.custom_model_font_style_hint))
				}
				FeatureHint {
					SwitchField(
						modifier = Modifier.fillMaxWidth(),
						title = stringResource(Res.string.custom_model_short_city_names),
						check = draft.hasShortCityNames,
						onCheckedChange = { draft = draft.copy(hasShortCityNames = it) },
					)
					Hint(stringResource(Res.string.custom_model_short_city_names_hint))
				}
				FeatureHint {
					SwitchField(
						modifier = Modifier.fillMaxWidth(),
						title = stringResource(Res.string.custom_model_weekday_language),
						check = draft.hasWeekdayLanguage,
						onCheckedChange = { draft = draft.copy(hasWeekdayLanguage = it) },
					)
					Hint(stringResource(Res.string.custom_model_weekday_language_hint))
				}
				FeatureHint {
					SwitchField(
						modifier = Modifier.fillMaxWidth(),
						title = stringResource(Res.string.custom_model_snooze_alarm),
						check = draft.hasSnoozeAlarm,
						onCheckedChange = { draft = draft.copy(hasSnoozeAlarm = it) },
					)
					Hint(stringResource(Res.string.custom_model_snooze_alarm_hint))
				}
				FeatureHint {
					SwitchField(
						modifier = Modifier.fillMaxWidth(),
						title = stringResource(Res.string.custom_model_time_zone_location),
						check = draft.hasTimeZoneLocation,
						onCheckedChange = { draft = draft.copy(hasTimeZoneLocation = it) },
					)
					Hint(stringResource(Res.string.custom_model_time_zone_location_hint))
				}
				FeatureHint {
					SwitchField(
						modifier = Modifier.fillMaxWidth(),
						title = stringResource(Res.string.custom_model_hand_adjustment),
						check = draft.canAdjustHands,
						onCheckedChange = { draft = draft.copy(canAdjustHands = it) },
					)
					Hint(stringResource(Res.string.custom_model_hand_adjustment_hint))
				}
				Text(
					text = stringResource(Res.string.custom_model_warning),
					style = MaterialTheme.typography.bodySmall,
				)
				if (disconnectsOnSave) {
					ErrorText(text = stringResource(Res.string.custom_model_disconnects_on_save))
				}
				if (hasUnsavedChanges) {
					ErrorText(text = stringResource(Res.string.custom_model_unsaved_other_tabs))
				}
			}
		},
		confirmButton = {
			TextButton(
				onClick = { onSave(draft) },
				enabled = draft != initial,
			) { Text(stringResource(Res.string.common_save)) }
		},
		dismissButton = {
			TextButton(onClick = onDismiss) { Text(stringResource(Res.string.common_cancel)) }
		},
	)
}

@Composable
private fun FeatureHint(content: @Composable () -> Unit) {
	Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { content() }
}

@Composable
private fun Hint(text: String) {
	Text(
		text = text,
		style = MaterialTheme.typography.bodySmall,
		color = MaterialTheme.colorScheme.onSurfaceVariant,
	)
}

/**
 * Explains, the first time a watch skuare doesn't recognise is connected, that only the basics
 * are on and where to adjust them.
 *
 * @param watchName Advertised name of the watch, e.g. CASIO GBD-H2000.
 * @param onAdjust Opens [CustomWatchModelDialog].
 * @param onLater Closes the dialog, it isn't shown again either way.
 */
@Composable
fun CustomWatchModelIntroDialog(
	watchName: String,
	onAdjust: () -> Unit,
	onLater: () -> Unit,
) {
	AlertDialog(
		onDismissRequest = onLater,
		title = { Text(stringResource(Res.string.custom_model_intro_title, watchName)) },
		text = {
			Text(stringResource(Res.string.custom_model_intro_text))
		},
		confirmButton = {
			TextButton(onClick = onAdjust) { Text(stringResource(Res.string.custom_model_intro_adjust)) }
		},
		dismissButton = {
			TextButton(onClick = onLater) { Text(stringResource(Res.string.custom_model_intro_later)) }
		},
	)
}

@Preview(showBackground = true, widthDp = 420, heightDp = 720)
@Composable
private fun CustomWatchModelDialogPreview() {
	SkuareTheme {
		CustomWatchModelDialog(
			watchName = "CASIO GBD-H2000",
			initial = CustomWatchModel(),
			hasUnsavedChanges = true,
			onSave = {},
			onDismiss = {},
		)
	}
}

@Preview(showBackground = true, widthDp = 420, heightDp = 720)
@Composable
private fun CustomWatchModelIntroDialogPreview() {
	SkuareTheme {
		CustomWatchModelIntroDialog(watchName = "CASIO GBD-H2000", onAdjust = {}, onLater = {})
	}
}
