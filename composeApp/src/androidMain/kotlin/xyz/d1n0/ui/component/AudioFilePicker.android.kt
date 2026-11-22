package xyz.d1n0.ui.component

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import xyz.d1n0.phone.PickedAudio

/**
 * The system file picker for audio. It needs no storage permission, and the app copies the file,
 * so no lasting access to it is kept either.
 */
@Composable
actual fun rememberAudioFilePicker(onPicked: (PickedAudio) -> Unit): (() -> Unit)? {
	val resolver = LocalContext.current.contentResolver
	val currentOnPicked by rememberUpdatedState(onPicked)
	val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
		uri?.let { currentOnPicked(resolver.pickedAudio(it)) }
	}
	return remember(launcher) { { launcher.launch(arrayOf("audio/*")) } }
}

private fun ContentResolver.pickedAudio(uri: Uri): PickedAudio {
	var name: String? = null
	var size: Long? = null
	query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
		if (cursor.moveToFirst()) {
			name = cursor.getString(0)
			size = if (cursor.isNull(1)) null else cursor.getLong(1)
		}
	}
	return PickedAudio(
		displayName = name ?: uri.lastPathSegment.orEmpty(),
		sizeBytes = size,
		read = {
			withContext(Dispatchers.IO) {
				checkNotNull(openInputStream(uri)) { "Couldn't open the picked file" }.use { it.readBytes() }
			}
		},
	)
}
