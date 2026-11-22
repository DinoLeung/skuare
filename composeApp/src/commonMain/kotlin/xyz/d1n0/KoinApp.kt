package xyz.d1n0

import androidx.room.RoomDatabase
import co.touchlab.kermit.Logger
import co.touchlab.kermit.koin.KermitKoinLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import org.koin.mp.KoinPlatform
import xyz.d1n0.data.database.SkuareDatabase
import xyz.d1n0.data.database.buildSkuareDatabase
import xyz.d1n0.data.repository.AppSettingsRepository
import xyz.d1n0.data.repository.InstallationRepository
import xyz.d1n0.data.repository.PairedWatchRepository
import xyz.d1n0.data.repository.PressedLocationRepository
import xyz.d1n0.lib.connection.WatchConnectionManager
import xyz.d1n0.phone.PhoneFinder
import xyz.d1n0.phone.builtInFindPhoneSound
import xyz.d1n0.sync.WatchSessionHandler
import xyz.d1n0.ui.navigation.NavBarViewModel
import xyz.d1n0.ui.screen.alarms.AlarmsScreenViewModel
import xyz.d1n0.ui.screen.appsettings.AppSettingsScreenViewModel
import xyz.d1n0.ui.screen.locations.LocationsScreenViewModel
import xyz.d1n0.ui.screen.timezones.TimezonesScreenViewModel
import xyz.d1n0.ui.screen.pairing.PairingScreenViewModel
import xyz.d1n0.ui.screen.reminders.RemindersScreenViewModel
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
	single { get<SkuareDatabase>().appSettingsDao() }
	single { get<SkuareDatabase>().pressedLocationDao() }
	singleOf(::PairedWatchRepository)
	singleOf(::InstallationRepository)
	singleOf(::AppSettingsRepository)
	singleOf(::PressedLocationRepository)

	// not singleOf, it would ask Koin for the scope too
	single { WatchConnectionManager(connectionNotifier = get(), pairedWatchRepository = get()) }
	singleOf(::WatchSessionHandler)
	// rings outside any screen or session, for as long as the process lives
	single {
		PhoneFinder(
			ringer = get(),
			loadSound = ::builtInFindPhoneSound,
			scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
		)
	}
	// Resolved on every injection, so screens get the currently connected watch
	factory { get<WatchConnectionManager>().requireWatch() }
}

val uiModule = module {
	viewModel { params -> PairingScreenViewModel(onConnect = params[0], permissionsController = params[1]) }
	viewModel { params ->
		AppSettingsScreenViewModel(
			permissions = params[0],
			appSettingsRepository = get(),
			locationProvider = get(),
			presenceMonitor = get(),
		)
	}
	// not viewModelOf, it would ask Koin for the clock and time zone too
	viewModel { LocationsScreenViewModel(pressedLocationRepository = get(), appSettingsRepository = get()) }

	viewModelOf(::NavBarViewModel)
	viewModelOf(::TimezonesScreenViewModel)
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
