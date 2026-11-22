package xyz.d1n0

import org.koin.core.module.Module
import org.koin.dsl.module
import xyz.d1n0.data.database.databaseBuilder
import xyz.d1n0.phone.IosLocationProvider
import xyz.d1n0.phone.AppleMapsLinks
import xyz.d1n0.phone.CustomSoundStore
import xyz.d1n0.phone.LocationProvider
import xyz.d1n0.phone.MapLinkBuilder
import xyz.d1n0.phone.PhoneRinger
import xyz.d1n0.phone.UnsupportedCustomSoundStore
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
	// Find phone, step 7 of research/button-actions-plan.md, to do with a Mac and an iOS device.
	// Unsupported until then, the Find phone card says it isn't available yet. It only becomes
	// useful with iOS background sync (research/ios-background-sync.md), a long press needs it.
	// TODO: IosPhoneRinger. AVAudioSession category .playback (plays in silent mode),
	//  AVAudioPlayer(data:) with numberOfLoops = -1 and volume = 1, until cancelled.
	//  canForceMaxVolume = false, iOS has no public API to raise the system volume. forceMaxVolume
	//  = false (Play sound) plays the same way. canPlay = whether AVAudioPlayer(data:) initialises.
	// TODO: spike: can a BLE-woken app start playback in the background? Needs `audio` in
	//  UIBackgroundModes in Info.plist. If not, fall back to a time-sensitive local notification
	//  with the sound (at most 30 s, follows the ringer volume and the silent switch).
	// TODO: the notification with Stop. A time-sensitive UNNotificationRequest while
	//  PhoneFinder.ringing, a UNNotificationCategory with a Stop action and customDismissAction, both
	//  calling PhoneFinder.stop(). The equivalent of Android's WatchSyncService notification.
	single<PhoneRinger> { UnsupportedPhoneRinger() }
	// TODO: IosCustomSoundStore. The copy in Application Support/find_phone/custom.<ext>, written to
	//  a temporary file then moved over the old one, marked NSURLIsExcludedFromBackupKey, like
	//  AndroidCustomSoundStore. Pass the extension as AVAudioPlayer's fileTypeHint when ringing.
	single<CustomSoundStore> { UnsupportedCustomSoundStore() }
	single<MapLinkBuilder> { AppleMapsLinks }
}
