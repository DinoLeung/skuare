package xyz.d1n0.ui.component

import androidx.compose.runtime.Composable
import xyz.d1n0.phone.PickedAudio

/**
 * Lets the user pick an audio file with the system's file picker, for the find phone ringtone.
 * The file isn't read until [PickedAudio.read].
 *
 * @param onPicked Called with the file the user picked, not when they cancel.
 * @return A function that opens the picker, or null where the platform has none yet.
 */
@Composable
expect fun rememberAudioFilePicker(onPicked: (PickedAudio) -> Unit): (() -> Unit)?
