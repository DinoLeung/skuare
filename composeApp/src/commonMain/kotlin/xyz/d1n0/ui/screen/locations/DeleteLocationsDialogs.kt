package xyz.d1n0.ui.screen.locations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import xyz.d1n0.resources.*
import xyz.d1n0.ui.theme.SkuareTheme

val LocationAge.label: StringResource
	get() = when (this) {
		LocationAge.WEEK -> Res.string.locations_older_than_week
		LocationAge.MONTH -> Res.string.locations_older_than_month
		LocationAge.THREE_MONTHS -> Res.string.locations_older_than_three_months
		LocationAge.SIX_MONTHS -> Res.string.locations_older_than_six_months
		LocationAge.YEAR -> Res.string.locations_older_than_year
	}

/** Asks before deleting every location, it can't be undone. */
@Composable
fun DeleteAllLocationsDialog(
	count: Int,
	onConfirm: () -> Unit,
	onDismiss: () -> Unit,
) {
	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text(stringResource(Res.string.locations_delete_all_title)) },
		text = { Text(pluralStringResource(Res.plurals.locations_delete_all_text, count, count)) },
		confirmButton = {
			TextButton(onClick = onConfirm) { Text(stringResource(Res.string.common_delete)) }
		},
		dismissButton = {
			TextButton(onClick = onDismiss) { Text(stringResource(Res.string.common_cancel)) }
		},
	)
}

/**
 * Picks a [LocationAge] and deletes the locations older than it. Starts on the oldest age, the one
 * that deletes least.
 *
 * @param counts How many locations each age would delete.
 */
@Composable
fun DeleteOlderLocationsDialog(
	counts: Map<LocationAge, Int>,
	onConfirm: (LocationAge) -> Unit,
	onDismiss: () -> Unit,
) {
	var selected by remember { mutableStateOf(LocationAge.YEAR) }
	val count = counts[selected] ?: 0
	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text(stringResource(Res.string.locations_delete_older)) },
		text = {
			Column(modifier = Modifier.selectableGroup()) {
				LocationAge.entries.forEach { age ->
					Row(
						modifier = Modifier
							.fillMaxWidth()
							.selectable(selected = age == selected, onClick = { selected = age }, role = Role.RadioButton)
							.padding(vertical = 4.dp),
						verticalAlignment = Alignment.CenterVertically,
						horizontalArrangement = Arrangement.spacedBy(8.dp),
					) {
						// the row handles the click, so the label selects it too
						RadioButton(selected = age == selected, onClick = null)
						Text(stringResource(age.label), modifier = Modifier.weight(1f))
						Text(
							text = (counts[age] ?: 0).toString(),
							style = MaterialTheme.typography.bodyMedium,
							color = MaterialTheme.colorScheme.onSurfaceVariant,
						)
					}
				}
			}
		},
		confirmButton = {
			TextButton(onClick = { onConfirm(selected) }, enabled = count > 0) {
				Text(pluralStringResource(Res.plurals.locations_delete_count, count, count))
			}
		},
		dismissButton = {
			TextButton(onClick = onDismiss) { Text(stringResource(Res.string.common_cancel)) }
		},
	)
}

@Preview(showBackground = true, widthDp = 420, heightDp = 720)
@Composable
private fun DeleteOlderLocationsDialogPreview() {
	SkuareTheme {
		DeleteOlderLocationsDialog(
			counts = mapOf(
				LocationAge.WEEK to 12,
				LocationAge.MONTH to 8,
				LocationAge.THREE_MONTHS to 3,
				LocationAge.SIX_MONTHS to 1,
				LocationAge.YEAR to 0,
			),
			onConfirm = {},
			onDismiss = {},
		)
	}
}
