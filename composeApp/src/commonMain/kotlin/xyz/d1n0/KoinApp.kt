package xyz.d1n0

import co.touchlab.kermit.Logger
import co.touchlab.kermit.koin.KermitKoinLogger
import dev.icerock.moko.permissions.PermissionsController
import org.koin.core.KoinApplication
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import xyz.d1n0.model.Watch
import xyz.d1n0.screen.scan.ScanScreenViewModel
import xyz.d1n0.screen.watch.WatchScreenViewModel

fun KoinApp(permissionsController: PermissionsController): KoinApplication.() -> Unit =
{
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
	}