package xyz.d1n0.phone

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import android.os.SystemClock
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.PermissionDelegate
import dev.icerock.moko.permissions.PermissionState
import dev.icerock.moko.permissions.location.LOCATION
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds

/**
 * Reads the location with the platform's [LocationManager], no Play Services, so the F-Droid build
 * works the same.
 *
 * The session runs in a foreground service started from the background by the Companion Device
 * Manager. Android only lets such a service read the location with background location ("Allow
 * all the time"), so that is required on top of precise location.
 */
class AndroidLocationProvider(context: Context) : LocationProvider {
	private val context = context.applicationContext
	private val manager = context.getSystemService(LocationManager::class.java)

	override val isSupported = true

	override val requiredPermissions: List<Permission> = listOf(Permission.LOCATION, BackgroundLocationOnly)

	override fun isPermissionGranted(): Boolean =
		isGranted(Manifest.permission.ACCESS_FINE_LOCATION) && isGranted(Manifest.permission.ACCESS_BACKGROUND_LOCATION)

	// the permissions are checked by isPermissionGranted first
	@SuppressLint("MissingPermission")
	override suspend fun currentLocation(timeout: Duration, maxAge: Duration): LocationResult {
		if (!isPermissionGranted()) return LocationResult.PermissionDenied
		if (!manager.isLocationEnabled) return LocationResult.Disabled
		val providers = PROVIDERS.filter { manager.hasProvider(it) && manager.isProviderEnabled(it) }
		val provider = providers.firstOrNull() ?: return LocationResult.Disabled

		val fix = withTimeoutOrNull(timeout) { requestFix(provider) }
		val location = fix ?: lastKnownLocation(providers, maxAge) ?: return LocationResult.TimedOut
		return LocationResult.Fix(
			GeoPoint(
				latitude = location.latitude,
				longitude = location.longitude,
				accuracyMeters = location.takeIf { it.hasAccuracy() }?.accuracy?.toDouble(),
			)
		)
	}

	/** A fresh fix, null if the provider gave up without one. */
	@SuppressLint("MissingPermission")
	private suspend fun requestFix(provider: String): Location? = suspendCancellableCoroutine { continuation ->
		val signal = CancellationSignal()
		continuation.invokeOnCancellation { signal.cancel() }
		manager.getCurrentLocation(provider, signal, context.mainExecutor) { location ->
			if (continuation.isActive) continuation.resume(location)
		}
	}

	/** The newest last known location of [providers], if it's younger than [maxAge]. */
	@SuppressLint("MissingPermission")
	private fun lastKnownLocation(providers: List<String>, maxAge: Duration): Location? =
		providers
			.mapNotNull { manager.getLastKnownLocation(it) }
			.filter { (SystemClock.elapsedRealtimeNanos() - it.elapsedRealtimeNanos).nanoseconds <= maxAge }
			.maxByOrNull { it.elapsedRealtimeNanos }

	private fun isGranted(permission: String) =
		context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

	/**
	 * Background location on its own. moko's BACKGROUND_LOCATION asks for it together with the
	 * foreground location, which Android 11+ ignores. Asked after [Permission.LOCATION] is granted,
	 * Android then opens the app's location settings to pick "Allow all the time".
	 */
	private object BackgroundLocationOnly : Permission {
		override val delegate = object : PermissionDelegate {
			override fun getPermissionStateOverride(applicationContext: Context): PermissionState? = null
			override fun getPlatformPermission() = listOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
		}
	}

	private companion object {
		/** In order of preference, the fused provider combines the others where the phone has it. */
		val PROVIDERS = listOf(
			LocationManager.FUSED_PROVIDER,
			LocationManager.GPS_PROVIDER,
			LocationManager.NETWORK_PROVIDER,
		)
	}
}
