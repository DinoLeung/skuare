package xyz.d1n0.ui.component

import androidx.compose.runtime.Composable
import xyz.d1n0.phone.PickedAudio

// step 7 of research/button-actions-plan.md, UIDocumentPickerViewController, the Sound row hides till then
@Composable
actual fun rememberAudioFilePicker(onPicked: (PickedAudio) -> Unit): (() -> Unit)? = null
