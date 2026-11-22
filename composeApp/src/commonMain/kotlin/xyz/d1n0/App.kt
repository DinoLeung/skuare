package xyz.d1n0

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.tooling.preview.Preview
import org.koin.compose.KoinApplicationPreview
import org.koin.compose.koinInject
import org.koin.dsl.module
import xyz.d1n0.phone.CustomSoundStore
import xyz.d1n0.phone.PhoneRinger
import xyz.d1n0.phone.UnsupportedCustomSoundStore
import xyz.d1n0.phone.UnsupportedPhoneRinger
import xyz.d1n0.ui.component.FindPhoneRingingDialog
import xyz.d1n0.ui.navigation.RootNavGraph
import xyz.d1n0.ui.text.IsoLocaleFormatter
import xyz.d1n0.ui.text.LocalLocaleFormatter
import xyz.d1n0.ui.text.LocaleFormatter
import xyz.d1n0.ui.theme.SkuareTheme

/**
 * Root of the UI, Koin must be started with [initKoin] before this is shown.
 */
@Composable
fun App() {
	CompositionLocalProvider(LocalLocaleFormatter provides koinInject<LocaleFormatter>()) {
		SkuareTheme {
			RootNavGraph()
			// over whatever screen is open, see PhoneFinder
			FindPhoneRingingDialog()
		}
	}
}

@Preview
@Composable
fun AppPreview() {
	// the platform module isn't loaded in previews
	val previewModule = module {
		single<LocaleFormatter> { IsoLocaleFormatter }
		single<PhoneRinger> { UnsupportedPhoneRinger() }
		single<CustomSoundStore> { UnsupportedCustomSoundStore() }
	}
	KoinApplicationPreview(application = { modules(coreModule, uiModule, previewModule) }) {
		App()
	}
}
