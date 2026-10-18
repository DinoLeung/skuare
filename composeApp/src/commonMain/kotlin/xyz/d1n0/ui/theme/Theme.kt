package xyz.d1n0.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * Colors from the wallpaper (Material You), or null if the platform has none and should use the
 * static schemes.
 */
@Composable
expect fun dynamicColorScheme(darkTheme: Boolean): ColorScheme?

/**
 * Follows the system light/dark theme, preferring dynamic colors over the static #607D8B schemes.
 */
@Composable
fun SkuareTheme(
	darkTheme: Boolean = isSystemInDarkTheme(),
	dynamicColor: Boolean = true,
	content: @Composable () -> Unit,
) {
	val colorScheme = (if (dynamicColor) dynamicColorScheme(darkTheme) else null)
		?: if (darkTheme) SkuareDarkColorScheme else SkuareLightColorScheme

	MaterialTheme(colorScheme = colorScheme, content = content)
}
