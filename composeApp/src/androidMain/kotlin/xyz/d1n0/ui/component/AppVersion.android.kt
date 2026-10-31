package xyz.d1n0.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun appVersion(): String {
	val context = LocalContext.current
	return remember(context) {
		val info = context.packageManager.getPackageInfo(context.packageName, 0)
		"${info.versionName} (${info.longVersionCode})"
	}
}
