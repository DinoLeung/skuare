package xyz.d1n0

import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import xyz.d1n0.data.database.databaseBuilder
import xyz.d1n0.sync.CompanionWatchPresenceMonitor
import xyz.d1n0.sync.AndroidWatchConnectionNotifier
import xyz.d1n0.sync.WatchConnectionNotifier
import xyz.d1n0.sync.WatchPresenceMonitor
import xyz.d1n0.ui.text.AndroidLocaleFormatter
import xyz.d1n0.ui.text.LocaleFormatter

actual val platformModule: Module = module {
	single { databaseBuilder(androidContext()) }
	single<WatchPresenceMonitor> { CompanionWatchPresenceMonitor(androidContext()) }
	single<WatchConnectionNotifier> { AndroidWatchConnectionNotifier(androidContext()) }
	single<LocaleFormatter> { AndroidLocaleFormatter(androidContext()) }
}
