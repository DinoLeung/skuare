package xyz.d1n0

import androidx.navigation.NavHostController
import co.touchlab.kermit.Logger
import co.touchlab.kermit.koin.KermitKoinLogger
import dev.icerock.moko.permissions.PermissionsController
import org.koin.core.KoinApplication
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import xyz.d1n0.lib.model.Watch
import xyz.d1n0.ui.screen.scan.ScanScreenViewModel
import xyz.d1n0.ui.screen.watch.WatchScreenViewModel

fun KoinApp(
    rootNavHostController: NavHostController,
    permissionsController: PermissionsController,
): KoinApplication.() -> Unit =
{
	logger(
        KermitKoinLogger(Logger.withTag("koin"))
	)
	modules(module {
        single { Log }
        single<NavHostController>(named("rootNavHostController")) { rootNavHostController }
        single<PermissionsController> { permissionsController }

        singleOf(::Watch)

        viewModelOf(::ScanScreenViewModel)
        viewModelOf(::WatchScreenViewModel)
    })
}