package xyz.d1n0.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RotateLeft
import androidx.compose.material.icons.outlined.RotateRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import xyz.d1n0.lib.constant.HandGroup
import xyz.d1n0.ui.theme.SkuareTheme

/**
 * Adjusts one [HandGroup]: a step count that the anticlockwise and clockwise buttons count down
 * and up from 0, and Set to move the hands by it. Negative is anticlockwise, sent as
 * [HandGroup.stepModulus] − n, see [xyz.d1n0.lib.model.HandAdjust.movePacket]. Set resets the
 * count to 0, moves are relative to where the hands are.
 *
 * Everything, Finish included, is disabled while the hands move, until they stop. Only Finish
 * closes the dialog, so the watch always gets told the session has finished.
 *
 * @param isReady Whether the hands have reached the adjusting position. Nothing can be set before.
 * @param isMoving Whether the hands are moving, to the adjusting position or after Set.
 * @param error Why the last command failed, null if it didn't.
 * @param onSet Called with the steps to move, negative for anticlockwise.
 * @param onFinish Called when Finish is pressed, the caller closes the dialog and ends the session.
 */
@Composable
fun HandAdjustDialog(
	group: HandGroup,
	isReady: Boolean,
	isMoving: Boolean,
	error: String?,
	onSet: (steps: Int) -> Unit,
	onFinish: () -> Unit,
) {
	var stepsText by rememberSaveable(group) { mutableStateOf("0") }

	// a whole turn or more either way would be sent as 0, the move to the adjusting position
	val maxSteps = group.stepModulus - 1
	val parsed = stepsText.toIntOrNull()
	val steps = parsed?.takeIf { it in -maxSteps..maxSteps }
	val enabled = isReady && isMoving == false
	val count = steps ?: 0

	AlertDialog(
		// only Finish closes it, see onFinish
		onDismissRequest = {},
		properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
		title = { Text(group.toString()) },
		text = {
			Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
				if (isMoving) {
					Row(
						verticalAlignment = Alignment.CenterVertically,
						horizontalArrangement = Arrangement.spacedBy(12.dp),
					) {
						CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
						Text(
							text = if (isReady) "Moving the hands…" else "Moving the hands to the adjusting position…",
							style = MaterialTheme.typography.bodyMedium,
						)
					}
				}
				OutlinedTextField(
					label = { Text("Steps") },
					value = stepsText,
					onValueChange = { input ->
						// digits, with an optional leading minus for anticlockwise
						stepsText = input.filterIndexed { i, c -> c.isDigit() || (i == 0 && c == '-') }.take(5)
					},
					keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
					singleLine = true,
					enabled = enabled,
					// a lone minus or an empty field is still being typed
					isError = parsed != null && steps == null,
					supportingText = { Text(group.stepHint) },
					modifier = Modifier.fillMaxWidth(),
				)
				// a segmented group for the look, never selected: both buttons are actions, not a choice
				SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
					SegmentedButton(
						selected = false,
						onClick = { stepsText = (count - 1).toString() },
						shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
						enabled = enabled && count > -maxSteps,
						icon = { Icon(Icons.Outlined.RotateLeft, contentDescription = null) },
						label = { Text("Anticlockwise") },
					)
					SegmentedButton(
						selected = false,
						onClick = { stepsText = (count + 1).toString() },
						shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
						enabled = enabled && count < maxSteps,
						icon = { Icon(Icons.Outlined.RotateRight, contentDescription = null) },
						label = { Text("Clockwise") },
					)
				}
				Button(
					onClick = {
						steps?.let(onSet)
						stepsText = "0"
					},
					// 0 would be the move to the adjusting position
					enabled = enabled && steps != null && steps != 0,
					modifier = Modifier.fillMaxWidth(),
				) { Text("Set") }
				error?.let { ErrorText(it) }
			}
		},
		confirmButton = {
			// waits for the hands too, a move the watch hasn't finished isn't confirmed yet
			TextButton(onClick = onFinish, enabled = isMoving == false) { Text("Finish") }
		},
	)
}

/** How far a step moves the hands, for the steps field. */
private val HandGroup.stepHint: String
	get() = when (this) {
		HandGroup.CLOCK -> "3 steps is 1 minute"
		HandGroup.MODE -> "90 steps is half a turn"
	}

@Preview(showBackground = true, widthDp = 420, heightDp = 720)
@Composable
private fun HandAdjustDialogPreview() {
	SkuareTheme {
		HandAdjustDialog(
			group = HandGroup.CLOCK,
			isReady = true,
			isMoving = false,
			error = null,
			onSet = {},
			onFinish = {},
		)
	}
}

@Preview(showBackground = true, widthDp = 420, heightDp = 720)
@Composable
private fun HandAdjustDialogMovingPreview() {
	SkuareTheme {
		HandAdjustDialog(
			group = HandGroup.MODE,
			isReady = false,
			isMoving = true,
			error = null,
			onSet = {},
			onFinish = {},
		)
	}
}
