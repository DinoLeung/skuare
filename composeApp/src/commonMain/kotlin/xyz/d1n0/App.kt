package xyz.d1n0

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import co.touchlab.kermit.*
import co.touchlab.kermit.koin.KermitKoinLogger
import dev.icerock.moko.permissions.PermissionsController
import dev.icerock.moko.permissions.compose.rememberPermissionsControllerFactory
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.KoinApplication
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import xyz.d1n0.model.Watch
import xyz.d1n0.viewModel.ScanScreenViewModel
import xyz.d1n0.viewModel.WatchScreenViewModel

@Composable
@Preview
fun App() {
	MaterialTheme {

		val permissionsControllerFactory = rememberPermissionsControllerFactory()
		val permissionsController = remember { permissionsControllerFactory.createPermissionsController() }

		KoinApplication(application = {
			logger(
				KermitKoinLogger(Logger.withTag("koin"))
			)
			modules(module {
				single { Log }
				single { permissionsController }

				singleOf(::Watch)
				viewModelOf(::ScanScreenViewModel)
				viewModelOf(::WatchScreenViewModel)
			})
		}) {
			val navController: NavHostController = rememberNavController()
			NavGraph(navController)
		}
	}
}
