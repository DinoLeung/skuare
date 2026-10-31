package xyz.d1n0.ui.component

import androidx.compose.runtime.Composable

/**
 * The installed app's version name and build number, e.g. "0.0.3 (4)".
 */
@Composable
expect fun appVersion(): String
