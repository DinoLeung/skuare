package xyz.d1n0.ui.screen.pairing

import androidx.compose.runtime.Composable
import com.juul.kable.Peripheral

@Composable
actual fun rememberWatchPicker(onPicked: (Peripheral) -> Unit): (() -> Unit)? = null
