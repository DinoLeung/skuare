package xyz.d1n0

import org.koin.core.module.Module
import org.koin.dsl.module
import xyz.d1n0.data.database.databaseBuilder
import xyz.d1n0.sync.UnsupportedWatchPresenceMonitor
import xyz.d1n0.sync.WatchConnectionNotifier
import xyz.d1n0.sync.WatchPresenceMonitor

actual val platformModule: Module = module {
	single { databaseBuilder() }
	single<WatchPresenceMonitor> { UnsupportedWatchPresenceMonitor() }
	single<WatchConnectionNotifier> { WatchConnectionNotifier.None }
}
