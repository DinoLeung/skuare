package xyz.d1n0

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.write
import kotlinx.serialization.json.Json
import org.jetbrains.compose.ui.tooling.preview.Preview

import xyz.d1n0.model.Watch
import xyz.d1n0.view.ScanRoute
import xyz.d1n0.view.ScanScreen
import xyz.d1n0.view.WatchRoute
import xyz.d1n0.view.WatchScreen
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.reflect.typeOf

@Composable
@Preview
fun App() {

	val navController: NavHostController = rememberNavController()

	MaterialTheme {
		NavHost(navController, startDestination = ScanRoute) {
			composable<ScanRoute> {
				ScanScreen(navController)
			}
			composable<WatchRoute>(
				typeMap = mapOf(
					typeOf<Watch>() to CustomNavType.WatchNavType
				)
			) {
				val args = it.toRoute<WatchRoute>()
				WatchScreen(navController, args.watch)
			}
		}
	}
}

@OptIn(ExperimentalEncodingApi::class)
object CustomNavType {
	val WatchNavType = object: NavType<Watch>(isNullableAllowed = false) {
		override fun get(bundle: SavedState, key: String): Watch? {
			val json = bundle.read { if (!contains(key) || isNull(key)) null else getString(key) }
			return json?.let { Json.decodeFromString<Watch>(it) }
		}

		override fun put(bundle: SavedState, key: String, value: Watch) {
			bundle.write { putString(key, Json.encodeToString(value)) }
		}

		// TODO: the string must be URI safe
		//  although NavUriUtils.encode and NavUriUtils.decode are internal
		//  https://youtrack.jetbrains.com/issue/CMP-7677/NavUriUtils-in-new-navigation-versions-is-internal-and-breaks-custom-NavTypes
		override fun serializeAsValue(value: Watch): String {
			val json = Json.encodeToString(value)
			return Base64.UrlSafe.encode(json.encodeToByteArray())
		}

		override fun parseValue(value: String): Watch {
			val json = Base64.UrlSafe.decode(value)
			return Json.decodeFromString<Watch>(json.decodeToString())
		}
	}
}
