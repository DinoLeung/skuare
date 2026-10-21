package xyz.d1n0.sync

import android.companion.AssociationInfo
import android.companion.CompanionDeviceManager
import android.companion.CompanionDeviceService
import android.companion.DevicePresenceEvent
import android.os.Build
import androidx.annotation.RequiresApi
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import xyz.d1n0.Log

/**
 * Bound by the system when a watch registered with [CompanionWatchPresenceMonitor] starts
 * advertising, i.e. it wants to connect. Hands the watch over to [WatchSyncService].
 */
class WatchCompanionService : CompanionDeviceService(), KoinComponent {
	private val log: Log by inject()

	// API 33-35
	@Deprecated("Replaced by onDevicePresenceEvent on API 36")
	override fun onDeviceAppeared(associationInfo: AssociationInfo) {
		// API 36+ calls this too, next to onDevicePresenceEvent, also when the app itself connects
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) return
		associationInfo.deviceMacAddress?.let {
			startSync(it.toString(), associationInfo.displayName?.toString())
		}
	}

	@RequiresApi(Build.VERSION_CODES.BAKLAVA)
	override fun onDevicePresenceEvent(event: DevicePresenceEvent) {
		if (event.event != DevicePresenceEvent.EVENT_BLE_APPEARED) return
		getSystemService(CompanionDeviceManager::class.java).myAssociations
			.firstOrNull { it.id == event.associationId }
			?.let { association ->
				association.deviceMacAddress?.let {
					startSync(it.toString(), association.displayName?.toString())
				}
			}
	}

	private fun startSync(address: String, advertisedName: String?) {
		log.d { "Watch appeared: $address" }
		runCatching { WatchSyncService.start(this, address.uppercase(), advertisedName) }
			.onFailure { log.e { "Failed to start background sync for $address: ${it.message}" } }
	}
}
