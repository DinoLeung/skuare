package xyz.d1n0

import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import xyz.d1n0.data.database.databaseBuilder
import xyz.d1n0.phone.AndroidCustomSoundStore
import xyz.d1n0.phone.AndroidLocationProvider
import xyz.d1n0.phone.AndroidPhoneRinger
import xyz.d1n0.phone.CustomSoundStore
import xyz.d1n0.phone.GeoUriMapLinks
import xyz.d1n0.phone.LocationProvider
import xyz.d1n0.phone.MapLinkBuilder
import xyz.d1n0.phone.PhoneFinder
import xyz.d1n0.phone.PhoneRinger
import xyz.d1n0.sync.CompanionWatchPresenceMonitor
import xyz.d1n0.sync.AndroidWatchConnectionNotifier
import xyz.d1n0.sync.WatchConnectionNotifier
import xyz.d1n0.sync.WatchPresenceMonitor
import xyz.d1n0.ui.text.AndroidLocaleFormatter
import xyz.d1n0.ui.text.LocaleFormatter

actual val platformModule: Module = module {
	single { databaseBuilder(androidContext()) }
	single<WatchPresenceMonitor> { CompanionWatchPresenceMonitor(androidContext()) }
	single<WatchConnectionNotifier> {
		// looked up when asked, PhoneFinder needs the PhoneRinger below
		AndroidWatchConnectionNotifier(androidContext(), isRinging = { get<PhoneFinder>().ringing.value })
	}
	single<LocaleFormatter> { AndroidLocaleFormatter(androidContext()) }
	single<LocationProvider> { AndroidLocationProvider(androidContext()) }
	single<PhoneRinger> { AndroidPhoneRinger(androidContext()) }
	single<CustomSoundStore> { AndroidCustomSoundStore(androidContext()) }
	single<MapLinkBuilder> { GeoUriMapLinks }
}
