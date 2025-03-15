package xyz.d1n0

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.KoinApplication
import org.koin.compose.KoinContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import xyz.d1n0.viewModel.ScanScreenViewModel
import xyz.d1n0.viewModel.WatchScreenViewModel

@Composable
@Preview
fun App() {
	MaterialTheme {
		KoinApplication(application = {
			modules(module {
				single<Repo> { RepoImpl() }

				viewModelOf(::ScanScreenViewModel)
				viewModelOf(::WatchScreenViewModel)
			})
		}) {
			val navController: NavHostController = rememberNavController()
			NavGraph(navController)
		}
	}
}
