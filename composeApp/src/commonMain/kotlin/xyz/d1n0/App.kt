package xyz.d1n0

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.koin.compose.KoinApplicationPreview
import xyz.d1n0.ui.navigation.RootNavGraph
import xyz.d1n0.ui.theme.SkuareTheme

/**
 * Root of the UI, Koin must be started with [initKoin] before this is shown.
 */
@Composable
fun App() {
	SkuareTheme {
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
