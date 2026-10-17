package xyz.d1n0.ui.screen.pairing

import androidx.compose.runtime.Composable
import com.juul.kable.Peripheral

/**
 * Lets the user pick a watch from a system dialog. Android uses the Companion Device Manager, so
 * the picked watch can wake the app later for background sync.
 *
 * @param onPicked Called with the watch the user picked, not connected yet.
 * @return A function that opens the dialog, or null if the platform has none and should search
 *         instead.
 */
@Composable
expect fun rememberWatchPicker(onPicked: (Peripheral) -> Unit): (() -> Unit)?
