package xyz.d1n0.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import xyz.d1n0.data.database.PairedWatch
import kotlin.time.Instant

/**
 * A paired watch with its last background sync, and a button to forget it after confirming.
 */
@Composable
fun PairedWatchCard(
	watch: PairedWatch,
	onForget: () -> Unit,
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
			watch.lastSyncError?.let { ErrorText(it) }
			TextButton(onClick = { showForgetDialog = true }) {
				Text("Forget watch")
			}
		}
	}

	if (showForgetDialog) {
		AlertDialog(
			onDismissRequest = { showForgetDialog = false },
			title = { Text("Forget ${watch.displayName}?") },
			text = { Text("It won't adjust its time in the background until you pair it again.") },
			confirmButton = {
				TextButton(onClick = {
					showForgetDialog = false
					onForget()
				}) { Text("Forget") }
			},
			dismissButton = {
				TextButton(onClick = { showForgetDialog = false }) { Text("Cancel") }
			},
		)
	}
}

private val lastSyncFormat = LocalDateTime.Format {
	date(LocalDate.Formats.ISO)
	char(' ')
	hour()
	char(':')
	minute()
}

private val PairedWatch.lastSyncText: String
	get() = when {
		lastSyncAt == null -> "Never synced in the background"
		lastSyncError == null -> "Last synced ${lastSyncAt.format()}"
		else -> "Last sync failed ${lastSyncAt.format()}"
	}

private fun Instant.format(): String =
	toLocalDateTime(TimeZone.currentSystemDefault()).format(lastSyncFormat)

@Preview(showBackground = true)
@Composable
private fun PairedWatchCardPreview() {
	MaterialTheme {
		PairedWatchCard(
			watch = PairedWatch(
				address = "D8:70:90:50:94:26",
				advertisedName = "CASIO GW-B5600#",
				displayName = "CASIO GW-B5600#",
				pairedAt = Instant.fromEpochMilliseconds(1792220000000),
				lastSyncAt = Instant.fromEpochMilliseconds(1792231646000),
			),
			onForget = {},
		)
	}
}
