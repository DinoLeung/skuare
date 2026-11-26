package xyz.d1n0.ui.screen.locations

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.stringResource
import xyz.d1n0.resources.*
import xyz.d1n0.ui.theme.SkuareTheme

/**
 * Names a recorded location, e.g. "Secret hideout". An empty name goes back to the time of the
 * press, which the empty field shows as its placeholder.
 *
 * @param name The location's own name, null if it has none.
 * @param defaultName What the location is titled without a name, the time of the press.
 * @param onConfirm Gets the name as typed, the repository trims it.
 */
@Composable
fun RenameLocationDialog(
	name: String?,
	defaultName: String,
	onConfirm: (String) -> Unit,
	onDismiss: () -> Unit,
) {
	// the cursor at the end, to add to the name or clear it
	var value by remember { mutableStateOf(TextFieldValue(name.orEmpty(), TextRange(name.orEmpty().length))) }
	val focusRequester = remember { FocusRequester() }
	LaunchedEffect(Unit) { focusRequester.requestFocus() }

	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text(stringResource(Res.string.locations_rename_title)) },
		text = {
			OutlinedTextField(
				value = value,
				onValueChange = { value = it },
				label = { Text(stringResource(Res.string.locations_name)) },
				placeholder = { Text(defaultName) },
				supportingText = { Text(stringResource(Res.string.locations_name_hint)) },
				singleLine = true,
				keyboardOptions = KeyboardOptions(
					capitalization = KeyboardCapitalization.Sentences,
					imeAction = ImeAction.Done,
				),
				keyboardActions = KeyboardActions(onDone = { onConfirm(value.text) }),
				modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
			)
		},
		confirmButton = {
			TextButton(onClick = { onConfirm(value.text) }) { Text(stringResource(Res.string.common_save)) }
		},
		dismissButton = {
			TextButton(onClick = onDismiss) { Text(stringResource(Res.string.common_cancel)) }
		},
	)
}

@Preview(showBackground = true, widthDp = 420, heightDp = 480)
@Composable
private fun RenameLocationDialogPreview() {
	SkuareTheme {
		RenameLocationDialog(name = "Secret hideout", defaultName = "15 Oct 2026, 13:27", onConfirm = {}, onDismiss = {})
	}
}
