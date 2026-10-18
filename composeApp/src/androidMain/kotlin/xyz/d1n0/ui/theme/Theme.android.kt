package xyz.d1n0.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// minSdk is above 31, so dynamic colors are always available
@Composable
actual fun dynamicColorScheme(darkTheme: Boolean): ColorScheme? {
	val context = LocalContext.current
	return if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
}
