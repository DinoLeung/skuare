package xyz.d1n0.ui.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import xyz.d1n0.ui.theme.SkuareTheme

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
		title = { Text("Disconnect the watch?") },
		text = {
			Text(
				"Unsaved changes on ${unsavedTabs.joinToString()} will be discarded. " +
					"Save them first to keep them."
			)
		},
		confirmButton = {
			TextButton(onClick = onDisconnect) { Text("Disconnect") }
		},
		dismissButton = {
			TextButton(onClick = onDismiss) { Text("Cancel") }
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
