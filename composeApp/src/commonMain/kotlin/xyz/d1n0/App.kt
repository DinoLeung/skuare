package xyz.d1n0

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.compose.rememberNavController
import dev.icerock.moko.permissions.compose.rememberPermissionsControllerFactory
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.KoinApplication
import xyz.d1n0.ui.navigation.RootNavGraph

@Composable
@Preview
fun App() {
	MaterialTheme {

		val permissionsControllerFactory = rememberPermissionsControllerFactory()
		val permissionsController =
			remember { permissionsControllerFactory.createPermissionsController() }

		KoinApplication(
			application = KoinApp(
				rootNavHostController = rememberNavController(),
				permissionsController = permissionsController,
			)
		) {
			RootNavGraph()
		}
	}
}

