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
		title = { Text("$watchName features") },
		text = {
			Column(
				modifier = Modifier.verticalScroll(rememberScrollState()),
				verticalArrangement = Arrangement.spacedBy(12.dp),
			) {
				FeatureHint {
					SliderField(
						label = "Clocks",
						value = draft.clockCount,
						range = CustomWatchModel.CLOCK_COUNT_RANGE,
						onValueChange = {
							draft = draft.copy(clockCount = it.coerceIn(CustomWatchModel.CLOCK_COUNT_RANGE))
						},
						error = null,
					)
					Hint("Home time plus the world times your watch shows.")
				}
				FeatureHint {
					SliderField(
						label = "Reminders",
						value = draft.reminderCount,
						range = CustomWatchModel.REMINDER_COUNT_RANGE,
						onValueChange = {
							draft = draft.copy(reminderCount = it.coerceIn(CustomWatchModel.REMINDER_COUNT_RANGE))
						},
						error = null,
					)
					Hint("0 if your watch has none, anything above adds the Reminders tab.")
				}
				FeatureHint {
					SwitchField(
						modifier = Modifier.fillMaxWidth(),
						title = "Font style",
						check = draft.hasFontStyle,
						onCheckedChange = { draft = draft.copy(hasFontStyle = it) },
					)
					Hint("Your watch has a display font setting.")
				}
				FeatureHint {
					SwitchField(
						modifier = Modifier.fillMaxWidth(),
						title = "Short city names",
						check = draft.hasShortCityNames,
						onCheckedChange = { draft = draft.copy(hasShortCityNames = it) },
					)
					Hint("Your watch shows 3-letter city codes, e.g. TYO.")
				}
				FeatureHint {
					SwitchField(
						modifier = Modifier.fillMaxWidth(),
						title = "Weekday language",
						check = draft.hasWeekdayLanguage,
						onCheckedChange = { draft = draft.copy(hasWeekdayLanguage = it) },
					)
					Hint("The weekday can be shown in other languages.")
				}
				FeatureHint {
					SwitchField(
						modifier = Modifier.fillMaxWidth(),
						title = "Snooze alarm",
						check = draft.hasSnoozeAlarm,
						onCheckedChange = { draft = draft.copy(hasSnoozeAlarm = it) },
					)
					Hint("Your watch's fifth alarm is a snooze alarm, off if it's a normal alarm.")
				}
				FeatureHint {
					SwitchField(
						modifier = Modifier.fillMaxWidth(),
						title = "Time zone location",
						check = draft.hasTimeZoneLocation,
						onCheckedChange = { draft = draft.copy(hasTimeZoneLocation = it) },
					)
					Hint("Also sends each city's location and radio station with its time zone.")
				}
				FeatureHint {
					SwitchField(
						modifier = Modifier.fillMaxWidth(),
						title = "Hand adjustment",
						check = draft.canAdjustHands,
						onCheckedChange = { draft = draft.copy(canAdjustHands = it) },
					)
					Hint("Your watch has hands to adjust, a clock hand and a mode hand like the GA-B2100.")
				}
				Text(
					text = "Turning on something your watch doesn't have can make syncing fail. " +
						"If time sync starts failing, turn it back off.",
					style = MaterialTheme.typography.bodySmall,
				)
				if (disconnectsOnSave) {
					ErrorText(
						text = "The watch returned an error, saving disconnects it so it can " +
							"recover. Reconnect it from the watch afterwards."
					)
				}
				if (hasUnsavedChanges) {
					ErrorText(text = "Unsaved changes on other tabs will be discarded.")
				}
			}
		},
		confirmButton = {
			TextButton(
				onClick = { onSave(draft) },
				enabled = draft != initial,
			) { Text("Save") }
		},
		dismissButton = {
			TextButton(onClick = onDismiss) { Text("Cancel") }
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
		title = { Text("$watchName isn't fully supported yet") },
		text = {
			Text(
				"Only the basics are on: a home clock and one world clock. Use the wrench button " +
					"in the title bar to turn on the features your watch has."
			)
		},
		confirmButton = {
			TextButton(onClick = onAdjust) { Text("Adjust") }
		},
		dismissButton = {
			TextButton(onClick = onLater) { Text("Later") }
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
