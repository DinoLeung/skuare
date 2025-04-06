package xyz.d1n0

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import dev.icerock.moko.permissions.compose.rememberPermissionsControllerFactory
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.KoinApplication

@Composable
@Preview
fun App() {
	MaterialTheme {

		val permissionsControllerFactory = rememberPermissionsControllerFactory()
		val permissionsController = remember { permissionsControllerFactory.createPermissionsController() }

		val rootNavController: NavHostController = rememberNavController()

		KoinApplication(application = KoinApp(permissionsController)) {
			NavGraph(rootNavController = rootNavController)
		}
	}
}

