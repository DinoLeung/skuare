package xyz.d1n0.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
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
import xyz.d1n0.data.database.PairedWatch
import kotlin.time.Instant
import xyz.d1n0.ui.text.LocalLocaleFormatter
import xyz.d1n0.ui.text.formatDateTime
import xyz.d1n0.ui.text.lastSyncFailureMessage
import xyz.d1n0.ui.theme.SkuareTheme
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import xyz.d1n0.resources.*

/**
 * A paired watch with its last background sync, and a button to forget it after confirming.
 *
 * @param locationCount Locations recorded with this watch. With any, the forget dialog asks
 *        whether to delete them too, unchecked by default since it can't be undone.
 * @param onForget Called with whether to delete the watch's locations too.
 */
@Composable
fun PairedWatchCard(
	watch: PairedWatch,
	locationCount: Int,
	onForget: (deleteLocations: Boolean) -> Unit,
	modifier: Modifier = Modifier,
) {
	var showForgetDialog by remember { mutableStateOf(false) }

	CardView(
		modifier = modifier,
		title = { Text(watch.displayName) },
	) {
		Column(
			modifier = Modifier.fillMaxWidth(),
			verticalArrangement = Arrangement.spacedBy(8.dp),
		) {
			Text(
				text = watch.lastSyncText,
				style = MaterialTheme.typography.bodyMedium,
			)
			watch.lastSyncFailureMessage?.let { ErrorText(it) }
			TextButton(onClick = { showForgetDialog = true }) {
				Text(stringResource(Res.string.paired_watch_forget_button))
			}
		}
	}

	if (showForgetDialog) {
		var deleteLocations by remember { mutableStateOf(false) }
		AlertDialog(
			onDismissRequest = { showForgetDialog = false },
			title = { Text(stringResource(Res.string.paired_watch_forget_title, watch.displayName)) },
			text = {
				Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
					Text(stringResource(Res.string.paired_watch_forget_text))
					if (locationCount > 0) {
						Row(
							modifier = Modifier
								.fillMaxWidth()
								.toggleable(
									value = deleteLocations,
									onValueChange = { deleteLocations = it },
									role = Role.Checkbox,
								),
							verticalAlignment = Alignment.CenterVertically,
							// without its own click the checkbox has no touch target padding, this puts the
							// label where it sits next to a standard 48 dp checkbox
							horizontalArrangement = Arrangement.spacedBy(12.dp),
						) {
							// the row handles the click, so the label toggles it too
							Checkbox(checked = deleteLocations, onCheckedChange = null)
							Text(
								pluralStringResource(
									Res.plurals.paired_watch_forget_delete_locations,
									locationCount,
									locationCount,
								)
							)
						}
					}
				}
			},
			confirmButton = {
				TextButton(onClick = {
					showForgetDialog = false
					onForget(locationCount > 0 && deleteLocations)
				}) { Text(stringResource(Res.string.paired_watch_forget_confirm)) }
			},
			dismissButton = {
				TextButton(onClick = { showForgetDialog = false }) { Text(stringResource(Res.string.common_cancel)) }
			},
		)
	}
}

private val PairedWatch.lastSyncText: String
	@Composable get() {
		val at = lastSyncAt ?: return stringResource(Res.string.paired_watch_never_synced)
		val formatted = LocalLocaleFormatter.current.formatDateTime(at)
		return if (lastSyncFailure == null) stringResource(Res.string.paired_watch_last_synced, formatted)
		else stringResource(Res.string.paired_watch_last_sync_failed, formatted)
	}

@Preview(showBackground = true)
@Composable
private fun PairedWatchCardPreview() {
	SkuareTheme {
		PairedWatchCard(
			watch = PairedWatch(
				address = "D8:70:90:50:94:26",
				advertisedName = "CASIO GW-B5600#",
				displayName = "CASIO GW-B5600#",
				pairedAt = Instant.fromEpochMilliseconds(1792220000000),
				lastSyncAt = Instant.fromEpochMilliseconds(1792231646000),
			),
			locationCount = 3,
			onForget = {},
		)
	}
}
