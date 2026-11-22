package xyz.d1n0.ui.component

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.d1n0.phone.PickedAudio
import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * The system file picker for audio. It needs no storage permission, and the app copies the file,
 * so no lasting access to it is kept either.
 */
@Composable
actual fun rememberAudioFilePicker(onPicked: (PickedAudio) -> Unit): (() -> Unit)? {
	val resolver = LocalContext.current.contentResolver
	val currentOnPicked by rememberUpdatedState(onPicked)
	// The composition's scope resumes on the main thread, so onPicked is called there.
	val scope = rememberCoroutineScope()
	val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
		uri?.let { scope.launch { currentOnPicked(resolver.pickedAudio(it)) } }
	}
	return remember(launcher) { { launcher.launch(arrayOf("audio/*")) } }
}

/** Queries the file's name and size off the main thread, since the provider may be another process. */
private suspend fun ContentResolver.pickedAudio(uri: Uri): PickedAudio {
	var name: String? = null
	var size: Long? = null
	withContext(Dispatchers.IO) {
		query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
			if (cursor.moveToFirst()) {
				name = cursor.getString(0)
				size = if (cursor.isNull(1)) null else cursor.getLong(1)
			}
		}
	}
	return PickedAudio(
		displayName = name ?: uri.lastPathSegment.orEmpty(),
		sizeBytes = size,
		read = { maxBytes ->
			withContext(Dispatchers.IO) {
				checkNotNull(openInputStream(uri)) { "Couldn't open the picked file" }.use { it.readAtMost(maxBytes) }
			}
		},
	)
}

/** Like readNBytes, which needs API 33: stops after [limit] bytes instead of reading the whole stream. */
private fun InputStream.readAtMost(limit: Int): ByteArray {
	val out = ByteArrayOutputStream()
	val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
	while (out.size() < limit) {
		val count = read(buffer, 0, minOf(buffer.size, limit - out.size()))
		if (count < 0) break
		out.write(buffer, 0, count)
	}
	return out.toByteArray()
}
