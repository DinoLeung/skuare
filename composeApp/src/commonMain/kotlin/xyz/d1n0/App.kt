package xyz.d1n0

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.koin.compose.KoinApplicationPreview
import xyz.d1n0.ui.navigation.RootNavGraph

/**
 * Root of the UI, Koin must be started with [initKoin] before this is shown.
 */
@Composable
fun App() {
	MaterialTheme {
		RootNavGraph()
	}
}

@Preview
@Composable
fun AppPreview() {
	KoinApplicationPreview(application = { modules(coreModule, uiModule) }) {
		App()
	}
}
