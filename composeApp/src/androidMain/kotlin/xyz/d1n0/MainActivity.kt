package xyz.d1n0

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		enableEdgeToEdge()
		super.onCreate(savedInstanceState)

		setContent {
			// The activity handles uiMode changes itself rather than being recreated, so the system
			// bar icons follow the theme from here instead of once in onCreate
			val darkTheme = isSystemInDarkTheme()
			DisposableEffect(darkTheme) {
				enableEdgeToEdge(
					statusBarStyle = SystemBarStyle.auto(
						lightScrim = Color.TRANSPARENT,
						darkScrim = Color.TRANSPARENT,
					) { darkTheme },
					navigationBarStyle = SystemBarStyle.auto(
						lightScrim = Color.argb(0xe6, 0xFF, 0xFF, 0xFF),
						darkScrim = Color.argb(0x80, 0x1b, 0x1b, 0x1b),
					) { darkTheme },
				)
				onDispose {}
			}

			App()
		}
	}
}

@Preview
@Composable
fun AppAndroidPreview() {
	AppPreview()
}
