package xyz.d1n0.ui.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import xyz.d1n0.phone.PhoneFinder
import xyz.d1n0.resources.*
import xyz.d1n0.ui.theme.SkuareTheme

/**
 * Over every screen while find phone rings, so whoever opens the app can stop it. It goes away
 * when the ringing stops, however it stopped, it can't be dismissed otherwise.
 */
@Composable
fun FindPhoneRingingDialog(phoneFinder: PhoneFinder = koinInject()) {
	val ringing by phoneFinder.ringing.collectAsState()
	val scope = rememberCoroutineScope()
	if (ringing) FindPhoneRingingDialogContent(onStop = { scope.launch { phoneFinder.stop() } })
}

@Composable
private fun FindPhoneRingingDialogContent(onStop: () -> Unit) {
	AlertDialog(
		// only Stop ends it, a tap beside it mustn't leave the phone ringing without the dialog
		onDismissRequest = {},
		title = { Text(stringResource(Res.string.find_phone_ringing_title)) },
		text = { Text(stringResource(Res.string.find_phone_ringing_text)) },
		confirmButton = {
			TextButton(onClick = onStop) { Text(stringResource(Res.string.find_phone_stop)) }
		},
	)
}

@Preview
@Composable
private fun FindPhoneRingingDialogPreview() {
	SkuareTheme {
		FindPhoneRingingDialogContent(onStop = {})
	}
}
