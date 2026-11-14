package xyz.d1n0.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import xyz.d1n0.lib.constant.HandGroup
import xyz.d1n0.ui.theme.SkuareTheme

/**
 * Asks which hands to adjust, one option per [HandGroup].
 *
 * @param onSelect Called with the chosen group, the dialog doesn't close itself.
 */
@Composable
fun HandGroupDialog(
	onSelect: (HandGroup) -> Unit,
	onDismiss: () -> Unit,
	groups: List<HandGroup> = HandGroup.entries,
) {
	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text("Adjust Hands") },
		text = {
			Column {
				groups.forEach { group ->
					ListItem(
						headlineContent = { Text(group.toString()) },
						colors = ListItemDefaults.colors(containerColor = Color.Transparent),
						modifier = Modifier.clickable { onSelect(group) },
					)
				}
			}
		},
		confirmButton = {
			TextButton(onClick = onDismiss) { Text("Cancel") }
		},
	)
}

@Preview(showBackground = true, widthDp = 420, heightDp = 720)
@Composable
private fun HandGroupDialogPreview() {
	SkuareTheme {
		HandGroupDialog(onSelect = {}, onDismiss = {})
	}
}
