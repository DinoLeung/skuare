package xyz.d1n0.ui.component

import androidx.compose.runtime.Composable
import xyz.d1n0.phone.PickedAudio

/**
 * Lets the user pick an audio file with the system's file picker, for the find phone ringtone.
 * The file isn't read until [PickedAudio.read].
 *
 * @param onPicked Called as soon as the user picks a file, not when they cancel. Its argument looks
 *        up the file's name and size, which may take a while and throws if it fails, so the caller
 *        runs it in turn with whatever else changes the sound, e.g. Reset.
 * @return A function that opens the picker, or null where the platform has none yet.
 */
@Composable
expect fun rememberAudioFilePicker(onPicked: (load: suspend () -> PickedAudio) -> Unit): (() -> Unit)?
