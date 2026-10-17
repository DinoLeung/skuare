package xyz.d1n0.sync

import android.companion.AssociationInfo
import android.companion.CompanionDeviceManager
import android.companion.ObservingDevicePresenceRequest
import android.content.Context
import android.os.Build
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import xyz.d1n0.Log
import xyz.d1n0.data.database.PairedWatch

/**
 * Observes paired watches through the Companion Device Manager. A watch must have been picked in
 * the pairing dialog (see `rememberWatchPicker`) before it can be registered.
 */
class CompanionWatchPresenceMonitor(context: Context) : WatchPresenceMonitor, KoinComponent {
	private val log: Log by inject()

	private val companionDeviceManager = context.getSystemService(CompanionDeviceManager::class.java)

	override val isSupported = true

	/**
	 * Starts observing the watch's presence.
	 *
	 * Picking an already paired watch in the dialog again creates another association, so only
	 * the newest one is kept.
	 *
	 * @return [watch] with its association id, or unchanged if the watch is not associated, in
	 *         which case background sync is not available for it.
	 */
	override suspend fun register(watch: PairedWatch): PairedWatch =
		runCatching {
			val associations = associationsOf(watch)
			val association = associations.maxByOrNull { it.id }
				?: error("${watch.address} is not associated")
			associations.filter { it.id != association.id }
				.forEach { companionDeviceManager.disassociate(it.id) }
			startObserving(association)
			watch.copy(platformAssociationId = association.id)
		}.onSuccess {
			log.d { "Observing presence of ${watch.address}" }
		}.getOrElse { error ->
			log.e { "Background sync unavailable for ${watch.address}: ${error.message}" }
			watch
		}

	/**
	 * Removes the watch's associations, which also stops observing its presence.
	 */
	override suspend fun unregister(watch: PairedWatch) {
		runCatching {
			associationsOf(watch).forEach { companionDeviceManager.disassociate(it.id) }
		}.onFailure { error ->
			log.e { "Failed to unregister ${watch.address}: ${error.message}" }
		}
	}

	private fun associationsOf(watch: PairedWatch): List<AssociationInfo> =
		companionDeviceManager.myAssociations.filter {
			it.deviceMacAddress?.toString().equals(watch.address, ignoreCase = true)
		}

	private fun startObserving(association: AssociationInfo) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
			companionDeviceManager.startObservingDevicePresence(
				ObservingDevicePresenceRequest.Builder().setAssociationId(association.id).build()
			)
		} else {
			// deprecated on API 36, but the only option on API 33-35
			@Suppress("DEPRECATION")
			companionDeviceManager.startObservingDevicePresence(association.deviceMacAddress.toString())
		}
	}
}
