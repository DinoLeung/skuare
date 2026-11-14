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
import androidx.compose.runtime.LaunchedEffect
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
 * What [HandAdjustDialog] shows for a hand adjust session, see
 * [xyz.d1n0.ui.screen.settings.HandAdjustSession].
 *
 * @property steps The next move, negative for anticlockwise, see
 *           [xyz.d1n0.ui.screen.settings.HandAdjustSession.set].
 * @property isReady Whether the hands have reached the adjusting position.
 * @property isMoving Whether the hands are moving, to the adjusting position or after a set.
 * @property error Why the last command failed, null if it didn't.
 */
data class HandAdjustUiState(
	val group: HandGroup,
	val steps: Int = 0,
	val isReady: Boolean = false,
	val isMoving: Boolean = true,
	val error: String? = null,
) {
	/**
	 * The largest move either way. A whole turn or more would be sent as 0, the move to the
	 * adjusting position, see [xyz.d1n0.lib.model.HandAdjust.movePacket].
	 */
	val maxSteps: Int get() = group.stepModulus - 1

	/** Whether [steps] can be sent, 0 would be the move to the adjusting position. */
	val isStepsValid: Boolean get() = steps in -maxSteps..maxSteps

	/** Whether the steps can be changed: the hands are ready and not moving. */
	val canEditSteps: Boolean get() = isReady && isMoving == false

	val canStepAnticlockwise: Boolean get() = canEditSteps && steps > -maxSteps
	val canStepClockwise: Boolean get() = canEditSteps && steps < maxSteps
	val canSet: Boolean get() = canEditSteps && isStepsValid && steps != 0

	/** Finish waits for the hands too, a move the watch hasn't finished isn't confirmed yet. */
	val canFinish: Boolean get() = isMoving == false
}

/**
 * Shows a hand adjust session ([xyz.d1n0.ui.screen.settings.HandAdjustSession]): the steps, which
 * the anticlockwise and clockwise buttons count down and up, and Set to move the hands by them. The
 * rules (limits, what's enabled, resetting after a move) are the session's, see
 * [HandAdjustUiState]. Only Finish closes the dialog, so the watch always gets told the session
 * has finished.
 *
 * @param onStepsChange Called with the steps as typed, 0 while the field holds no number yet.
 * @param onFinish Called when Finish is pressed, the caller closes the dialog and ends the session.
 */
@Composable
fun HandAdjustDialog(
	state: HandAdjustUiState,
	onStepsChange: (Int) -> Unit,
	onStepAnticlockwise: () -> Unit,
	onStepClockwise: () -> Unit,
	onSet: () -> Unit,
	onFinish: () -> Unit,
) {
	// the field's own text, "-" or an empty field aren't numbers yet
	var stepsText by rememberSaveable(state.group) { mutableStateOf(state.steps.toString()) }
	// follow the session when the steps change elsewhere, e.g. the buttons or a reset after Set
	LaunchedEffect(state.steps) {
		if ((stepsText.toIntOrNull() ?: 0) != state.steps) stepsText = state.steps.toString()
	}

	AlertDialog(
		// only Finish closes it, see onFinish
		onDismissRequest = {},
		properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
		title = { Text(state.group.toString()) },
		text = {
			Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
				if (state.isMoving) {
					Row(
						verticalAlignment = Alignment.CenterVertically,
						horizontalArrangement = Arrangement.spacedBy(12.dp),
					) {
						CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
						Text(
							text = if (state.isReady) "Moving the hands…" else "Moving the hands to the adjusting position…",
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
						onStepsChange(stepsText.toIntOrNull() ?: 0)
					},
					keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
					singleLine = true,
					enabled = state.canEditSteps,
					isError = state.isStepsValid == false,
					supportingText = { Text(state.group.stepHint) },
					modifier = Modifier.fillMaxWidth(),
				)
				// a segmented group for the look, never selected: both buttons are actions, not a choice
				SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
					SegmentedButton(
						selected = false,
						onClick = onStepAnticlockwise,
						shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
						enabled = state.canStepAnticlockwise,
						icon = { Icon(Icons.Outlined.RotateLeft, contentDescription = null) },
						label = { Text("Anticlockwise") },
					)
					SegmentedButton(
						selected = false,
						onClick = onStepClockwise,
						shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
						enabled = state.canStepClockwise,
						icon = { Icon(Icons.Outlined.RotateRight, contentDescription = null) },
						label = { Text("Clockwise") },
					)
				}
				Button(
					onClick = onSet,
					enabled = state.canSet,
					modifier = Modifier.fillMaxWidth(),
				) { Text("Set") }
				state.error?.let { ErrorText(it) }
			}
		},
		confirmButton = {
			TextButton(onClick = onFinish, enabled = state.canFinish) { Text("Finish") }
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
			state = HandAdjustUiState(group = HandGroup.CLOCK, steps = -15, isReady = true, isMoving = false),
			onStepsChange = {},
			onStepAnticlockwise = {},
			onStepClockwise = {},
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
			state = HandAdjustUiState(group = HandGroup.MODE),
			onStepsChange = {},
			onStepAnticlockwise = {},
			onStepClockwise = {},
			onSet = {},
			onFinish = {},
		)
	}
}
