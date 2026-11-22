package xyz.d1n0.ui.component

import androidx.compose.runtime.Composable
import xyz.d1n0.phone.PickedAudio

/**
 * None on iOS yet, so the Sound row of the Find phone card stays hidden. See step 7 of
 * research/button-actions-plan.md, to do once there's a Mac and an iOS device to try it on.
 */
@Composable
actual fun rememberAudioFilePicker(onPicked: (PickedAudio) -> Unit): (() -> Unit)? {
	// TODO: present UIDocumentPickerViewController(forOpeningContentTypes = listOf(UTTypeAudio),
	//  asCopy = true) from the Compose UIViewController, with a delegate calling onPicked.
	// TODO: asCopy hands over a temporary copy, no security-scoped access needed. PickedAudio from
	//  its URL: displayName = lastPathComponent, sizeBytes from NSURLFileSizeKey, read = at most
	//  maxBytes via NSFileHandle.readDataUpToLength on Dispatchers.IO, not NSData(contentsOfURL),
	//  which reads the whole file.
	// TODO: not MPMediaPickerController, songs protected or only in iCloud have no readable file.
	return null
}
