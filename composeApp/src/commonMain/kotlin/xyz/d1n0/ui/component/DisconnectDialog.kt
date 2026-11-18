package xyz.d1n0.ui.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import xyz.d1n0.ui.text.joinLocalized
import xyz.d1n0.ui.theme.SkuareTheme
import org.jetbrains.compose.resources.stringResource
import xyz.d1n0.resources.*

/**
 * Asks before disconnecting the watch while tabs have unsaved changes, which disconnecting
 * discards.
 *
 * @param unsavedTabs Names of the tabs with unsaved changes, e.g. Alarms.
 * @param onDisconnect Disconnects the watch, discarding the changes.
 */
@Composable
fun DisconnectDialog(
	unsavedTabs: List<String>,
	onDisconnect: () -> Unit,
	onDismiss: () -> Unit,
) {
	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text(stringResource(Res.string.disconnect_title)) },
		text = {
			Text(stringResource(Res.string.disconnect_unsaved_changes, unsavedTabs.joinLocalized()))
		},
		confirmButton = {
			TextButton(onClick = onDisconnect) { Text(stringResource(Res.string.common_disconnect)) }
		},
		dismissButton = {
			TextButton(onClick = onDismiss) { Text(stringResource(Res.string.common_cancel)) }
		},
	)
}

@Preview
@Composable
private fun DisconnectDialogPreview() {
	SkuareTheme {
		DisconnectDialog(unsavedTabs = listOf("Alarms", "Settings"), onDisconnect = {}, onDismiss = {})
	}
}
