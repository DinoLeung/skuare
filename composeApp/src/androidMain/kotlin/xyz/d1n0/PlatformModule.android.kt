package xyz.d1n0

import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import xyz.d1n0.data.database.databaseBuilder
import xyz.d1n0.sync.CompanionWatchPresenceMonitor
import xyz.d1n0.sync.WatchPresenceMonitor

actual val platformModule: Module = module {
	single { databaseBuilder(androidContext()) }
	single<WatchPresenceMonitor> { CompanionWatchPresenceMonitor(androidContext()) }
}
