package xyz.d1n0.phone

import dev.icerock.moko.permissions.Permission
import kotlinx.coroutines.CompletableDeferred
import kotlin.time.Duration

/**
 * [LocationProvider] that returns [result], after [answer] completes if it is set, so a test can
 * hold the read open.
 */
internal class FakeLocationProvider(
	var result: LocationResult = LocationResult.Fix(GeoPoint(22.2793, 114.16281, accuracyMeters = 12.0)),
	override val isSupported: Boolean = true,
) : LocationProvider {
	override val requiredPermissions = emptyList<Permission>()

	var answer: CompletableDeferred<Unit>? = null
	var requestCount = 0
		private set

	override fun isPermissionGranted() = result != LocationResult.PermissionDenied

	override suspend fun currentLocation(timeout: Duration, maxAge: Duration): LocationResult {
		requestCount++
		answer?.await()
		return result
	}
}
