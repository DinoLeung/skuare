package xyz.d1n0.phone

import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.location.BACKGROUND_LOCATION
import dev.icerock.moko.permissions.location.LOCATION
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.Foundation.NSError
import platform.Foundation.timeIntervalSinceNow
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Reads the location with CoreLocation. A background session needs "Always" authorization,
 * "While Using" only works with the app open.
 *
 * Only type-checked so far (no Mac). It only runs once iOS has background sync, see
 * research/ios-background-sync.md, the App settings switch stays hidden until then.
 */
@OptIn(ExperimentalForeignApi::class)
class IosLocationProvider : LocationProvider {
	override val isSupported = true

	// moko asks for "While Using" first and "Always" after it, as iOS expects
	override val requiredPermissions: List<Permission> = listOf(Permission.LOCATION, Permission.BACKGROUND_LOCATION)

	override fun isPermissionGranted(): Boolean =
		CLLocationManager().authorizationStatus == kCLAuthorizationStatusAuthorizedAlways

	// CLLocationManager calls its delegate on the thread it was created on, which needs a run loop
	override suspend fun currentLocation(timeout: Duration, maxAge: Duration): LocationResult =
		withContext(Dispatchers.Main) {
			if (!isPermissionGranted()) return@withContext LocationResult.PermissionDenied
			if (!CLLocationManager.locationServicesEnabled()) return@withContext LocationResult.Disabled

			val manager = CLLocationManager().apply { desiredAccuracy = kCLLocationAccuracyBest }
			val fix = withTimeoutOrNull(timeout) { requestFix(manager) }
			val location = fix ?: manager.location?.takeIf { it.age <= maxAge }
				?: return@withContext LocationResult.TimedOut
			LocationResult.Fix(location.toGeoPoint())
		}

	/** A fresh fix, null if CoreLocation failed to get one. */
	private suspend fun requestFix(manager: CLLocationManager): CLLocation? =
		suspendCancellableCoroutine { continuation ->
			val delegate = FixDelegate { location ->
				manager.delegate = null
				if (continuation.isActive) continuation.resume(location)
			}
			// the manager holds its delegate weakly, the continuation keeps it alive until done
			manager.delegate = delegate
			continuation.invokeOnCancellation {
				manager.stopUpdatingLocation()
				delegate.onResult = {}
			}
			manager.requestLocation()
		}

	private class FixDelegate(var onResult: (CLLocation?) -> Unit) : NSObject(), CLLocationManagerDelegateProtocol {
		override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
			onResult(didUpdateLocations.lastOrNull() as? CLLocation)
		}

		override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
			onResult(null)
		}
	}
}

/** How long ago the location was taken. */
private val CLLocation.age: Duration
	get() = (-timestamp.timeIntervalSinceNow).seconds

@OptIn(ExperimentalForeignApi::class)
private fun CLLocation.toGeoPoint(): GeoPoint = coordinate.useContents {
	GeoPoint(
		latitude = latitude,
		longitude = longitude,
		// negative means the position is invalid, CoreLocation doesn't report those as a fix
		accuracyMeters = horizontalAccuracy.takeIf { it >= 0 },
	)
}
