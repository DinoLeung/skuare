package xyz.d1n0.phone

import dev.icerock.moko.permissions.Permission
import kotlinx.coroutines.CompletableDeferred
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration

/**
 * [LocationProvider] whose fresh fix returns [result], after [answer] completes if it is set, so a
 * test can hold the read open, and whose last known location is [lastKnown].
 */
internal class FakeLocationProvider(
	var result: LocationResult = LocationResult.Fix(GeoPoint(22.2793, 114.16281, accuracyMeters = 12.0)),
	override val isSupported: Boolean = true,
	var lastKnown: GeoPoint? = null,
) : LocationProvider {
	override val requiredPermissions = emptyList<Permission>()

	var answer: CompletableDeferred<Unit>? = null
	var requestCount = 0
		private set

	/** Whether the caller gave up on the fresh fix. */
	var cancelled = false
		private set

	override fun isPermissionGranted() = result != LocationResult.PermissionDenied

	override suspend fun currentLocation(): LocationResult {
		requestCount++
		try {
			answer?.await()
		} catch (e: CancellationException) {
			cancelled = true
			throw e
		}
		return result
	}

	override suspend fun lastKnownLocation(maxAge: Duration): GeoPoint? = lastKnown
}
