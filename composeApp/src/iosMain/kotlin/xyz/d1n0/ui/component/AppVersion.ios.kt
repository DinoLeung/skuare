package xyz.d1n0.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSBundle

@Composable
actual fun appVersion(): String = remember {
	val bundle = NSBundle.mainBundle
	val versionName = bundle.objectForInfoDictionaryKey("CFBundleShortVersionString")
	val buildNumber = bundle.objectForInfoDictionaryKey("CFBundleVersion")
	"$versionName ($buildNumber)"
}
