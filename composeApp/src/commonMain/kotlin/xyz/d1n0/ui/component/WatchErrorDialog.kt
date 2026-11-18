package xyz.d1n0.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import xyz.d1n0.lib.constant.OpCode
import xyz.d1n0.lib.model.WatchErrorPacket
import xyz.d1n0.ui.theme.SkuareTheme
import org.jetbrains.compose.resources.stringResource
import xyz.d1n0.resources.*

/**
 * Tells the user the watch replied with [error] instead of the data that was asked for, showing
 * the packet so it can be reported. The watch can keep refusing until it reconnects, so the
 * dialog leads to reconnecting.
 *
 * The bottom always has Disconnect then Close. A clocks error on a custom watch also has Adjust
 * features above them.
 *
 * @param onAdjustFeatures Opens [CustomWatchModelDialog], null for a known watch. A clocks error
 *        on a custom watch can mean it was set to more clocks than the watch has.
 * @param onDisconnect Disconnects the watch so it can be reconnected.
 */
@Composable
fun WatchErrorDialog(
	error: WatchErrorPacket,
	onAdjustFeatures: (() -> Unit)?,
	onDisconnect: () -> Unit,
	onDismiss: () -> Unit,
) {
	val isClocksError = error.failedOpCode == OpCode.CLOCK
	val adjustFeatures = onAdjustFeatures?.takeIf { isClocksError }

	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text(stringResource(Res.string.watch_error_title)) },
		text = {
			Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
				if (isClocksError) {
					Text(stringResource(Res.string.watch_error_clocks))
					Text(
						stringResource(
							if (adjustFeatures != null) Res.string.watch_error_clocks_check_features
							else Res.string.watch_error_clocks_reconnect
						)
					)
				} else {
					Text(stringResource(Res.string.watch_error_request))
					Text(stringResource(Res.string.watch_error_keeps_happening))
				}
				SelectionContainer {
					Text(
						text = error.hex,
						fontFamily = FontFamily.Monospace,
						style = MaterialTheme.typography.bodyLarge,
					)
				}
				adjustFeatures?.let {
					TextButton(onClick = it, modifier = Modifier.align(Alignment.End)) {
						Text(stringResource(Res.string.watch_error_adjust_features))
					}
				}
			}
		},
		confirmButton = {
			Row {
				TextButton(onClick = onDisconnect) { Text(stringResource(Res.string.common_disconnect)) }
				TextButton(onClick = onDismiss) { Text(stringResource(Res.string.common_close)) }
			}
		},
	)
}

private val clocksError = WatchErrorPacket(byteArrayOf(0xFF.toByte(), 0x81.toByte(), 0x1D))

@Preview
@Composable
private fun WatchErrorDialogPreview() {
	SkuareTheme {
		WatchErrorDialog(error = clocksError, onAdjustFeatures = null, onDisconnect = {}, onDismiss = {})
	}
}

@Preview
@Composable
private fun WatchErrorDialogCustomModelPreview() {
	SkuareTheme {
		WatchErrorDialog(error = clocksError, onAdjustFeatures = {}, onDisconnect = {}, onDismiss = {})
	}
}
