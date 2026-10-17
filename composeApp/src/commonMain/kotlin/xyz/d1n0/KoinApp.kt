package xyz.d1n0

import androidx.room.RoomDatabase
import co.touchlab.kermit.Logger
import co.touchlab.kermit.koin.KermitKoinLogger
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import org.koin.mp.KoinPlatform
import xyz.d1n0.data.database.SkuareDatabase
import xyz.d1n0.data.database.buildSkuareDatabase
import xyz.d1n0.data.repository.InstallationRepository
import xyz.d1n0.data.repository.PairedWatchRepository
import xyz.d1n0.lib.connection.WatchConnectionManager
import xyz.d1n0.sync.WatchSessionHandler
import xyz.d1n0.ui.navigation.NavBarViewModel
import xyz.d1n0.ui.screen.alarms.AlarmsScreenViewModel
import xyz.d1n0.ui.screen.clocks.ClocksScreenViewModel
import xyz.d1n0.ui.screen.reminders.RemindersScreenViewModel
import xyz.d1n0.ui.screen.scan.ScanScreenViewModel
import xyz.d1n0.ui.screen.settings.SettingsScreenViewModel
import xyz.d1n0.ui.screen.timer.TimerScreenViewModel

/**
 * Dependencies that work without UI, so background sync can use them too.
 * Needs [platformModule] for the database builder and presence monitor.
 */
val coreModule = module {
	single { Log }

	single { get<RoomDatabase.Builder<SkuareDatabase>>().buildSkuareDatabase() }
	single { get<SkuareDatabase>().pairedWatchDao() }
	single { get<SkuareDatabase>().appConfigDao() }
	singleOf(::PairedWatchRepository)
	singleOf(::InstallationRepository)

	singleOf(::WatchConnectionManager)
	singleOf(::WatchSessionHandler)
	// Resolved on every injection, so screens get the currently connected watch
	factory { get<WatchConnectionManager>().requireWatch() }
}

val uiModule = module {
	viewModel { params -> ScanScreenViewModel(onConnect = params[0], permissionsController = params[1]) }

	viewModelOf(::NavBarViewModel)
	viewModelOf(::ClocksScreenViewModel)
	viewModelOf(::AlarmsScreenViewModel)
	viewModelOf(::TimerScreenViewModel)
	viewModelOf(::RemindersScreenViewModel)
	viewModelOf(::SettingsScreenViewModel)
}

/**
 * Starts Koin once per process, does nothing if it is already started.
 *
 * @param appDeclaration Platform specific setup, e.g. the Android context.
 */
fun initKoin(appDeclaration: KoinAppDeclaration = {}) {
	if (KoinPlatform.getKoinOrNull() != null) return
	startKoin {
		logger(KermitKoinLogger(Logger.withTag("koin")))
		appDeclaration()
		modules(platformModule, coreModule, uiModule)
	}
}
