package xyz.d1n0

import org.koin.core.module.Module
import org.koin.dsl.module
import xyz.d1n0.data.database.databaseBuilder
import xyz.d1n0.phone.IosLocationProvider
import xyz.d1n0.phone.AppleMapsLinks
import xyz.d1n0.phone.LocationProvider
import xyz.d1n0.phone.MapLinkBuilder
import xyz.d1n0.phone.PhoneRinger
import xyz.d1n0.phone.UnsupportedPhoneRinger
import xyz.d1n0.sync.UnsupportedWatchPresenceMonitor
import xyz.d1n0.sync.WatchConnectionNotifier
import xyz.d1n0.sync.WatchPresenceMonitor
import xyz.d1n0.ui.text.IosLocaleFormatter
import xyz.d1n0.ui.text.LocaleFormatter

actual val platformModule: Module = module {
	single { databaseBuilder() }
	single<WatchPresenceMonitor> { UnsupportedWatchPresenceMonitor() }
	single<WatchConnectionNotifier> { WatchConnectionNotifier.None }
	single<LocaleFormatter> { IosLocaleFormatter() }
	single<LocationProvider> { IosLocationProvider() }
	// find phone, step 4 (Android) and step 7 (iOS) of research/button-actions-plan.md
	single<PhoneRinger> { UnsupportedPhoneRinger() }
	single<MapLinkBuilder> { AppleMapsLinks }
}
